#!/bin/bash
echo "Cleaning maven cache"
rm -rf $(mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]')/*
echo "CLU cloning"
git clone https://github.com/YunaBraska/command-line-util.git clu
echo "CLU installing"
mvn --file=clu/pom.xml install -DskipTests=true --quiet
echo "CLU removing"
rm -rf clu
echo "Start testing"