#! /bin/bash
set -e
java -jar maven-deployment.jar $@ | tail -n 1
