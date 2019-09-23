package berlin.yuna.mavendeploy.archive;

public class MavenCommands {

    public static final String CMD_MVN_CLEAN = "generate-resources generate-sources dependency:resolve-plugins";
    public static final String CMD_MVN_REPORT = "versions:display-dependency-updates versions:display-plugin-updates";
    public static final String CMD_MVN_UPDATE_MAJOR = "versions:update-parent versions:update-properties versions:update-child-modules versions:use-latest-releases versions:use-latest-versions versions:use-next-snapshots versions:commit -DallowSnapshots=true -DallowMajorUpdates=true";
    public static final String CMD_MVN_UPDATE_MINOR = "versions:update-parent versions:update-properties versions:update-child-modules versions:use-latest-releases versions:use-latest-versions ersions:use-next-snapshots versions:commit -DallowSnapshots=true -DallowMajorUpdates=false";
    public static final String CMD_MVN_CLEAN_CACHE = "dependency:purge-local-repository";
    public static final String CMD_MVN_JAVADOC = "javadoc:jar -Dnodeprecatedlist -Dquiet=true";
    public static final String CMD_MVN_SOURCE_XX = "source:jar-no-fork -D--source=";
    public static final String CMD_MVN_TAG_XX = "scm:tag -Dtag=";
    public static final String CMD_MVN_SETTINGS_XX = "--settings=";
    public static final String XX_CMD_MVN_TAG_MSG = "-Dmessage=";
    public static final String CMD_MVN_SKIP_TEST = "-Dmaven.test.skip=true";
    public static final String CMD_MVN_VERSION_XX = "versions:set";
    public static final String XX_CMD_MVN_SNAPSHOT = "-DremoveSnapshot=true";
    public static final String XX_CMD_MVN_VERSION = "-DnewVersion=";
    public static final String CMD_MVN_GPG_SIGN_XX = "gpg:sign -Darguments=-D--pinentry-mode -Darguments=-Dloopback -Dgpg.passphrase=";
    public static final String CMD_MVN_GPG_SIGN_ALT_XX = "berlin.yuna:maven-gpg-plugin:sign -Darguments=-D--pinentry-mode -Darguments=-Dloopback -Dgpg.passphrase=";
    public static final String MVN_DEPLOY_LAYOUT = "default";
    public static final String NEXUS_DEPLOY_XX = "org.sonatype.plugins:nexus-staging-maven-plugin:1.6.8:";

    //TODO configurable
    public static final String CMD_MVN_SURFIRE_XX = "-Dsurefire.useSystemClassLoader=false -Dsurefire.excludesFile=";
    public static final String FILE_MVN_SURFIRE = "**/*IntegrationTest.java\n**/*ComponentTest.java\n**/*SmokeTest.java";
    public static final String CMD_MVN_FAILSAFE_XX = "failsafe:integration-test -Dfailsafe.includesFile=";
    public static final String FILE_MVN_FAILSAFE = "**/*IntegrationTest.java\n**/*ComponentTest.java\n**/*Test.java";
}