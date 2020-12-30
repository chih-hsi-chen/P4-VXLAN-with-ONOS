package nctu.pncourse.p4extensiontreatment;

import org.onlab.packet.MacAddress;
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
public class P4SetTunnelDmac extends AbstractExtension implements
        ExtensionTreatment {

    private MacAddress tunnelDmac;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(new MacAddressSerializer(), MacAddress.class)
            .register(byte[].class)
            .build("P4SetTunnelDmac");

    /**
     * Creates a new set tunnel destination instruction.
     */
    public P4SetTunnelDmac() {
        tunnelDmac = null;
    }

    /**
     * Creates a new set tunnel destination instruction with a particular IPv4
     * address.
     *
     * @param tunnelDst tunnel destination IPv4 address
     */
    public P4SetTunnelDmac(MacAddress tunnelDmac) {
        checkNotNull(tunnelDmac);
        this.tunnelDmac = tunnelDmac;
    }

    /**
     * Gets the tunnel source Macaddress.
     *
     * @return tunnel source Macaddress
     */
    public MacAddress tunnelDmac() {
        return tunnelDmac;
    }

    @Override
    public ExtensionTreatmentType type() {
        return P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type();
    }

    @Override
    public void deserialize(byte[] data) {
        tunnelDmac = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(tunnelDmac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunnelDmac);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof P4SetTunnelDmac) {
            P4SetTunnelDmac that = (P4SetTunnelDmac) obj;
            return Objects.equals(tunnelDmac, that.tunnelDmac);

        }
        return false;
    }

    @Override
    public String toString() {
        return "tunnelDmac = " + this.tunnelDmac;
    }
}
