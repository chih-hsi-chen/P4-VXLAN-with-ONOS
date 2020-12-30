/* -*- P4_16 -*- */
#include <core.p4>
#include <v1model.p4>
#define MAX_PORTS 255

/*************************************************************************
*********************** C O N S T S  ***********************************
*************************************************************************/

typedef bit<9>  egressSpec_t;
typedef bit<9>  port_t;
typedef bit<48> macAddr_t;
typedef bit<32> ip4Addr_t;
typedef bit<24> vni_t;
typedef bit<16> vxlan_type_t;

const bit<16> TYPE_IPV4 = 0x800;
const bit<16> TYPE_ARP  = 0x806;
const bit<8> PROTO_UDP = 0x11;
const bit<16> VXLAN_DST_PORT = 4789;
const port_t CPU_PORT = 255;
const port_t FLOOD_PORT = 500;

const bit<4> IP_VERSION_4 = 4;
const bit<4> IPV4_MIN_IHL = 5;

const bit<16> ETH_HDR_SIZE = 14;
const bit<16> IPV4_HDR_SIZE = 20;
const bit<16> ARP_HDR_SIZE = 28;
const bit<16> UDP_HDR_SIZE = 8;
const bit<16> VXLAN_HDR_SIZE = 8;

/*************************************************************************
*********************** H E A D E R S  ***********************************
*************************************************************************/

// Packet-in header. Prepended to packets sent to the controller and used to
// carry the original ingress port where the packet was received.
@controller_header("packet_in")
header packet_in_header_t {
    bit<9> ingress_port;
    bit<7> _padding;
}

// Packet-out header. Prepended to packets received by the controller and used
// to tell the switch on which port this packet should be forwarded.
@controller_header("packet_out")
header packet_out_header_t {
    bit<9> egress_port;
    bit<7> _padding;
}

header ethernet_t {
    macAddr_t dstAddr;
    macAddr_t srcAddr;
    bit<16>   etherType;
}

header ipv4_t {
    bit<4>    version;
    bit<4>    ihl;
    bit<8>    diffserv;
    bit<16>   totalLen;
    bit<16>   identification;
    bit<3>    flags;
    bit<13>   fragOffset;
    bit<8>    ttl;
    bit<8>    protocol;
    bit<16>   hdrChecksum;
    ip4Addr_t srcAddr;
    ip4Addr_t dstAddr;
}

header udp_t {
    bit<16> srcPort;
    bit<16> dstPort;
    bit<16> totalLen;
    bit<16> hdrChecksum;
}

header vxlan_t {
    bit<8> flag;
    bit<24> rsv_1;
    bit<24> vni;
    bit<8> rsv_2;
}

header arp_t {
    bit<16> hwType;
    bit<16> protoType;
    bit<8>  hwLen;
    bit<8>  protoLen;
    bit<16> op;
    bit<48> srcHwAddr;
    bit<32> srcProtoAddr;
    bit<48> dstHwAddr;
    bit<32> dstProtoAddr;
}

struct vxlan_info_t {
    vni_t      vni;
    ip4Addr_t  vtep_ip;
    ip4Addr_t  remote_ip;
}

struct metadata {
    bit<16> inner_packet_len;
}

struct headers {
    packet_in_header_t   packet_in;
    packet_out_header_t  packet_out;
    ethernet_t           ethernet;
    arp_t                arp;
    ipv4_t               ipv4;
    udp_t                udp;
    vxlan_t              vxlan;
    ethernet_t           inner_ethernet;
    arp_t                inner_arp;
    ipv4_t               inner_ipv4;
    udp_t                inner_udp;
}

counter(MAX_PORTS, CounterType.packets_and_bytes) tx_port_counter;
counter(MAX_PORTS, CounterType.packets_and_bytes) rx_port_counter;

/*************************************************************************
*********************** P A R S E R  ***********************************
*************************************************************************/

parser MyParser(packet_in packet,
                out headers hdr,
                inout metadata meta,
                inout standard_metadata_t standard_metadata) {

    state start {
        transition select(standard_metadata.ingress_port) {
            CPU_PORT: parse_packet_out;
            default: parse_ethernet;
        }
    }

    state parse_packet_out {
        packet.extract(hdr.packet_out);
        transition parse_ethernet;
    }

    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.etherType) {
            TYPE_IPV4: parse_ipv4;
            TYPE_ARP : parse_arp;
            default: accept;
        }
    }

    state parse_arp {
        packet.extract(hdr.arp);
        transition accept;
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition select(hdr.ipv4.protocol) {
            PROTO_UDP: parse_udp;
            default: accept;
        }
    }

    state parse_udp {
        packet.extract(hdr.udp);
        transition select(hdr.udp.dstPort) {
            VXLAN_DST_PORT: parse_vxlan;
            default: accept;
        }
    }

    state parse_vxlan {
        packet.extract(hdr.vxlan);
        transition parse_inner_ethernet;
    }

    state parse_inner_ethernet {
        packet.extract(hdr.inner_ethernet);
        transition select(hdr.inner_ethernet.etherType) {
            TYPE_IPV4: parse_inner_ipv4;
            TYPE_ARP : parse_inner_arp;
            default: accept;
        }
    }
    
    state parse_inner_ipv4 {
        packet.extract(hdr.inner_ipv4);
        transition accept;
    }

    state parse_inner_arp {
        packet.extract(hdr.inner_arp);
        transition accept;
    }
}

/*************************************************************************
************   C H E C K S U M    V E R I F I C A T I O N   *************
*************************************************************************/

control MyVerifyChecksum(inout headers hdr, inout metadata meta) {   
    apply {  }
}


/*************************************************************************
**************  I N G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyIngress(inout headers hdr,
                  inout metadata meta,
                  inout standard_metadata_t standard_metadata) {

    action drop() {
        mark_to_drop(standard_metadata);
    }

    action send_to_cpu() {
        standard_metadata.egress_spec = CPU_PORT;
        // Packets sent to the controller needs to be prepended with the
        // packet-in header. By setting it valid we make sure it will be
        // deparsed on the wire (see c_deparser).
        hdr.packet_in.setValid();
        hdr.packet_in.ingress_port = standard_metadata.ingress_port;
    }

    action set_out_port(egressSpec_t port) {
        // Specifies the output port for this packet by setting the
        // corresponding metadata.
        standard_metadata.egress_spec = port;
    }

    action vxlan_decap() {
        hdr.ethernet = hdr.inner_ethernet;
        // IPv4 and ARP headers are mutual exclusive
        // Only one of two headers is valid
        hdr.ipv4 = hdr.inner_ipv4;
        hdr.arp = hdr.inner_arp;

        hdr.udp.setInvalid();
        hdr.vxlan.setInvalid();
        hdr.inner_ethernet.setInvalid();
        // The same as outer ones
        hdr.inner_ipv4.setInvalid();
        hdr.inner_arp.setInvalid();
    }

    action l2_multicast(bit<16> grp) {
        standard_metadata.mcast_grp = grp;
    }

    action l3_forward(macAddr_t dmac, egressSpec_t port) {
        standard_metadata.egress_spec = port;
        hdr.ethernet.srcAddr = hdr.ethernet.dstAddr;
        hdr.ethernet.dstAddr = dmac;
        hdr.ipv4.ttl = hdr.ipv4.ttl - 1;
    }

    direct_counter(CounterType.packets_and_bytes) l2_fwd_counter;
    direct_counter(CounterType.packets_and_bytes) l3_fwd_counter;
    direct_counter(CounterType.packets_and_bytes) vxlan_decap_counter;
    
    table ipv4_lpm {
        key = {
            hdr.ipv4.dstAddr: ternary;
        }
        actions = {
            l3_forward;
            drop;
            NoAction;
        }
        size = 1024;
        default_action = NoAction();
        counters = l3_fwd_counter;
    }

    table decap_table {
        key = {
            hdr.ipv4.dstAddr: lpm;
        }
        actions = {
            vxlan_decap;
            drop;
            NoAction;
        }
        size = 1024;
        default_action = NoAction();
        counters = vxlan_decap_counter;
    }

    table l2_forward {
        key = {
            hdr.ethernet.dstAddr: ternary;
            hdr.ethernet.etherType: ternary;
        }
        actions = {
            l2_multicast;
            set_out_port;
            send_to_cpu;
            drop;
            NoAction;
        }
        size = 1024;
        default_action = NoAction();
        counters = l2_fwd_counter;
    }
    
    apply {
        if (standard_metadata.ingress_port == CPU_PORT) {
            // Packet received from CPU_PORT, this is a packet-out sent by the
            // controller. Skip table processing, set the egress port as
            // requested by the controller (packet_out header) and remove the
            // packet_out header.
            if(hdr.packet_out.egress_port == FLOOD_PORT)
                standard_metadata.mcast_grp = 1;
            else
                standard_metadata.egress_spec = hdr.packet_out.egress_port;
            hdr.packet_out.setInvalid();
        } else {
            if(hdr.vxlan.isValid()) {
                decap_table.apply();
            }
            if (hdr.ethernet.isValid()) {
                l2_forward.apply();
                ipv4_lpm.apply();
            }
        }

        // Update port counters at index = ingress or egress port.
        if (standard_metadata.egress_spec < MAX_PORTS) {
            tx_port_counter.count((bit<32>) standard_metadata.egress_spec);
        }
        if (standard_metadata.ingress_port < MAX_PORTS) {
            rx_port_counter.count((bit<32>) standard_metadata.ingress_port);
        }
    }
}

/*************************************************************************
****************  E G R E S S   P R O C E S S I N G   *******************
*************************************************************************/

control MyEgress(inout headers hdr,
                 inout metadata meta,
                 inout standard_metadata_t standard_metadata) {

    action drop() {
        mark_to_drop(standard_metadata);
    }

    action vxlan_encap(vni_t vni, macAddr_t smac, macAddr_t dmac, ip4Addr_t srcIP, ip4Addr_t dstIP) {
        // copy outer to inner
        hdr.inner_ethernet = hdr.ethernet;
        hdr.inner_ipv4 = hdr.ipv4;
        hdr.inner_arp = hdr.arp;
        hdr.inner_udp = hdr.udp;

        hdr.arp.setInvalid();

        hdr.ethernet.srcAddr = smac;
        hdr.ethernet.dstAddr = dmac;
        hdr.ethernet.etherType = TYPE_IPV4;

        hdr.ipv4.setValid();
        hdr.ipv4.version = IP_VERSION_4;
        hdr.ipv4.ihl = IPV4_MIN_IHL;
        hdr.ipv4.diffserv = 0;
        hdr.ipv4.totalLen = meta.inner_packet_len
                            + (ETH_HDR_SIZE + IPV4_HDR_SIZE + UDP_HDR_SIZE + VXLAN_HDR_SIZE);
        hdr.ipv4.identification = 0x1513; /* From NGIC */
        hdr.ipv4.flags = 0;
        hdr.ipv4.fragOffset = 0;
        hdr.ipv4.ttl = 64;
        hdr.ipv4.protocol = PROTO_UDP;
        hdr.ipv4.dstAddr = dstIP;
        hdr.ipv4.srcAddr = srcIP;
        hdr.ipv4.hdrChecksum = 0;

        hdr.udp.setValid();
        // The VTEP calculates the source port by performing the hash of the inner Ethernet frame's header.
        hash(hdr.udp.srcPort, HashAlgorithm.crc16, (bit<16>)0, { hdr.inner_ethernet }, (bit<17>)65536);
        hdr.udp.dstPort = VXLAN_DST_PORT;
        hdr.udp.totalLen = meta.inner_packet_len + (UDP_HDR_SIZE + VXLAN_HDR_SIZE + ETH_HDR_SIZE);
        hdr.udp.hdrChecksum = 0;

        hdr.vxlan.setValid();
        hdr.vxlan.rsv_1 = 0;
        hdr.vxlan.rsv_2 = 0;
        hdr.vxlan.flag = 0;
        hdr.vxlan.vni = vni;
    }

    direct_counter(CounterType.packets_and_bytes) vxlan_encap_counter;

    table encap_table {
        key = {
            standard_metadata.egress_port: ternary;
            hdr.ipv4.dstAddr: ternary;
            hdr.ethernet.dstAddr: ternary;
            hdr.arp.dstProtoAddr: ternary;
        }
        actions = {
            vxlan_encap;
            NoAction;
        }
        size = 1024;
        default_action = NoAction();
        counters = vxlan_encap_counter;
    }

    apply {
        // Prune multicast packet to ingress port to preventing loop
        if (standard_metadata.egress_port == standard_metadata.ingress_port)
            drop();
        else {
            if(hdr.ipv4.isValid()) {
                meta.inner_packet_len = hdr.ipv4.totalLen;
            } else if (hdr.arp.isValid()) {
                meta.inner_packet_len = ARP_HDR_SIZE;
            }
            encap_table.apply();
        }

        // Update port counters at index = ingress or egress port.
        if (standard_metadata.egress_port < MAX_PORTS) {
            tx_port_counter.count((bit<32>) standard_metadata.egress_port);
        }
        if (standard_metadata.ingress_port < MAX_PORTS) {
            rx_port_counter.count((bit<32>) standard_metadata.ingress_port);
        }
    }
}

/*************************************************************************
*************   C H E C K S U M    C O M P U T A T I O N   **************
*************************************************************************/

control MyComputeChecksum(inout headers  hdr, inout metadata meta) {
     apply {
        update_checksum(
            hdr.ipv4.isValid(),
                { hdr.ipv4.version,
                hdr.ipv4.ihl,
                hdr.ipv4.diffserv,
                hdr.ipv4.totalLen,
                hdr.ipv4.identification,
                hdr.ipv4.flags,
                hdr.ipv4.fragOffset,
                hdr.ipv4.ttl,
                hdr.ipv4.protocol,
                hdr.ipv4.srcAddr,
                hdr.ipv4.dstAddr },
                hdr.ipv4.hdrChecksum,
                HashAlgorithm.csum16);
        update_checksum(
            hdr.udp.isValid(),
                { hdr.udp.srcPort,
                hdr.udp.dstPort,
                hdr.udp.totalLen},
                hdr.udp.hdrChecksum,
                HashAlgorithm.csum16);
    }
}

/*************************************************************************
***********************  D E P A R S E R  *******************************
*************************************************************************/

control MyDeparser(packet_out packet, in headers hdr) {
    apply {
        packet.emit(hdr.packet_in);
        packet.emit(hdr.ethernet);
        packet.emit(hdr.arp);
        packet.emit(hdr.ipv4);
        packet.emit(hdr.udp);
        packet.emit(hdr.vxlan);
        packet.emit(hdr.inner_ethernet);
        packet.emit(hdr.inner_arp);
        packet.emit(hdr.inner_ipv4);
        packet.emit(hdr.inner_udp);
    }
}

/*************************************************************************
***********************  S W I T C H  *******************************
*************************************************************************/

V1Switch(
MyParser(),
MyVerifyChecksum(),
MyIngress(),
MyEgress(),
MyComputeChecksum(),
MyDeparser()
) main;
