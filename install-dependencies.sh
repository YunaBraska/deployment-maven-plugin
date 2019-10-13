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
#https://central.sonatype.org/pages/working-with-pgp-signatures.html
if [ -z ${GPG_KEY_UID+x} ]; then
  echo "GPG_KEY_UID not set - skipping gpg key import"
else
echo "import gpg key"
  gpg --keyserver hkp://pool.sks-keyservers.net --recv-keys "${GPG_KEY_UID}"
fi