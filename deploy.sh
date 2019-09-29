#!/bin/bash
CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
"${CURRENT_DIR}"/build.sh
echo "Start deploy"
project_version=$(grep 'project.version' target/all.properties | cut -d'=' -f2)
mvn berlin.yuna:deployment-maven-plugin:"${project_version}":run -Djava.doc -Djava.source -Ddeploy -Dtag
echo "Finished deploy"
