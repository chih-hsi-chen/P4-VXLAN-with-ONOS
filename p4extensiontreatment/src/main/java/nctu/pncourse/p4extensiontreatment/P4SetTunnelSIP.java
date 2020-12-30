package nctu.pncourse.p4extensiontreatment;

import org.onlab.packet.Ip4Address;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.store.serializers.MacAddressSerializer;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nicira set tunnel destination extension instruction.
 */
public class P4SetTunnelSIP extends AbstractExtension implements
        ExtensionTreatment {

    private Ip4Address tunnelSIP;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(new MacAddressSerializer(), Ip4Address.class)
            .register(byte[].class)
            .build("P4SetTunnelSIP");

    /**
     * Creates a new set tunnel destination instruction.
     */
    public P4SetTunnelSIP() {
        tunnelSIP = null;
    }

    /**
     * Creates a new set tunnel destination instruction with a particular IPv4
     * address.
     *
     * @param tunnelDst tunnel destination IPv4 address
     */
    public P4SetTunnelSIP(Ip4Address tunnelSIP) {
        checkNotNull(tunnelSIP);
        this.tunnelSIP = tunnelSIP;
    }

    /**
     * Gets the tunnel source Macaddress.
     *
     * @return tunnel source Macaddress
     */
    public Ip4Address tunnelSIP() {
        return tunnelSIP;
    }

    @Override
    public ExtensionTreatmentType type() {
        return P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP.type();
    }

    @Override
    public void deserialize(byte[] data) {
        tunnelSIP = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(tunnelSIP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunnelSIP);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof P4SetTunnelSIP) {
            P4SetTunnelSIP that = (P4SetTunnelSIP) obj;
            return Objects.equals(tunnelSIP, that.tunnelSIP);

        }
        return false;
    }

    @Override
    public String toString() {
        return "tunnelSIP = " + this.tunnelSIP;
    }
}
