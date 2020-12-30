package nctu.pncourse.p4extensiontreatment;

import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

public final class P4ExtensionTypes {
    public enum P4ExtensionTreatmentType {
        P4_SET_TUNNEL_SMAC(0),
        P4_SET_TUNNEL_DMAC(1),
        P4_SET_TUNNEL_SIP(2),
        P4_SET_TUNNEL_DIP(3),
        P4_SET_MULTICAST_GRP(4),
        P4_SET_TUNNEL_DECAP(5);

        private ExtensionTreatmentType type;

        /**
         * Creates a new named extension treatment type.
         *
         * @param type type code
         */
        P4ExtensionTreatmentType(int type) {
            this.type = new ExtensionTreatmentType(type);
        }

        /**
         * Gets the extension type object for this named type code.
         *
         * @return extension type object
         */
        public ExtensionTreatmentType type() {
            return type;
        }
    }
}
