/*
 * Copyright 2020-present Open Networking Foundation
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
package nctu.pncourse.vxlan;

import com.google.common.collect.ImmutableSet;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;

import static org.onlab.util.Tools.get;

import nctu.pncourse.p4extensiontreatment.P4ExtensionTypes.P4ExtensionTreatmentType;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
           service = {SomeInterface.class},
           property = {
               "someProperty=Some Default String Value",
           })
public class AppComponent implements SomeInterface {

    public static final String TUNNEL_SMAC = "tunnelSmac";
    public static final String TUNNEL_DMAC = "tunnelDmac";
    public static final String TUNNEL_SIP = "tunnelSIP";
    public static final String TUNNEL_DIP = "tunnelDIP";
    public static final String MULTICAST_GRP = "multicastGrp";
    public static final String DUMMY = "dummyVal";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ApplicationId appId;

    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("nctu.pncourse.vxlan");
        log.info("Started");
        flowRuleForS1();
        flowRuleForS2();
        flowRuleForS3();
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
        flowRuleService.removeFlowRulesById(appId);
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
        log.info("Reconfigured");
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }

    private void flowRuleForS1() {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;
        FlowRule.Builder rule;
        DeviceId deviceId;
        long tunnelId = 30;
        int priority = 40005;
        
        int table_l2_fwd = 0;
        int table_ipv4_fwd = 1;
        int table_vxlan_decap = 2;
        int table_vxlan_encap = 3;

        // Switch 1
        deviceId = DeviceId.deviceId("device:bmv2:s1");
        // VXLAN ENCAP
        selector = DefaultTrafficSelector.builder()
                                    .matchIPDst(IpPrefix.valueOf("10.0.1.64/26"))
                                    .matchInPort(PortNumber.fromString("3"));
        treatment = DefaultTrafficTreatment.builder();

        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type(), TUNNEL_SMAC, MacAddress.valueOf("00:00:00:00:00:01"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type(), TUNNEL_DMAC, MacAddress.valueOf("00:00:00:00:00:02"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP.type(), TUNNEL_SIP, Ip4Address.valueOf("192.168.1.1"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP.type(), TUNNEL_DIP, Ip4Address.valueOf("192.169.1.2"));
        treatment.setTunnelId(tunnelId);

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_vxlan_encap)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // VXLAN ENCAP
        selector = DefaultTrafficSelector.builder()
                        .matchArpTpa(Ip4Address.valueOf("10.0.1.64"))
                        .matchInPort(PortNumber.fromString("3"));
        treatment = DefaultTrafficTreatment.builder();

        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type(), TUNNEL_SMAC, MacAddress.valueOf("00:00:00:00:00:01"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type(), TUNNEL_DMAC, MacAddress.valueOf("00:00:00:00:00:02"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP.type(), TUNNEL_SIP, Ip4Address.valueOf("192.168.1.1"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP.type(), TUNNEL_DIP, Ip4Address.valueOf("192.169.1.2"));
        treatment.setTunnelId(tunnelId);

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_vxlan_encap)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // VXLAN DECAP
        selector = DefaultTrafficSelector.builder().matchIPDst(IpPrefix.valueOf("192.168.1.1/32"));
        treatment = DefaultTrafficTreatment.builder();
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DECAP.type(), DUMMY, 1);

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_vxlan_decap)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // L2 Multicast
        selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.valueOf("ff:ff:ff:ff:ff:ff"));
        treatment = DefaultTrafficTreatment.builder();
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_MULTICAST_GRP.type(), MULTICAST_GRP, (short) 1);

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_l2_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // Send to Host 1
        selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.valueOf("00:00:00:00:00:01"));
        treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.fromString("1"));

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_l2_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // Send to Host 2
        selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.valueOf("00:00:00:00:00:02"));
        treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.fromString("2"));

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_l2_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // Send to Host 3
        selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.valueOf("00:00:00:00:00:03"));
        treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.fromString("3"));

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_l2_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());
    }

    private void flowRuleForS2() {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;
        FlowRule.Builder rule;
        DeviceId deviceId = DeviceId.deviceId("device:bmv2:s2");
        int priority = 40005;
        int table_ipv4_fwd = 1;

        // L3 Forward to S3
        selector = DefaultTrafficSelector.builder().matchIPDst(IpPrefix.valueOf("192.169.1.2/32"));
        treatment = DefaultTrafficTreatment.builder()
                                           .setEthDst(MacAddress.valueOf("00:00:00:00:00:03"))
                                           .setOutput(PortNumber.fromString("2"));

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_ipv4_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // L3 Forward to S1
        selector = DefaultTrafficSelector.builder().matchIPDst(IpPrefix.valueOf("192.168.1.1/32"));
        treatment = DefaultTrafficTreatment.builder()
                                           .setEthDst(MacAddress.valueOf("00:00:00:00:00:01"))
                                           .setOutput(PortNumber.fromString("1"));

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_ipv4_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());
    }

    private void flowRuleForS3() {
        TrafficSelector.Builder selector;
        TrafficTreatment.Builder treatment;
        FlowRule.Builder rule;
        DeviceId deviceId;
        long tunnelId = 30;
        int priority = 40005;
        
        int table_l2_fwd = 0;
        int table_ipv4_fwd = 1;
        int table_vxlan_decap = 2;
        int table_vxlan_encap = 3;

        // Switch 1
        deviceId = DeviceId.deviceId("device:bmv2:s3");
        // VXLAN ENCAP
        selector = DefaultTrafficSelector.builder()
                                    .matchIPDst(IpPrefix.valueOf("10.0.1.0/26"))
                                    .matchInPort(PortNumber.fromString("2"));
        treatment = DefaultTrafficTreatment.builder();

        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type(), TUNNEL_SMAC, MacAddress.valueOf("00:00:00:00:00:03"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type(), TUNNEL_DMAC, MacAddress.valueOf("00:00:00:00:00:02"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP.type(), TUNNEL_SIP, Ip4Address.valueOf("192.169.1.2"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP.type(), TUNNEL_DIP, Ip4Address.valueOf("192.168.1.1"));
        treatment.setTunnelId(tunnelId);

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_vxlan_encap)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // ARP VXLAN ENCAP (To Host1)
        selector = DefaultTrafficSelector.builder()
                        .matchArpTpa(Ip4Address.valueOf("10.0.1.1"))
                        .matchInPort(PortNumber.fromString("2"));
        treatment = DefaultTrafficTreatment.builder();

        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type(), TUNNEL_SMAC, MacAddress.valueOf("00:00:00:00:00:03"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type(), TUNNEL_DMAC, MacAddress.valueOf("00:00:00:00:00:02"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP.type(), TUNNEL_SIP, Ip4Address.valueOf("192.169.1.2"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP.type(), TUNNEL_DIP, Ip4Address.valueOf("192.168.1.1"));
        treatment.setTunnelId(tunnelId);

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_vxlan_encap)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // ARP VXLAN ENCAP (To Host2)
        selector = DefaultTrafficSelector.builder()
                        .matchArpTpa(Ip4Address.valueOf("10.0.1.2"))
                        .matchInPort(PortNumber.fromString("2"));
        treatment = DefaultTrafficTreatment.builder();

        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type(), TUNNEL_SMAC, MacAddress.valueOf("00:00:00:00:00:03"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type(), TUNNEL_DMAC, MacAddress.valueOf("00:00:00:00:00:02"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_SIP.type(), TUNNEL_SIP, Ip4Address.valueOf("192.169.1.2"));
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DIP.type(), TUNNEL_DIP, Ip4Address.valueOf("192.168.1.1"));
        treatment.setTunnelId(tunnelId);

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_vxlan_encap)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // VXLAN DECAP
        selector = DefaultTrafficSelector.builder().matchIPDst(IpPrefix.valueOf("192.169.1.2/32"));
        treatment = DefaultTrafficTreatment.builder();
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_TUNNEL_DECAP.type(), DUMMY, 1);

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_vxlan_decap)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // L2 Multicast
        selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.valueOf("ff:ff:ff:ff:ff:ff"));
        treatment = DefaultTrafficTreatment.builder();
        addExTreatment(treatment, deviceId, P4ExtensionTreatmentType.P4_SET_MULTICAST_GRP.type(), MULTICAST_GRP, (short) 1);

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_l2_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // Send to Host 1
        selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.valueOf("00:00:00:00:00:01"));
        treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.fromString("2"));

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_l2_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // Send to Host 2
        selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.valueOf("00:00:00:00:00:02"));
        treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.fromString("2"));

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_l2_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());

        // Send to Host 3
        selector = DefaultTrafficSelector.builder().matchEthDst(MacAddress.valueOf("00:00:00:00:00:03"));
        treatment = DefaultTrafficTreatment.builder().setOutput(PortNumber.fromString("1"));

        rule = DefaultFlowRule.builder()
                              .forDevice(deviceId)
                              .forTable(table_l2_fwd)
                              .withSelector(selector.build())
                              .withTreatment(treatment.build())
                              .withPriority(priority)
                              .fromApp(appId)
                              .makePermanent();

        flowRuleService.applyFlowRules(rule.build());
    }

    private TrafficTreatment.Builder addExTreatment(TrafficTreatment.Builder selector, DeviceId deviceId, ExtensionTreatmentType type, String propertyName, Object value) {
        DriverHandler handler = driverService.createHandler(deviceId);
        ExtensionTreatmentResolver resolver =  handler.behaviour(ExtensionTreatmentResolver.class);

        // Get an instance of the NICIRA_SET_TUNNEL_DST ExtensionInstruction from the driver
        ExtensionTreatment extension = resolver.getExtensionInstruction(type);

        if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type()) ||
            type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type())
        ) {
            value = (MacAddress) value;
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_SMAC.type()) ||
                 type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DMAC.type())) {
            value = (Ip4Address) value;
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_MULTICAST_GRP.type())) {
            value = (short) value;
        }
        else if (type.equals(P4ExtensionTreatmentType.P4_SET_TUNNEL_DECAP.type())) {
            value = (int) value;
        }

        // Set the tunnelDst property of the extension instruction.
        try {
            extension.setPropertyValue(propertyName, value);
        } catch (ExtensionPropertyException e) {
            log.error("Error setting extension property", e);
        }
        
        // Build a treatment that includes the extension (of course we could add other instructions to this treatment as well)
        return selector.extension(extension, deviceId);
    }
}
