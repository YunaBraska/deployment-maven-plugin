#!/bin/bash
echo "Cleaning maven cache"
mvn clean --quiet
rm -rf $(mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]')/*
echo "CLU cloning"
echo "CLU installing"
mvn --file=clu/pom.xml install -DskipTests=true --quiet
mvn generate-resources --quiet
mvn dependency:resolve-plugins --quiet
git clone https://github.com/YunaBraska/command-line-util.git clu
echo "CLU removing"
rm -rf clu
echo "Start testing"