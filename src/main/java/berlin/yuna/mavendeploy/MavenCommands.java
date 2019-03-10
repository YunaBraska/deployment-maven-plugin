package berlin.yuna.mavendeploy;

public class MavenCommands {

    static final String CMD_MVN_UPDATE = "versions:update-parent versions:update-properties versions:update-child-modules versions:use-latest-releases versions:use-next-snapshots versions:commit -DallowSnapshots=true -DgenerateBackupPoms=false";
    static final String CMD_MVN_CLEAN_CACHE = "dependency:purge-local-repository";
    static final String CMD_MVN_JAVADOC = "javadoc:jar -Dnodeprecatedlist -Dquiet=true";
    static final String CMD_MVN_SOURCE = "source:jar-no-fork";
    static final String CMD_MVN_TAG_XX = "scm:tag -Dtag=";
}
