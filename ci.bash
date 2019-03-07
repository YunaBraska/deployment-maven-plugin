#! /bin/bash
set -e

JAVA_VERSION='1.8'
ENCODING='UTF-8'
MVN_PROFILES="true"
GPG_PASSPHRASE=''
PROJECT_VERSION=''
IS_POM='false'

MVN_CLEAN_CMD='clean verify'
MVN_CLEAN_PARAM='generate-resources generate-sources dependency:resolve-plugins'

MVN_CACHE_CMD='dependency:purge-local-repository'

for i in "$@"
do
case $i in
    -V=*|--PROJECT_VERSION=*)
    PROJECT_VERSION="${i#*=}"
    shift # past argument=value
    ;;
    -P=*|--MVN_PROFILES=*)
    MVN_PROFILES="${i#*=}"
    shift # past argument=value
    ;;
    -G=*|--GPG_PASSPHRASE=*)
    GPG_PASSPHRASE="${i#*=}"
    shift # past argument=value
    ;;
    -E=*|--ENCODING=*)
    ENCODING="${i#*=}"
    shift # past argument=value
    ;;
    -J=*|--JAVA_VERSION=*)
    JAVA_VERSION="${i#*=}"
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

#if [ ${MVN_PROFILES} = "true" ]; then
#fi

echo "########## SETUP ENVIRONMENT ##########"


echo "MVN_PROFILES [${MVN_PROFILES}]"
echo "GPG_PASSPHRASE [${GPG_PASSPHRASE}]"
echo "PROJECT_VERSION [${PROJECT_VERSION}]"
echo "JAVA_VERSION [${JAVA_VERSION}]"
echo "ENCODING [${ENCODING}]"

exit


#TODO if no ${PROFILES}

MVN_CLEAN_CMD="clean verify"
MVN_CLEAN_PARAM="generate-resources generate-sources dependency:resolve-plugins" # -P ${PROFILES}

#if purge local mvn cache
MVN_CHACHE_CMD="dependency:purge-local-repository"

#if set version
MVN_VER_PARAM="-DnewVersion=${NEW_VERSION}"

#if update dependencies default == true
MVN_UPDATE_CMD="versions:set"
MVN_UPDATE_PARAM="${MVN_VER_PARAM} versions:update-parent versions:update-properties versions:update-child-modules versions:use-latest-releases versions:use-next-snapshots versions:commit -DallowSnapshots=true -DgenerateBackupPoms=false"

#if source default == true
MVN_DOC_CMD="javadoc:jar"
MVN_DOC_PARAM="-Dnodeprecatedlist -Dquiet=true"

MVN_SRC_CMD="source:jar-no-fork"
MVN_SRC_PARAM=""

#if sign default == true
export GPG_TTY=$(tty)
gpg -k --armor --always-trust --passphrase=${GPG_PASSPHRASE} --pinentry-mode loopback
#echo "GPG ACTIVATION" > gpg.act; gpg --sign --armor --always-trust --passphrase=${GPG_PASSPHRASE} --pinentry-mode loopback gpg.act; rm gpg.act gpg.act.asc
MVN_GPG_CMD="gpg:sign"
MVN_GPG_PARAM="-Dgpg.passphrase=${GPG_PASSPHRASE} -Darguments=-D--pinentry-mode -Darguments=loopback"

#if print update needs
MVN_PTU_CMD="versions:display-dependency-updates versions:display-plugins-updates"


echo "########## PARAMETERS ##########"
echo "Encoding [${ENDCODING}]"
echo "Encoding [${JAVA_VERSION}]"
if [[ $(grep '<packaging>pom</packaging>' pom.xml | wc -l) = *1* ]] ; then IS_POM=true; fi
echo "Pom Artifact [${IS_POM}]"
OLD_VERSION=$(mvn help:evaluate -Dexpression=project.version -Dexpression=project.version | grep -v '\[')
echo "Version [${OLD_VERSION}] -> [${NEW_VERSION}]"



echo "Profiles [${PROFILES}]"

mvn ${MVN_CHACHE_CMD} ${MVN_CLEAN_CMD} ${MVN_CLEAN_PARAM} ${MVN_UPDATE_CMD} ${MVN_UPDATE_PARAM} ${MVN_DOC_CMD} ${MVN_DOC_PARAM} ${MVN_SRC_CMD} ${MVN_SRC_PARAM} ${MVN_GPG_CMD} ${MVN_GPG_PARAM}
exit 0

MAVEN_OPTIONS="-Dproject.build.sourceEncoding=${ENDCODING} -Dproject.encoding=${ENDCODING} -Dproject.reporting.outputEncoding=${ENDCODING} -Dmaven.compiler.source=${JAVA_VERSION} -Dmaven.compiler.target=${JAVA_VERSION} --quiet"
echo ${MAVEN_OPTIONS}
echo "########## CLEAN REPOSITORY ##########"
mvn dependency:purge-local-repository clean generate-resources generate-sources dependency:sources dependency:resolve-plugins -P ${PROFILES} ${MAVEN_OPTIONS}
#mvn dependency:purge-local-repository clean generate-resources generate-sources dependency:sources dependency:resolve-plugins -P ${PROFILES}
echo "########## UPDATING PROJECT VERSIONS ##########"
mvn versions:set -DnewVersion=${NEW_VERSION} versions:update-parent versions:update-properties versions:update-child-modules versions:use-latest-releases versions:use-next-snapshots versions:commit -DallowSnapshots=true -DgenerateBackupPoms=false ${MAVEN_OPTIONS}
echo "########## BUILD ##########"
mvn clean verify -DskipTests=true ${MAVEN_OPTIONS}
if [[ $IS_POM = "false" ]]; then
    echo "########## GENERATE JAVADOC ##########"
	#https://maven.apache.org/plugins/maven-javadoc-plugin/
	mvn javadoc:jar -Dnodeprecatedlist -Dquiet=true ${MAVEN_OPTIONS}
	echo "########## GENERATE SOURCES ##########"
	#https://maven.apache.org/plugins/maven-source-plugin/
	mvn source:jar-no-fork ${MAVEN_OPTIONS}
	echo "########## SIGN ARTIFACTS ##########"
	export GPG_TTY=$(tty)
	#for x in target/*.jar; do gpg --sign --armor --always-trust --passphrase=7576Simba --pinentry-mode loopback "${x}"; done
	mvn package gpg:sign -DskipTests=true -Dgpg.passphrase=${GPG_PASSPHRASE} -Darguments=-D--pinentry-mode -Darguments=loopback
	for x in target/*.asc; do gpg --verify "${x}"; done
	#http://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html
fi
echo "########## TODO: SCM DEPLOY TAG \${project.version} <inherited>false</inherited> ##########"
echo "########## TODO: org.sonatype.plugins ##########"
echo "########## TODO: RELEASE <autoVersionSubmodules>true</autoVersionSubmodules> <useReleaseProfile>true</useReleaseProfile> ##########"
#TODO: mvn versions:display-plugin-updates

#TODO OWN BUILD POM - backup normal one, add steps from build pom and switch back to the old pom