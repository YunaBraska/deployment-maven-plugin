#!/bin/bash
echo "Cleaning maven cache"
mvn dependency:purge-local-repository --quiet
mvn clean --quiet
mvn generate-resources --quiet
mvn dependency:resolve-plugins --quiet
echo "CLU cloning"
git clone https://github.com/YunaBraska/command-line-util.git clu
echo "CLU installing"
mvn --file=clu/pom.xml install -DskipTests=true --quiet
echo "CLU removing"
rm -rf clu
echo "Start testing"