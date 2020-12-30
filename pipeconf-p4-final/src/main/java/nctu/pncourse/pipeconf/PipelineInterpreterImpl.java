/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nctu.pncourse.pipeconf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions.ExtensionInstructionWrapper;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModTunnelIdInstruction;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.net.pi.runtime.PiPacketOperation;

import nctu.pncourse.p4extensiontreatment.P4SetMulticastGRP;
import nctu.pncourse.p4extensiontreatment.P4SetTunnelDIP;
import nctu.pncourse.p4extensiontreatment.P4SetTunnelDmac;
import nctu.pncourse.p4extensiontreatment.P4SetTunnelSIP;
import nctu.pncourse.p4extensiontreatment.P4SetTunnelSmac;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.PortNumber.CONTROLLER;
import static org.onosproject.net.PortNumber.FLOOD;
import static org.onosproject.net.flow.instructions.Instruction.Type.OUTPUT;
import static org.onosproject.net.flow.instructions.Instruction.Type.EXTENSION;
import static org.onosproject.net.flow.instructions.Instruction.Type.L2MODIFICATION;
import static org.onosproject.net.pi.model.PiPacketOperationType.PACKET_OUT;

import static nctu.pncourse.p4extensiontreatment.P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC;
import static nctu.pncourse.p4extensiontreatment.P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC;
import static nctu.pncourse.p4extensiontreatment.P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP;
import static nctu.pncourse.p4extensiontreatment.P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP;
import static nctu.pncourse.p4extensiontreatment.P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_MULTICAST_GRP;
import static nctu.pncourse.p4extensiontreatment.P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_DECAP;

/**
 * Implementation of a pipeline interpreter for the mytunnel.p4 program.
 */
public final class PipelineInterpreterImpl
        extends AbstractHandlerBehaviour
        implements PiPipelineInterpreter {

    private static final String DOT = ".";
    private static final String HDR = "hdr";
    private static final String MY_INGRESS = "MyIngress";
    private static final String MY_EGRESS = "MyEgress";
	private static final String T_L2_FWD = "l2_forward";
    private static final String T_DECAP = "decap_table";
    private static final String T_ENCAP = "encap_table";
	private static final String T_IPV4_FWD = "ipv4_lpm";
    private static final String EGRESS_PORT = "egress_port";
    private static final String INGRESS_PORT = "ingress_port";
	private static final String ETHERNET = "ethernet";
	private static final String IPV4 = "ipv4";
	private static final String ARP = "arp";
    private static final String STANDARD_METADATA = "standard_metadata";
    private static final int PORT_FIELD_BITWIDTH = 9;

    
    private static final PiMatchFieldId EGRESS_PORT_ID =
            PiMatchFieldId.of(STANDARD_METADATA + DOT + "egress_port");
    private static final PiMatchFieldId ETH_DST_ID =
            PiMatchFieldId.of(HDR + DOT + ETHERNET + DOT + "dstAddr");
    private static final PiMatchFieldId ETH_TYPE_ID =
            PiMatchFieldId.of(HDR + DOT + ETHERNET + DOT + "etherType");
    private static final PiMatchFieldId IPV4_DST_ID =
            PiMatchFieldId.of(HDR + DOT + IPV4 + DOT + "dstAddr");
	private static final PiMatchFieldId ARP_PROTO_DST_ID =
            PiMatchFieldId.of(HDR + DOT + ARP + DOT + "dstProtoAddr");
            
    private static final PiTableId TABLE_L2_FWD_ID =
            PiTableId.of(MY_INGRESS + DOT + T_L2_FWD);
    private static final PiTableId TABLE_VXLAN_ENCAP_ID =
			PiTableId.of(MY_EGRESS + DOT + T_ENCAP);
	private static final PiTableId TABLE_VXLAN_DECAP_ID =
			PiTableId.of(MY_INGRESS + DOT + T_DECAP);
	private static final PiTableId TABLE_IPV4_FWD_ID =
			PiTableId.of(MY_INGRESS + DOT + T_IPV4_FWD);

    private static final PiActionId ACT_ID_NOP =
			PiActionId.of("NoAction");
    private static final PiActionId ACT_ID_SET_OUTPORT =
            PiActionId.of(MY_INGRESS + DOT + "set_out_port");
    private static final PiActionId ACT_ID_SEND_TO_CPU =
			PiActionId.of(MY_INGRESS + DOT + "send_to_cpu");
	private static final PiActionId ACT_ID_VXLAN_ENCAP =
			PiActionId.of(MY_EGRESS + DOT + "vxlan_encap");
	private static final PiActionId ACT_ID_VXLAN_DECAP =
			PiActionId.of(MY_INGRESS + DOT + "vxlan_decap");
	private static final PiActionId ACT_ID_L2_MULTICAST =
			PiActionId.of(MY_INGRESS + DOT + "l2_multicast");
	private static final PiActionId ACT_ID_L3_FWD =
			PiActionId.of(MY_INGRESS + DOT + "l3_forward");

    private static final PiActionParamId ACT_PARAM_ID_PORT =
			PiActionParamId.of("port");
	private static final PiActionParamId ACT_PARAM_ID_VNI =
			PiActionParamId.of("vni");
	private static final PiActionParamId ACT_PARAM_ID_SMAC =
			PiActionParamId.of("smac");
	private static final PiActionParamId ACT_PARAM_ID_DMAC =
			PiActionParamId.of("dmac");
	private static final PiActionParamId ACT_PARAM_ID_SIP =
			PiActionParamId.of("srcIP");
	private static final PiActionParamId ACT_PARAM_ID_DIP =
			PiActionParamId.of("dstIP");
	private static final PiActionParamId ACT_PARAM_ID_GRP =
            PiActionParamId.of("grp");

    private static final Map<Integer, PiTableId> TABLE_MAP =
            new ImmutableMap.Builder<Integer, PiTableId>()
					.put(0, TABLE_L2_FWD_ID)
					.put(1, TABLE_IPV4_FWD_ID)
					.put(2, TABLE_VXLAN_DECAP_ID)
					.put(3, TABLE_VXLAN_ENCAP_ID)
                    .build();

    private static final Map<Criterion.Type, PiMatchFieldId> CRITERION_MAP =
            ImmutableMap.<Criterion.Type, PiMatchFieldId>builder()
                    .put(Criterion.Type.IN_PORT, EGRESS_PORT_ID)
                    .put(Criterion.Type.ETH_DST, ETH_DST_ID)
                    .put(Criterion.Type.ETH_TYPE, ETH_TYPE_ID)
					.put(Criterion.Type.IPV4_DST, IPV4_DST_ID)
					.put(Criterion.Type.ARP_TPA, ARP_PROTO_DST_ID)
                    .build();

    @Override
    public Optional<PiMatchFieldId> mapCriterionType(Criterion.Type type) {
        return Optional.ofNullable(CRITERION_MAP.get(type));
    }

    @Override
    public Optional<PiTableId> mapFlowRuleTableId(int flowRuleTableId) {
        return Optional.ofNullable(TABLE_MAP.get(flowRuleTableId));
    }

    private PiAction.Builder createExtensionPiAction(PiAction.Builder builder, Instruction instr, PiTableId piTableId) 
        throws ExtensionPropertyException {
        ExtensionInstructionWrapper exInstruction = (ExtensionInstructionWrapper) instr;
        ExtensionTreatment exTreatment = exInstruction.extensionInstruction();

        if(exTreatment.type().equals(P4_SET_TUNNEL_SMAC.type())) {
            exTreatment = (P4SetTunnelSmac) exTreatment;
            MacAddress value = (MacAddress) exTreatment.getPropertyValue(P4ExtensionTreatmentInterpreter.TUNNEL_SMAC);

            
            builder.withParameter(new PiActionParam(
                ACT_PARAM_ID_SMAC, copyFrom(value.toBytes())
            ));
        } else if(exTreatment.type().equals(P4_SET_TUNNEL_DMAC.type())) {
            exTreatment = (P4SetTunnelDmac) exTreatment;
            MacAddress value = (MacAddress) exTreatment.getPropertyValue(P4ExtensionTreatmentInterpreter.TUNNEL_DMAC);
            
            builder.withParameter(new PiActionParam(
                ACT_PARAM_ID_DMAC, copyFrom(value.toBytes())
            ));
        } else if(exTreatment.type().equals(P4_SET_TUNNEL_SIP.type())) {
            exTreatment = (P4SetTunnelSIP) exTreatment;
            Ip4Address value = (Ip4Address) exTreatment.getPropertyValue(P4ExtensionTreatmentInterpreter.TUNNEL_SIP);
            
            builder.withParameter(new PiActionParam(
                ACT_PARAM_ID_SIP, copyFrom(value.toOctets())
            ));
        } else if(exTreatment.type().equals(P4_SET_TUNNEL_DIP.type())) {
            exTreatment = (P4SetTunnelDIP) exTreatment;
            Ip4Address value = (Ip4Address) exTreatment.getPropertyValue(P4ExtensionTreatmentInterpreter.TUNNEL_DIP);
            
            
            builder.withParameter(new PiActionParam(
                ACT_PARAM_ID_DIP, copyFrom(value.toOctets())
            ));
        } else if(exTreatment.type().equals(P4_SET_MULTICAST_GRP.type())) {
            exTreatment = (P4SetMulticastGRP) exTreatment;
            short value = (short) exTreatment.getPropertyValue(P4ExtensionTreatmentInterpreter.MULTICAST_GRP);
            
            builder.withId(ACT_ID_L2_MULTICAST);

            builder.withParameter(new PiActionParam(
                ACT_PARAM_ID_GRP, copyFrom(value)
            ));
        } else if (exTreatment.type().equals(P4_SET_TUNNEL_DECAP.type())) {
            builder.withId(ACT_ID_VXLAN_DECAP);
        }
        return builder;
    }

    @Override
    public PiAction mapTreatment(TrafficTreatment treatment, PiTableId piTableId)
            throws PiInterpreterException {
        
        int instructionSize = treatment.allInstructions().size();
        PiAction.Builder piAction = PiAction.builder();

        if (instructionSize == 0) {
            // 0 instructions means "NoAction"
            return PiAction.builder().withId(ACT_ID_NOP).build();
        }

        for (Instruction instr : treatment.allInstructions()) {
            if (instr.type() == EXTENSION) {
                try {
                    piAction = createExtensionPiAction(piAction, instr, piTableId);
                } catch (ExtensionPropertyException e) {
                    e.printStackTrace();
                }
            } else if (instr.type() == OUTPUT) {
                OutputInstruction outInstruction = (OutputInstruction) instr;
                PortNumber port = outInstruction.port();
                if (!port.isLogical()) {
                    if(!piTableId.equals(TABLE_IPV4_FWD_ID))
                        piAction.withId(ACT_ID_SET_OUTPORT);
                        
                    piAction.withParameter(new PiActionParam(ACT_PARAM_ID_PORT, copyFrom(port.toLong())));
                } else if (port.equals(CONTROLLER)) {
                    piAction.withId(ACT_ID_SEND_TO_CPU);
                } else {
                    throw new PiInterpreterException(format(
                            "Output on logical port '%s' not supported", port));
                }
            } else if (instr.type() == L2MODIFICATION) {
                if(instr instanceof ModEtherInstruction) {
                    MacAddress dst = ((ModEtherInstruction) instr).mac();

                    piAction.withId(ACT_ID_L3_FWD)
                            .withParameter(new PiActionParam(
                                ACT_PARAM_ID_DMAC, copyFrom(dst.toBytes())
                            ));
                }
                else if (instr instanceof ModTunnelIdInstruction) {
                    long vni = ((ModTunnelIdInstruction) instr).tunnelId();
                    piAction.withId(ACT_ID_VXLAN_ENCAP).withParameter(new PiActionParam(
                        ACT_PARAM_ID_VNI, copyFrom(vni)
                    ));
                }
            }
        }
        return piAction.build();
    }

    @Override
    public Collection<PiPacketOperation> mapOutboundPacket(OutboundPacket packet)
            throws PiInterpreterException {

        TrafficTreatment treatment = packet.treatment();

        // We support only packet-out with OUTPUT instructions.
        if (treatment.allInstructions().size() != 1 &&
                treatment.allInstructions().get(0).type() != OUTPUT) {
            throw new PiInterpreterException(
                    "Treatment not supported: " + treatment.toString());
        }

        Instruction instruction = treatment.allInstructions().get(0);
        PortNumber port = ((OutputInstruction) instruction).port();
        List<PiPacketOperation> piPacketOps = Lists.newArrayList();

        if (!port.isLogical()) {
            piPacketOps.add(createPiPacketOp(packet.data(), port.toLong()));
        } else if (port.equals(FLOOD)) {
            // Since mytunnel.p4 does not support flooding, we create a packet
            // operation for each switch port.
            // DeviceService deviceService = handler().get(DeviceService.class);
            // DeviceId deviceId = packet.sendThrough();
            // for (Port p : deviceService.getPorts(deviceId)) {
            //     piPacketOps.add(createPiPacketOp(packet.data(), p.number().toLong()));
            // }
            long P4FloodPort = 500;
            piPacketOps.add(createPiPacketOp(packet.data(), P4FloodPort));
        } else {
            throw new PiInterpreterException(format(
                    "Output on logical port '%s' not supported", port));
        }

        return piPacketOps;
    }

    @Override
    public InboundPacket mapInboundPacket(PiPacketOperation packetIn, DeviceId deviceId)
            throws PiInterpreterException {
        // We assume that the packet is ethernet, which is fine since mytunnel.p4
        // can deparse only ethernet packets.
        Ethernet ethPkt;

        try {
            ethPkt = Ethernet.deserializer().deserialize(
                    packetIn.data().asArray(), 0, packetIn.data().size());
        } catch (DeserializationException dex) {
            throw new PiInterpreterException(dex.getMessage());
        }

        // Returns the ingress port packet metadata.
        Optional<PiPacketMetadata> packetMetadata = packetIn.metadatas().stream()
                .filter(metadata -> metadata.id().toString().equals(INGRESS_PORT))
                .findFirst();

        if (packetMetadata.isPresent()) {
            short s = packetMetadata.get().value().asReadOnlyBuffer().getShort();
            ConnectPoint receivedFrom = new ConnectPoint(
                    deviceId, PortNumber.portNumber(s));
            return new DefaultInboundPacket(
                    receivedFrom, ethPkt, packetIn.data().asReadOnlyBuffer());
        } else {
            throw new PiInterpreterException(format(
                    "Missing metadata '%s' in packet-in received from '%s': %s",
                    INGRESS_PORT, deviceId, packetIn));
        }
    }

    private PiPacketOperation createPiPacketOp(ByteBuffer data, long portNumber)
            throws PiInterpreterException {
        PiPacketMetadata metadata = createPacketMetadata(portNumber);
        return PiPacketOperation.builder()
                .withType(PACKET_OUT)
                .withData(copyFrom(data))
                .withMetadatas(ImmutableList.of(metadata))
                .build();
    }

    private PiPacketMetadata createPacketMetadata(long portNumber)
            throws PiInterpreterException {
        try {
            return PiPacketMetadata.builder()
                    .withId(PiPacketMetadataId.of(EGRESS_PORT))
                    .withValue(copyFrom(portNumber).fit(PORT_FIELD_BITWIDTH))
                    .build();
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            throw new PiInterpreterException(format(
                    "Port number %d too big, %s", portNumber, e.getMessage()));
        }
    }
}
