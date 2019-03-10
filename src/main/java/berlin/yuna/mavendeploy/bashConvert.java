package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.CommandLineReader;
import berlin.yuna.clu.logic.Terminal;
import org.slf4j.Logger;

import java.io.File;

import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_CLEAN_CACHE;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_JAVADOC;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_SOURCE;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_TAG_XX;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_UPDATE;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

public class bashConvert {

    public static void main(final String[] args) {
        new bashConvert(args).run();
    }

    final CommandLineReader clr;

    private File PROJECT_DIR = new File(System.getProperty("user.dir"));
    private String JAVA_VERSION = "1.8";
    private String ENCODING = "UTF-8";
    private String GPG_PASSPHRASE = null;
    private String PROJECT_VERSION = null;
    private String MVN_OPTIONS = "";

    private boolean IS_POM = false;
    private boolean MVN_CLEAN = true;
    private boolean MVN_UPDATE = true;
    private boolean MVN_JAVA_DOC = true;
    private boolean MVN_PROFILES = true;
    private boolean MVN_SOURCE = true;
    private boolean MVN_TAG = true;
    private boolean MVN_RELEASE = true;
    private boolean GIT_TAG = true;

    private String MVN_DEPLOY_ID = null;
    private String MVN_RELEASE_PARAM = null;
    private String MVN_DEPLOY_LAYOUT = "default";
    private String MVN_NEXUS_URL = "https://oss.sonatype.org";
    private String SONATYPE_PLUGIN = "org.sonatype.plugins:nexus-staging-maven-plugin:1.6.8:deploy";
    private String SONATYPE_STAGING_URL = "https://oss.sonatype.org/service/local/staging/deploy/maven2/";
    private String SONATYPE_DEPLOY_CMD = null;

    private String MVN_GOAL_CMD = 'clean'
    private String MVN_CLEAN_PARAM = 'generate-resources generate-sources dependency:resolve-plugins'
    private String MVN_UPDATE_CMD = "versions:update-parent versions:update-properties versions:update-child-modules versions:use-latest-releases versions:use-next-snapshots versions:commit -DallowSnapshots=true -DgenerateBackupPoms=false"
    private String MVN_DOC_CMD = "javadoc:jar"
    private String MVN_DOC_PARAM = "-Dnodeprecatedlist -Dquiet=true"
    private String MVN_SOURCE_CMD = "source:jar-no-fork"
    private String MVN_REPORT_CMD = "versions:display-dependency-updates versions:display-plugin-updates"
    //    MVN_GPG_CMD="gpg:sign"
    private String MVN_GPG_CMD = "berlin.yuna:maven-gpg-plugin:sign"
    private String MVN_GPG_PARAM = "-Darguments=-D--pinentry-mode -Darguments=loopback"
    private String MVN_TAG_CMD = "scm:tag -Dtag=";

    private static final Logger LOG = getLogger(bashConvert.class);

    public bashConvert(final String... args) {
        clr = new CommandLineReader(args);
        //Project
        PROJECT_DIR = getOrElse(clr.getValue("PROJECT_DIR"), PROJECT_DIR);
        JAVA_VERSION = getOrElse(clr.getValue("JAVA_VERSION"), JAVA_VERSION);
        ENCODING = getOrElse(clr.getValue("ENCODING"), ENCODING);
        PROJECT_VERSION = getOrElse(clr.getValue("PROJECT_VERSION"), PROJECT_VERSION);
        MVN_OPTIONS = getOrElse(clr.getValue("MVN_OPTIONS"), MVN_OPTIONS);

        //Boolean
        MVN_PROFILES = getOrElse(clr.getValue("MVN_PROFILES"), MVN_PROFILES);
        GIT_TAG = getOrElse(clr.getValue("GIT_TAG"), GIT_TAG);
        MVN_CLEAN = getOrElse(clr.getValue("MVN_CLEAN"), MVN_CLEAN);
        MVN_UPDATE = getOrElse(clr.getValue("MVN_UPDATE"), MVN_UPDATE);
        MVN_JAVA_DOC = getOrElse(clr.getValue("MVN_JAVA_DOC"), MVN_JAVA_DOC);
        MVN_SOURCE = getOrElse(clr.getValue("MVN_SOURCE"), MVN_SOURCE);
        MVN_TAG = getOrElse(clr.getValue("MVN_TAG"), MVN_TAG);
        MVN_RELEASE = getOrElse(clr.getValue("MVN_RELEASE"), MVN_RELEASE);

        //GPG
        GPG_PASSPHRASE = getOrElse(clr.getValue("GPG_PASSPHRASE"), GPG_PASSPHRASE);
        IS_POM = isParseJarArtifact();
    }

    public void run() {
        //TODO: read pom file
        //TODO: release on git changes (git status)

        LOG.info("PROJECT_DIR [{}]", PROJECT_DIR.toString());


//        MVN_ALL=" ${MVN_TAG_CMD} ${MVN_OPTIONS} ${MVN_GPG_CMD} ${MVN_GPG_PARAM} ${MVN_PROFILES} ${SONATYPE_DEPLOY_CMD} ${MVN_REPORT_CMD}"
        final StringBuilder mvnConsole = new StringBuilder();
        mvnConsole.append("mvn");
        mvnConsole.append(MVN_CLEAN ? CMD_MVN_CLEAN_CACHE : "");
        mvnConsole.append(isEmpty(MVN_DEPLOY_ID) ? "clean" : "deploy");
        mvnConsole.append(!IS_POM && MVN_UPDATE ? CMD_MVN_UPDATE : "");
        mvnConsole.append(!IS_POM && MVN_JAVA_DOC ? CMD_MVN_JAVADOC : "");
        mvnConsole.append(MVN_SOURCE ? CMD_MVN_SOURCE : "");
        mvnConsole.append(hasNewTag() ? CMD_MVN_TAG_XX + PROJECT_VERSION : "");
        mvnConsole.append(partMvnProfiles());
        newTerminal().execute(mvnConsole.toString());
    }

    private boolean hasNewTag() {
        if (GIT_TAG && !isEmpty(PROJECT_VERSION)) {
            final String lastGitTag = lastGitTag();
            final boolean newTag = !PROJECT_VERSION.equalsIgnoreCase(lastGitTag);
            LOG.info(newTag? "New GIT_TAG [{}]" : "GIT_TAG [{}] already exists", GIT_TAG);
            return newTag;
        }
        return false;
    }

    private String lastGitTag() {
        return newTerminal().execute("$(git describe --always)").consoleInfo().trim();
    }

    private boolean isEmpty(final String test) {
        return test == null || test.trim().isEmpty();
    }

    private String partMvnProfiles() {
        if (MVN_PROFILES) {
            LOG.info("Read maven profiles");
            //TODO: read pom file
            final String command = "$(mvn help:all - profiles | grep \"Profile Id\" | cut - d ' ' - f 5 | xargs | tr ' ' ',')";
            LOG.debug(command);
            LOG.info("Found maven profiles [{}]", command);
            return "-p" + newTerminal().clearConsole().execute(command).consumerInfo();
        }
        return "";
    }

    private Boolean isParseJarArtifact() {
        return Boolean.valueOf(newTerminal().execute("if [[ $(grep '<packaging>pom</packaging>' pom.xml | wc -l) = *1* ]] ; then IS_POM='true'; fi").consoleInfo());
    }

    private String getOrElse(final String test, final String fallback) {
        return test != null ? test : fallback;
    }

    private File getOrElse(final String test, final File fallback) {
        final File file = test != null ? new File(test) : fallback;
        if (!file.exists()) {
            throw new RuntimeException(format("Path [%s] does not exist", file));
        }
        return file;
    }

    private boolean getOrElse(final String test, final boolean fallback) {
        return test != null ? Boolean.valueOf(test) : fallback;
    }

    private Terminal newTerminal() {
        return new Terminal()
                .breakOnError(true)
                .consumerInfo(System.out::println)
                .consumerError(System.err::println)
                .dir(PROJECT_DIR).timeoutMs(10000);
    }
}
