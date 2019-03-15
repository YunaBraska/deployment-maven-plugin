#FROM openjdk:8-jre
FROM openjdk:8-jdk-alpine
MAINTAINER Yuna Morgenstern

# SETUP SERVICES
RUN set -eux \
    && apk add git maven
# SETUP ENVIRONMENT
RUN set -eux \
    && git clone https://github.com/YunaBraska/maven-gpg-plugin /opt/maven-gpg-plugin \
    && mvn clean install -f=/opt/maven-gpg-plugin -Drat.ignoreErrors=true \
    && MVN_REPO_PATH=$(mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]') \
    && MVN_REPO_PATH=$(mvn help:evaluate -Dexpression=settings.localRepository | grep -v '\[INFO\]')