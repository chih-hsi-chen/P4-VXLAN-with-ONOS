package nctu.pncourse.p4extensiontreatment;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
// import org.onosproject.store.serializers.MacAddressSerializer;

import java.util.Objects;

/**
 * Nicira set tunnel destination extension instruction.
 */
public class P4SetMulticastGRP extends AbstractExtension implements
        ExtensionTreatment {

    private short multicastGrp;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(Short.class)
            .build("P4SetMulticastGRP");

    /**
     * Creates a new set tunnel destination instruction.
     */
    public P4SetMulticastGRP() {
        multicastGrp = 0;
    }

    /**
     * Creates a new set tunnel destination instruction with a particular IPv4
     * address.
     *
     * @param tunnelDst tunnel destination IPv4 address
     */
    public P4SetMulticastGRP(short multicastGrp) {
        this.multicastGrp = multicastGrp;
    }

    /**
     * Gets the tunnel source Macaddress.
     *
     * @return tunnel source Macaddress
     */
    public short multicastGrp() {
        return multicastGrp;
    }

    @Override
    public ExtensionTreatmentType type() {
        return P4ExtensionTypes.P4ExtensionTreatmentType.P4_SET_MULTICAST_GRP.type();
    }

    @Override
    public void deserialize(byte[] data) {
        multicastGrp = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(multicastGrp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(multicastGrp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof P4SetMulticastGRP) {
            P4SetMulticastGRP that = (P4SetMulticastGRP) obj;
            return Objects.equals(multicastGrp, that.multicastGrp);

        }
        return false;
    }

    @Override
    public String toString() {
        return "multicastGrp = " + this.multicastGrp;
    }
}
