#!/bin/bash
set -e
echo "Cleaning maven cache"
HELPER=$(mvn help:evaluate -Dexpression=settings.localRepository)
rm -rf $(echo "${HELPER}" | grep -v '\[INFO\]')/berlin/yuna/$(echo "${HELPER}" | grep -i building | awk '{print $3}')
echo "CLU cloning"
git clone https://github.com/YunaBraska/command-line-util.git clu
echo "CLU installing"
mvn --file=clu/pom.xml install -DskipTests=true --quiet
echo "CLU removing"
rm -rf clu