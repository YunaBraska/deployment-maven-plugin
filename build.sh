#!/bin/bash
echo "CLU cloning"
git clone https://github.com/YunaBraska/command-line-util.git clu
echo "CLU installing"
mvn --file=clu/pom.xml install -DskipTests=true
echo "CLU removing"
rm -rf clu
echo "Start testing"
mvn clean verify