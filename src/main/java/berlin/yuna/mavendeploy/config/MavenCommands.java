package berlin.yuna.mavendeploy.config;

public class MavenCommands {

    public static final String CMD_MVN_CLEAN = "generate-resources generate-sources dependency:resolve-plugins";
    public static final String CMD_MVN_REPORT = "versions:display-dependency-updates versions:display-plugin-updates";
    public static final String CMD_MVN_UPDATE = "versions:update-parent versions:update-properties versions:update-child-modules versions:use-latest-releases versions:use-next-snapshots versions:commit -DallowSnapshots=true -DgenerateBackupPoms=false";
    public static final String CMD_MVN_CLEAN_CACHE = "dependency:purge-local-repository";
    public static final String CMD_MVN_JAVADOC = "javadoc:jar -Dnodeprecatedlist -Dquiet=true";
    public static final String CMD_MVN_SOURCE = "source:jar-no-fork";
    public static final String CMD_MVN_TAG_XX = "scm:tag -Dtag=";
    public static final String CMD_MVN_SKIP_TEST = "-Dmaven.test.skip=true";
    public static final String CMD_MVN_VERSION_XX = "versions:set -DnewVersion=";
    public static final String CMD_MVN_GPG_SIGN_XX = "gpg:sign -Darguments=-D--pinentry-mode -Darguments=-Dloopback -Dgpg.passphrase=";
    public static final String CMD_MVN_GPG_SIGN_ALT_XX = "berlin.yuna:maven-gpg-plugin:sign -Darguments=-D--pinentry-mode -Darguments=-Dloopback -Dgpg.passphrase=";
    public static String MVN_DEPLOY_LAYOUT = "default";

    //TODO configurable
    public static final String CMD_MVN_SURFIRE_XX = "-Dsurefire.useSystemClassLoader=false -Dsurefire.excludesFile=";
    public static final String FILE_MVN_SURFIRE = "**/*IntegrationTest.java\n**/*ComponentTest.java\n**/*SmokeTest.java";
    public static final String CMD_MVN_FAILSAFE_XX = "failsafe:integration-test -Dfailsafe.includesFile=";
    public static final String FILE_MVN_FAILSAFE = "**/*IntegrationTest.java\n**/*ComponentTest.java\n**/*Test.java";
    public static String SONATYPE_URL = "https://oss.sonatype.org";
    public static String SONATYPE_PLUGIN = "org.sonatype.plugins:nexus-staging-maven-plugin:1.6.8:deploy";
    public static String SONATYPE_STAGING_URL = "https://oss.sonatype.org/service/local/staging/deploy/maven2/";
}