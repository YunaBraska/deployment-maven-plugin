#! /bin/bash
set -e

JAVA_VERSION='1.8'
ENCODING='UTF-8'
MVN_PROFILES="true"
GPG_PASSPHRASE=''
PROJECT_VERSION=''
GIT_TAG=''
MAVEN_OPTIONS=''
IS_POM='false'
MVN_CLEAN='true'
MVN_UPDATE='true'
MVN_JAVA_DOC='true'
MVN_SOURCE='true'
MVN_TAG='true'

MVN_CLEAN_CMD='clean verify'
MVN_CLEAN_PARAM='generate-resources generate-sources dependency:resolve-plugins'
MVN_UPDATE_CMD="versions:update-parent versions:update-properties versions:update-child-modules versions:use-latest-releases versions:use-next-snapshots versions:commit -DallowSnapshots=true -DgenerateBackupPoms=false"
MVN_DOC_CMD="javadoc:jar"
MVN_DOC_PARAM="-Dnodeprecatedlist -Dquiet=true"
MVN_SOURCE_CMD="source:jar-no-fork"
MVN_GPG_CMD="gpg:sign"
MVN_GPG_PARAM="-Darguments=-D--pinentry-mode -Darguments=loopback"
MVN_TAG_CMD="scm:tag -Dtag="

for i in "$@"
do
case $i in
    -V=*|--PROJECT_VERSION=*)
    PROJECT_VERSION="${i#*=}"
    MVN_UPDATE_CMD="versions:set -DnewVersion=${PROJECT_VERSION} ${MVN_UPDATE_CMD}"
    shift # past argument=value
    ;;
    -C=*|--MVN_CLEAN=*)
    MVN_CLEAN="${i#*=}"
    shift # past argument=value
    ;;
    -D=*|--MVN_JAVA_DOC=*)
    MVN_JAVA_DOC="${i#*=}"
    shift # past argument=value
    ;;
    -E=*|--ENCODING=*)
    ENCODING="${i#*=}"
    shift # past argument=value
    ;;
    -G=*|--GPG_PASSPHRASE=*)
    GPG_PASSPHRASE="${i#*=}"
    shift # past argument=value
    ;;
    -J=*|--JAVA_VERSION=*)
    JAVA_VERSION="${i#*=}"
    shift # past argument=value
    ;;
    -P=*|--MVN_PROFILES=*)
    MVN_PROFILES="${i#*=}"
    shift # past argument=value
    ;;
    -S=*|--MVN_SOURCE=*)
    MVN_SOURCE="${i#*=}"
    shift # past argument=value
    ;;
    -T=*|--MVN_TAG=*)
    MVN_TAG="${i#*=}"
    shift # past argument=value
    ;;
    -U=*|--MVN_UPDATE=*)
    MVN_UPDATE="${i#*=}"
    shift # past argument=value
    ;;
    --default)
    DEFAULT=YES
    shift # past argument with no value
    ;;
    *)
          # unknown option
    ;;
esac
done


if [ ${MVN_PROFILES} = "true" ]; then
    echo "########## READING MVN_PROFILES [${MVN_PROFILES}] ##########"
    MVN_PROFILES=$(mvn help:all-profiles | grep "Profile Id" | cut -d' ' -f 5 | xargs | tr ' ' ',')
    if [ ${#MVN_PROFILES} -ge 4 ]; then MVN_PROFILES="-p ${MVN_PROFILES}"; MVN_CLEAN_PARAM="${MVN_CLEAN_PARAM} ${MVN_PROFILES}" ; else MVN_PROFILES=""; fi
else
  MVN_PROFILES=""
fi

echo "########## SETUP ENVIRONMENT ##########"
if [[ $(grep '<packaging>pom</packaging>' pom.xml | wc -l) = *1* ]] ; then IS_POM='true'; fi
if [ ${MVN_UPDATE} = "false" ]; then MVN_UPDATE_CMD=''; else MVN_UPDATE='true'; fi
if [ ${MVN_CLEAN} = "false" ]; then MVN_CLEAN_CMD=''; else MVN_UPDATE='true'; MVN_CLEAN_CMD="dependency:purge-local-repository ${MVN_CLEAN_CMD}"; fi
if [ ${MVN_JAVA_DOC} = "false" ]; then MVN_DOC_CMD=''; MVN_DOC_PARAM=''; else MVN_JAVA_DOC='true'; fi
if [ ${MVN_SOURCE} = "false" ]; then MVN_SOURCE_CMD=''; else MVN_SOURCE='true'; fi
if [ ${#GPG_PASSPHRASE} -ge 2 ]; then MVN_GPG_PARAM="-Dgpg.passphrase=${GPG_PASSPHRASE} ${MVN_GPG_PARAM}"; else MVN_GPG_CMD=''; MVN_GPG_PARAM=''; fi
if [ ${MVN_TAG} = "false" ]; then MVN_TAG_CMD=''; else MVN_TAG_CMD="${MVN_TAG_CMD}${PROJECT_VERSION}"; MVN_TAG='true'; GIT_TAG=$(git describe --always); fi
if [ ${#PROJECT_VERSION} -le 2 ] || [ ${GIT_TAG} = "${PROJECT_VERSION}" ]; then MVN_TAG_CMD=''; ${MVN_TAG} = "false"; echo "[WARN] Tagging failed cause PROJECT_VERSION [${PROJECT_VERSION}] is not set or the GIT_TAG [${GIT_TAG}] already exists"; fi
MAVEN_OPTIONS="-Dproject.build.sourceEncoding=${ENCODING} -Dproject.encoding=${ENCODING} -Dproject.reporting.outputEncoding=${ENCODING} -Dmaven.compiler.source=${JAVA_VERSION} -Dmaven.compiler.target=${JAVA_VERSION}"


#echo "GPG_PASSPHRASE [${GPG_PASSPHRASE}]"
echo "PROJECT_VERSION [${PROJECT_VERSION}]"
echo "JAVA_VERSION [${JAVA_VERSION}]"
echo "ENCODING [${ENCODING}]"
echo "IS_POM [${IS_POM}]"
echo "MVN_PROFILES [${MVN_PROFILES}]"
echo "MVN_CLEAN [${MVN_CLEAN}]"
echo "MVN_UPDATE [${MVN_UPDATE}]"
echo "MVN_JAVA_DOC [${MVN_JAVA_DOC}]"
echo "MVN_SOURCE [${MVN_SOURCE}]"
echo "MVN_TAG [${MVN_TAG}]"

if [ ${#GPG_PASSPHRASE} -ge 2 ]; then
    export GPG_TTY=$(tty)
    gpg -k
    #signing dummy so that maven is using right agent without settings XML
    echo 'gpg.activation' > gpg.act; gpg --sign --armor --always-trust --passphrase=${GPG_PASSPHRASE} --pinentry-mode loopback gpg.act; rm -rf gpg.act gpg.act.asc
    #for x in target/*.jar; do gpg --sign --armor --always-trust --passphrase=${GPG_PASSPHRASE} --pinentry-mode loopback "${x}"; done
    #mvn package gpg:sign -Dgpg.passphrase=${GPG_PASSPHRASE} -Darguments=-D--pinentry-mode -Darguments=loopback
    #for x in target/*.asc; do gpg --verify "${x}"; done
fi

MVN_ALL="${MVN_CLEAN_CMD} ${MVN_CLEAN_PARAM} ${MVN_UPDATE_CMD} ${MVN_DOC_CMD} ${MVN_DOC_PARAM} ${MVN_SOURCE_CMD} ${MVN_GPG_CMD} ${MVN_GPG_PARAM} ${MVN_TAG_CMD} ${MAVEN_OPTIONS} ${MVN_PROFILES} versions:display-dependency-updates versions:display-plugin-updates"
echo "[DEBUG] [${MVN_ALL}]"
mvn ${MVN_ALL}