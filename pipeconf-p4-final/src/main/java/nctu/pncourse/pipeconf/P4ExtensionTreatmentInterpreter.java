package nctu.pncourse.pipeconf;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onosproject.codec.CodecContext;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.ExtensionTreatmentCodec;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import nctu.pncourse.p4extensiontreatment.P4SetMulticastGRP;
import nctu.pncourse.p4extensiontreatment.P4SetTunnelDIP;
import nctu.pncourse.p4extensiontreatment.P4SetTunnelDecap;
import nctu.pncourse.p4extensiontreatment.P4SetTunnelDmac;
import nctu.pncourse.p4extensiontreatment.P4SetTunnelSIP;
import nctu.pncourse.p4extensiontreatment.P4SetTunnelSmac;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

import nctu.pncourse.p4extensiontreatment.P4ExtensionTypes.P4ExtensionTreatmentType;

public class P4ExtensionTreatmentInterpreter extends AbstractHandlerBehaviour
        implements ExtensionTreatmentCodec, ExtensionTreatmentResolver {

    public static final String TUNNEL_SMAC = "tunnelSmac";
    public static final String TUNNEL_DMAC = "tunnelDmac";
    public static final String TUNNEL_SIP = "tunnelSIP";
    public static final String TUNNEL_DIP = "tunnelDIP";
    public static final String MULTICAST_GRP = "multicastGrp";
    public static final String DUMMY = "dummyVal";

    private static final String TYPE = "type";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in NiciraExtensionTreatmentInterpreter";

    @Override
    public ExtensionTreatment getExtensionInstruction(ExtensionTreatmentType type) {
        if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type())) {
            return new P4SetTunnelSmac();
        } else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type())) {
            return new P4SetTunnelDmac();
        } else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP.type())) {
            return new P4SetTunnelSIP();
        } else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP.type())) {
            return new P4SetTunnelDIP();
        } else if (type.equals(P4ExtensionTreatmentType.P4_SET_MULTICAST_GRP.type())) {
            return new P4SetMulticastGRP();
        } else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DECAP.type())) {
            return new P4SetTunnelDecap();
        }
        throw new UnsupportedOperationException("Driver does not support extension type " + type.toString());
    }

    @Override
    public ObjectNode encode(ExtensionTreatment extensionTreatment, CodecContext context) {
        checkNotNull(extensionTreatment, "Extension treatment cannot be null");
        ExtensionTreatmentType type = extensionTreatment.type();
        ObjectNode root = context.mapper().createObjectNode();

        if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type())) {
            P4SetTunnelSmac tunnelSmac = (P4SetTunnelSmac) extensionTreatment;
            root.set(TUNNEL_SMAC, context.codec(P4SetTunnelSmac.class).encode(tunnelSmac, context));
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type())) {
            P4SetTunnelDmac tunnelDmac = (P4SetTunnelDmac) extensionTreatment;
            root.set(TUNNEL_DMAC, context.codec(P4SetTunnelDmac.class).encode(tunnelDmac, context));
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP.type())) {
            P4SetTunnelSIP tunnelSIP = (P4SetTunnelSIP) extensionTreatment;
            root.set(TUNNEL_SIP, context.codec(P4SetTunnelSIP.class).encode(tunnelSIP, context));
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP.type())) {
            P4SetTunnelDIP tunnelDIP = (P4SetTunnelDIP) extensionTreatment;
            root.set(TUNNEL_DIP, context.codec(P4SetTunnelDIP.class).encode(tunnelDIP, context));
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_MULTICAST_GRP.type())) {
            P4SetMulticastGRP multicastGrp = (P4SetMulticastGRP) extensionTreatment;
            root.set(MULTICAST_GRP, context.codec(P4SetMulticastGRP.class).encode(multicastGrp, context));
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DECAP.type())) {
            P4SetTunnelDecap tunnelDecap = (P4SetTunnelDecap) extensionTreatment;
            root.set(DUMMY, context.codec(P4SetTunnelDecap.class).encode(tunnelDecap, context));
        }
        
        return root;
    }
    
    @Override
    public ExtensionTreatment decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse extension type
        int typeInt = nullIsIllegal(json.get(TYPE), TYPE + MISSING_MEMBER_MESSAGE).asInt();
        ExtensionTreatmentType type = new ExtensionTreatmentType(typeInt);

        if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type())) {
            return context.codec(P4SetTunnelSmac.class).decode(json, context);
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type())) {
            return context.codec(P4SetTunnelDmac.class).decode(json, context);
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP.type())) {
            return context.codec(P4SetTunnelSIP.class).decode(json, context);
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP.type())) {
            return context.codec(P4SetTunnelDIP.class).decode(json, context);
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_MULTICAST_GRP.type())) {
            return context.codec(P4SetMulticastGRP.class).decode(json, context);
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DECAP.type())) {
            return context.codec(P4SetTunnelDecap.class).decode(json, context);
        }

        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }

    // public enum P4ExtensionTreatmentType {
    //     P4_SET_TUNNEL_SMAC(0),
    //     P4_SET_TUNNEL_DMAC(1),
    //     P4_SET_TUNNEL_SIP(2),
    //     P4_SET_TUNNEL_DIP(3),
    //     P4_SET_MULTICAST_GRP(4),
    //     P4_SET_TUNNEL_DECAP(5);

    //     private ExtensionTreatmentType type;

    //     /**
    //      * Creates a new named extension treatment type.
    //      *
    //      * @param type type code
    //      */
    //     P4ExtensionTreatmentType(int type) {
    //         this.type = new ExtensionTreatmentType(type);
    //     }

    //     /**
    //      * Gets the extension type object for this named type code.
    //      *
    //      * @return extension type object
    //      */
    //     public ExtensionTreatmentType type() {
    //         return type;
    //     }
    // }
}
