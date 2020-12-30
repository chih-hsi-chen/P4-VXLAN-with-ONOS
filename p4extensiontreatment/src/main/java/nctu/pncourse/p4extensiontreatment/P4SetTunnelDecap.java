package nctu.pncourse.p4extensiontreatment;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
// import org.onosproject.store.serializers.MacAddressSerializer;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nicira set tunnel destination extension instruction.
 */
public class P4SetTunnelDecap extends AbstractExtension implements
        ExtensionTreatment {

    private int dummyVal;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(Integer.class)
            .build("P4SetTunnelDecap");

    /**
     * Creates a new set tunnel destination instruction.
     */
    public P4SetTunnelDecap() {
        dummyVal = 0;
    }

    /**
     * Creates a new set tunnel destination instruction with a particular IPv4
     * address.
     *
     * @param tunnelDst tunnel destination IPv4 address
     */
    public P4SetTunnelDecap(int dummyVal) {
        checkNotNull(dummyVal);
        this.dummyVal = dummyVal;
    }

    /**
     * Gets the tunnel source Macaddress.
     *
     * @return tunnel source Macaddress
     */
    public int dummyVal() {
        return dummyVal;
    }

    @Override
    public ExtensionTreatmentType type() {
        return P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_TUNNEL_DECAP.type();
    }

    @Override
    public void deserialize(byte[] data) {
        dummyVal = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(dummyVal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dummyVal);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof P4SetTunnelDecap) {
            P4SetTunnelDecap that = (P4SetTunnelDecap) obj;
            return Objects.equals(dummyVal, that.dummyVal);

        }
        return false;
    }

    @Override
    public String toString() {
        return "dummyVal = " + this.dummyVal;
    }
}
