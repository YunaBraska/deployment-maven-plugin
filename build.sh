#!/bin/bash
git clone https://github.com/YunaBraska/command-line-util.git clu
mvn --file=clu/pom.xml install
rm -rf clu
mvn clean verify