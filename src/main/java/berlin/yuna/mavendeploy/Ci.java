package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.CommandLineReader;
import berlin.yuna.clu.logic.Terminal;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static berlin.yuna.mavendeploy.GpgUtil.downloadMavenGpgIfNotExists;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_CLEAN;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_CLEAN_CACHE;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_FAILSAFE_XX;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_GPG_SIGN_XX;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_JAVADOC;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_REPORT;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_SKIP_TEST;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_SOURCE;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_SURFIRE_XX;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_TAG_XX;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_UPDATE;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_VERSION_XX;
import static berlin.yuna.mavendeploy.MavenCommands.FILE_MVN_FAILSAFE;
import static berlin.yuna.mavendeploy.MavenCommands.FILE_MVN_SURFIRE;
import static berlin.yuna.mavendeploy.MavenCommands.MVN_DEPLOY_LAYOUT;
import static berlin.yuna.mavendeploy.MavenCommands.SONATYPE_PLUGIN;
import static berlin.yuna.mavendeploy.MavenCommands.SONATYPE_STAGING_URL;
import static berlin.yuna.mavendeploy.MavenCommands.SONATYPE_URL;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

class Ci {

    private static final Pattern PATTERN_ORIGINAL_BRANCH_NAME = Pattern.compile(
            "(?<prefix>.*refs\\/.*\\/)(?<branchName>.*)(?<suffix>@.*)");

    public static void main(final String[] args) {
        new Ci(args).run();
    }

    private final Model pom;
    private File PROJECT_DIR = new File(System.getProperty("user.dir"));

    private String JAVA_VERSION = "1.8";
    private String ENCODING = "UTF-8";
    private String GPG_PASSPHRASE = null;
    private String PROJECT_VERSION = null;
    private String MVN_OPTIONS = "";

    private final boolean IS_POM;
    private boolean MVN_CLEAN = true;
    private boolean MVN_SKIP_TEST = false;
    private boolean MVN_UPDATE = true;
    private boolean MVN_JAVA_DOC = true;
    private boolean MVN_PROFILES = true;
    private boolean MVN_SOURCE = true;
    private boolean MVN_TAG = true;
    private boolean MVN_RELEASE = true;
    private boolean MVN_TAG_BREAK = false;

    private String MVN_DEPLOY_ID = null;
    private String SEMANTIC_FORMAT = "\\.::release::feature::bugfix|hotfix::custom";

    private static final Logger LOG = getLogger(Ci.class);

    public Ci(final String... args) {
        final CommandLineReader clr = new CommandLineReader(args);
        //Project
        PROJECT_DIR = getOrElse(clr.getValue("PROJECT_DIR"), PROJECT_DIR);
        ENCODING = getOrElse(clr.getValue("ENCODING"), ENCODING);
        PROJECT_VERSION = getOrElse(clr.getValue("PROJECT_VERSION"), PROJECT_VERSION);
        MVN_OPTIONS = getOrElse(clr.getValue("MVN_OPTIONS"), MVN_OPTIONS);
        JAVA_VERSION = getOrElse(clr.getValue("JAVA_VERSION"), JAVA_VERSION);
        SEMANTIC_FORMAT = getOrElse(clr.getValue("SEMANTIC_FORMAT"), SEMANTIC_FORMAT);

        //Boolean
        MVN_PROFILES = getOrElse(clr.getValue("MVN_PROFILES"), MVN_PROFILES);
        MVN_TAG_BREAK = getOrElse(clr.getValue("MVN_TAG_BREAK"), MVN_TAG_BREAK);
        MVN_CLEAN = getOrElse(clr.getValue("MVN_CLEAN"), MVN_CLEAN);
        MVN_UPDATE = getOrElse(clr.getValue("MVN_UPDATE"), MVN_UPDATE);
        MVN_JAVA_DOC = getOrElse(clr.getValue("MVN_JAVA_DOC"), MVN_JAVA_DOC);
        MVN_SOURCE = getOrElse(clr.getValue("MVN_SOURCE"), MVN_SOURCE);
        MVN_TAG = getOrElse(clr.getValue("MVN_TAG"), MVN_TAG);
        MVN_RELEASE = getOrElse(clr.getValue("MVN_RELEASE"), MVN_RELEASE);
        MVN_SKIP_TEST = getOrElse(clr.getValue("MVN_SKIP_TEST"), MVN_SKIP_TEST);

        //DEOPLY (Nexus only currently)
        MVN_DEPLOY_ID = getOrElse(clr.getValue("MVN_DEPLOY_ID"), MVN_DEPLOY_ID);

        //GPG
        GPG_PASSPHRASE = getOrElse(clr.getValue("GPG_PASSPHRASE"), GPG_PASSPHRASE);
        IS_POM = isPomArtifact();
        pom = parsePomFile(PROJECT_DIR);

        PROJECT_VERSION = isEmpty(SEMANTIC_FORMAT) ?
                PROJECT_VERSION :
                new SemanticService(SEMANTIC_FORMAT).getNextSemanticVersion(pom.getVersion(), PROJECT_VERSION);
    }

    public void run() {
        //TODO: read pom file
        //TODO: release on git changes (git status)
        if (!isEmpty(GPG_PASSPHRASE)) {
            downloadMavenGpgIfNotExists(PROJECT_DIR);
        }
        System.out.println(prepareMavenCommand());
    }

    protected String prepareMavenCommand() {
        final StringBuilder mvnCommand = new StringBuilder();
        mvnCommand.append("mvn").append(" ");
        mvnCommand.append(ifDo(MVN_CLEAN, CMD_MVN_CLEAN_CACHE));
        mvnCommand.append("clean").append(" ");
        mvnCommand.append(isEmpty(MVN_DEPLOY_ID) ? "verify" : "deploy").append(" ");
        mvnCommand.append(ifDo(MVN_SKIP_TEST, CMD_MVN_SKIP_TEST));
        mvnCommand.append(ifDo(true, CMD_MVN_CLEAN));
        mvnCommand.append(ifDo(MVN_UPDATE, CMD_MVN_UPDATE));
        mvnCommand.append(ifDo(PROJECT_VERSION, CMD_MVN_VERSION_XX));
        mvnCommand.append(ifDo(!IS_POM && MVN_JAVA_DOC, CMD_MVN_JAVADOC));
        mvnCommand.append(ifDo(!IS_POM && MVN_SOURCE, CMD_MVN_SOURCE));
        mvnCommand.append(ifDo(hasNewTag(), CMD_MVN_TAG_XX + PROJECT_VERSION));
        mvnCommand.append(ifDo(GPG_PASSPHRASE, CMD_MVN_GPG_SIGN_XX + GPG_PASSPHRASE));
        mvnCommand.append(ifDo(MVN_DEPLOY_ID, prepareNexusDeployUrl()));
        mvnCommand.append(generateMavenOptions(MVN_OPTIONS, ENCODING, JAVA_VERSION)).append(" ");
        mvnCommand.append(ifDo(MVN_PROFILES, prepareMavenProfileParam()));
        mvnCommand.append(prepareSurFire()).append(" ");
        mvnCommand.append(prepareFailSafe()).append(" ");
        mvnCommand.append(CMD_MVN_REPORT).append(" ");
        return mvnCommand.toString();
    }

    private Model parsePomFile(final File projectDir) {
        try {
            return new MavenXpp3Reader().read(new FileReader(new File(projectDir, "pom.xml")));
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("could not read pom.xml \n ", e);
        }
    }

    private String prepareSurFire() {
        try {
            final File failSafeConf = File.createTempFile("mvnSurFireExcludes_", ".conf");
            Files.write(failSafeConf.toPath(), FILE_MVN_SURFIRE.getBytes());
            return CMD_MVN_SURFIRE_XX + failSafeConf.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String prepareFailSafe() {
        try {
            final File failSafeConf = File.createTempFile("mvnFailSafeIncludes_", ".conf");
            Files.write(failSafeConf.toPath(), FILE_MVN_FAILSAFE.getBytes());
            return CMD_MVN_FAILSAFE_XX + failSafeConf.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String prepareNexusDeployUrl() {
        return SONATYPE_PLUGIN + " -DaltDeploymentRepository=" + MVN_DEPLOY_ID + "::" + MVN_DEPLOY_LAYOUT + "::" + SONATYPE_STAGING_URL + " -DnexusUrl=" + SONATYPE_URL + " -DserverId=" + MVN_DEPLOY_ID + " -DautoReleaseAfterClose=false";
    }

    private String generateMavenOptions(final String previousMavenOptions, final String encoding, final String javaVersion) {
        return previousMavenOptions
                + " -Dproject.build.sourceEncoding=" + encoding
                + " -Dproject.encoding=" + encoding
                + " -Dproject.reporting.outputEncoding=" + encoding
                + " -Dmaven.compiler.source=" + javaVersion
                + " -Dmaven.compiler.target=" + javaVersion;
    }

    private boolean hasNewTag() {
        if (MVN_TAG && !isEmpty(PROJECT_VERSION)) {
            final String lastGitTag = lastGitTag();
            printTagMessage(lastGitTag);
            return !PROJECT_VERSION.equalsIgnoreCase(lastGitTag);
        }
        return false;
    }

    private void printTagMessage(final String lastGitTag) {
        if (MVN_TAG_BREAK && PROJECT_VERSION.equalsIgnoreCase(lastGitTag)) {
            throw new RuntimeException(format("GIT_TAG [%s] already exists", PROJECT_VERSION));
        } else {
            LOG.info("New GIT_TAG [{}]", PROJECT_VERSION);
        }
    }

    private String lastGitTag() {
        return newTerminal().execute("git describe --tags --always  | sed 's/\\(.*\\)-.*/\\1/'").consoleInfo().trim();
    }

    private boolean isEmpty(final String test) {
        return test == null || test.trim().isEmpty();
    }

    private String prepareMavenProfileParam() {
        LOG.info("Read maven profiles");
        //TODO: read pom file
        final String command = "mvn help:all-profiles | grep \"Profile Id\" | cut -d' ' -f 5 | xargs | tr ' ' ','";
        final String mvnProfiles = newTerminal().timeoutMs(-1).execute(command).consoleInfo();
        LOG.info("Found maven profiles [{}]", mvnProfiles.trim());
        return isEmpty(mvnProfiles) ? "" : "--activate-profiles=" + mvnProfiles.trim();
    }

    private Boolean isPomArtifact() {
        return newTerminal().execute("grep '<packaging>pom</packaging>' pom.xml | wc -l").consoleInfo().trim().equals(
                "1");
    }

    private String ifDo(final String trigger, final String arg) {
        return ifDo(!isEmpty(trigger), arg);
    }

    private String ifDo(final boolean trigger, final String arg) {
        return trigger ? arg + " " : "";
    }

    private String getOrElse(final String test, final String fallback) {
        return !isEmpty(test) ? test : fallback;
    }

    private File getOrElse(final String test, final File fallback) {
        final File file = !isEmpty(test) ? new File(test) : fallback;
        if (!file.exists()) {
            throw new RuntimeException(format("Path [%s] does not exist", file));
        }
        return file;
    }

    private boolean getOrElse(final String test, final boolean fallback) {
        return !isEmpty(test) ? Boolean.valueOf(test) : fallback;
    }

    private Terminal newTerminal() {
        return new Terminal()
                .breakOnError(true)
                .consumerError(System.err::println)
                .dir(PROJECT_DIR).timeoutMs(32000);
    }

}
