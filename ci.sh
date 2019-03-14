#! /bin/bash
set -e

#Generates CI jar
mvn clean package -Dmaven.test.skip=true
java java -jar maven-deployment.jar $@
