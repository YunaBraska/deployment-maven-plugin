#!/bin/bash
set -e
if [ -z ${1+x} ]; then
  echo "Intalling dependencies"
  CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
  "${CURRENT_DIR}"/install-dependencies.sh
fi
echo "Prepare self usage"
mvn clean install -Dmaven.test.skip=true
project_version=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\[')
echo "Start building"
mvn berlin.yuna:deployment-maven-plugin:"${project_version}":run -Dclean -Dupdate.major -Dupdate.plugins -Dtest.run -Dbuilder -Dproperties.print
echo "Finished building"