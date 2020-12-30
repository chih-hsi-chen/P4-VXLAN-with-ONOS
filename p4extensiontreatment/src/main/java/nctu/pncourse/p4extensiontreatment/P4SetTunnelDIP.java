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
public class P4SetTunnelDIP extends AbstractExtension implements
        ExtensionTreatment {

    private Ip4Address tunnelDIP;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(new MacAddressSerializer(), Ip4Address.class)
            .register(byte[].class)
            .build("P4SetTunnelDIP");

    /**
     * Creates a new set tunnel destination instruction.
     */
    public P4SetTunnelDIP() {
        tunnelDIP = null;
    }

    /**
     * Creates a new set tunnel destination instruction with a particular IPv4
     * address.
     *
     * @param tunnelDst tunnel destination IPv4 address
     */
    public P4SetTunnelDIP(Ip4Address tunnelDIP) {
        checkNotNull(tunnelDIP);
        this.tunnelDIP = tunnelDIP;
    }

    /**
     * Gets the tunnel source Macaddress.
     *
     * @return tunnel source Macaddress
     */
    public Ip4Address tunnelDIP() {
        return tunnelDIP;
    }

    @Override
    public ExtensionTreatmentType type() {
        return P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP.type();
    }

    @Override
    public void deserialize(byte[] data) {
        tunnelDIP = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(tunnelDIP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunnelDIP);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof P4SetTunnelDIP) {
            P4SetTunnelDIP that = (P4SetTunnelDIP) obj;
            return Objects.equals(tunnelDIP, that.tunnelDIP);

        }
        return false;
    }

    @Override
    public String toString() {
        return "tunnelDIP = " + this.tunnelDIP;
    }
}
