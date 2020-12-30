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
public class P4SetTunnelSmac extends AbstractExtension implements
        ExtensionTreatment {

    private MacAddress tunnelSmac;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(new MacAddressSerializer(), MacAddress.class)
            .register(byte[].class)
            .build("P4SetTunnelSmac");

    /**
     * Creates a new set tunnel destination instruction.
     */
    public P4SetTunnelSmac() {
        tunnelSmac = null;
    }

    /**
     * Creates a new set tunnel destination instruction with a particular IPv4
     * address.
     *
     * @param tunnelDst tunnel destination IPv4 address
     */
    public P4SetTunnelSmac(MacAddress tunnelSmac) {
        checkNotNull(tunnelSmac);
        this.tunnelSmac = tunnelSmac;
    }

    /**
     * Gets the tunnel source Macaddress.
     *
     * @return tunnel source Macaddress
     */
    public MacAddress tunnelSmac() {
        return tunnelSmac;
    }

    @Override
    public ExtensionTreatmentType type() {
        return P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type();
    }

    @Override
    public void deserialize(byte[] data) {
        tunnelSmac = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(tunnelSmac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunnelSmac);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof P4SetTunnelSmac) {
            P4SetTunnelSmac that = (P4SetTunnelSmac) obj;
            return Objects.equals(tunnelSmac, that.tunnelSmac);

        }
        return false;
    }

    @Override
    public String toString() {
        return "tunnelSmac = " + this.tunnelSmac;
    }
}
