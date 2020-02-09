#!/bin/bash
set -e
CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
"${CURRENT_DIR}"/build.sh $1
echo "Start deploy"
project_version=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\[')
mvn berlin.yuna:deployment-maven-plugin:"${project_version}":run -Djava.doc -Djava.source -Ddeploy.snapshot -Dtag -X
echo "Finished deploy"
