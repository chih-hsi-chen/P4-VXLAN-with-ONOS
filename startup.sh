#!/bin/bash

onos localhost app activate org.onosproject.protocols.grpc \
org.onosproject.protocols.p4runtime \
org.onosproject.p4runtime \
org.onosproject.protocols.gnmi \
org.onosproject.generaldeviceprovider \
org.onosproject.protocols.gnoi \
org.onosproject.drivers.gnoi \
org.onosproject.drivers.p4runtime \
org.onosproject.drivers.gnmi \
org.onosproject.pipelines.basic \
org.onosproject.drivers.stratum \
org.onosproject.drivers.bmv2

onos-app localhost install! pipeconf-p4-final/target/pipeconf-1.0-SNAPSHOT.oar

cd p4/vxlan
make clean
make
