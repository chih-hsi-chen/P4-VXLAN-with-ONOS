#!/bin/bash

cd p4extensiontreatment
mvn clean install -DskipTests
mvn install:install-file -Dfile=target/p4extensiontreatment-1.0-SNAPSHOT.jar -DgroupId=nctu.pncourse -DartifactId=p4extensiontreatment -Dversion=1.0-SNAPSHOT -Dpackaging=jar -DgeneratePom=true

cd ../pipeconf-p4-final
mvn clean install -DskipTests

cd ../vxlan-app
mvn clean install -DskipTests