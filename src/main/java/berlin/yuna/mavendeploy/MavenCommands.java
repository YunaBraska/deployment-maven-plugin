package berlin.yuna.mavendeploy;

public class MavenCommands {

    static final String CMD_MVN_CLEAN = "generate-resources generate-sources dependency:resolve-plugins";
    static final String CMD_MVN_REPORT = "gversions:display-dependency-updates versions:display-plugin-updates";
    static final String CMD_MVN_UPDATE = "versions:update-parent versions:update-properties versions:update-child-modules versions:use-latest-releases versions:use-next-snapshots versions:commit -DallowSnapshots=true -DgenerateBackupPoms=false";
    static final String CMD_MVN_CLEAN_CACHE = "dependency:purge-local-repository";
    static final String CMD_MVN_JAVADOC = "javadoc:jar -Dnodeprecatedlist -Dquiet=true";
    static final String CMD_MVN_SOURCE = "source:jar-no-fork";
    static final String CMD_MVN_TAG_XX = "scm:tag -Dtag=";
    static final String CMD_MVN_GPG_SIGN_XX = "berlin.yuna:maven-gpg-plugin:sign -Darguments=-D--pinentry-mode -Darguments=loopback -Dgpg.passphrase=";
    static String MVN_DEPLOY_LAYOUT = "default";
    static String SONATYPE_URL = "https://oss.sonatype.org";
    static String SONATYPE_PLUGIN = "org.sonatype.plugins:nexus-staging-maven-plugin:1.6.8:deploy";
    static String SONATYPE_STAGING_URL = "https://oss.sonatype.org/service/local/staging/deploy/maven2/";
}
