package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.CommandLineReader;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.logic.GitService;
import berlin.yuna.mavendeploy.logic.GpgUtil;
import berlin.yuna.mavendeploy.logic.SemanticService;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_CLEAN;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_CLEAN_CACHE;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_FAILSAFE_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_GPG_SIGN_ALT_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_GPG_SIGN_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_JAVADOC;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_REPORT;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_SKIP_TEST;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_SOURCE;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_SURFIRE_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_TAG_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_UPDATE;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_VERSION_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.FILE_MVN_FAILSAFE;
import static berlin.yuna.mavendeploy.config.MavenCommands.FILE_MVN_SURFIRE;
import static berlin.yuna.mavendeploy.config.MavenCommands.MVN_DEPLOY_LAYOUT;
import static berlin.yuna.mavendeploy.config.MavenCommands.SONATYPE_PLUGIN;
import static berlin.yuna.mavendeploy.config.MavenCommands.SONATYPE_STAGING_URL;
import static berlin.yuna.mavendeploy.config.MavenCommands.SONATYPE_URL;
import static java.lang.String.format;

public class Ci {

    private final Model pom;
    private File PROJECT_DIR = new File(System.getProperty("user.dir"));

    private String JAVA_VERSION = null;
    private String ENCODING = null;
    private String GPG_PASSPHRASE = null;
    private String GPG_PASSPHRASE_ALT = null;
    private String PROJECT_VERSION = null;
    private String MVN_OPTIONS = "";

    private final boolean IS_POM;
    private boolean MVN_CLEAN = true;
    private boolean MVN_CLEAN_CACHE = false;
    private boolean MVN_SKIP_TEST = false;
    private boolean MVN_UPDATE = false;
    private boolean MVN_JAVA_DOC = true;
    private boolean MVN_PROFILES = true;
    private boolean MVN_SOURCE = true;
    private boolean MVN_TAG = false;
    private boolean MVN_TAG_BREAK = false;
    private boolean MVN_REPORT = false;
    private boolean MVN_RELEASE = true;

    private String MVN_DEPLOY_ID = null;
    private String SEMANTIC_FORMAT = null;
//    private String SEMANTIC_FORMAT = "\\.::release::feature::bugfix|hotfix::custom";

    private final Log LOG;

    public Ci(final Log LOG, final String... args) {
        this.LOG = LOG;
        final CommandLineReader clr = new CommandLineReader(args == null ? new String[]{""} : args);
        //Project
        PROJECT_DIR = getOrElse(clr.getValue("PROJECT_DIR"), PROJECT_DIR);
        ENCODING = getOrElse(clr.getValue("ENCODING"), ENCODING);
        PROJECT_VERSION = getOrElse(clr.getValue("PROJECT_VERSION"), PROJECT_VERSION);
        MVN_OPTIONS = getOrElse(clr.getValue("MVN_OPTIONS"), MVN_OPTIONS);
        JAVA_VERSION = getOrElse(clr.getValue("JAVA_VERSION"), JAVA_VERSION);
        SEMANTIC_FORMAT = getOrElse(clr.getValue("SEMANTIC_FORMAT"), SEMANTIC_FORMAT);

        //Boolean
        MVN_PROFILES = getOrElse(clr.getValue("MVN_PROFILES"), MVN_PROFILES);
        MVN_CLEAN = getOrElse(clr.getValue("MVN_CLEAN"), MVN_CLEAN);
        MVN_CLEAN_CACHE = getOrElse(clr.getValue("MVN_CLEAN_CACHE"), MVN_CLEAN_CACHE);
        MVN_UPDATE = getOrElse(clr.getValue("MVN_UPDATE"), MVN_UPDATE);
        MVN_JAVA_DOC = getOrElse(clr.getValue("MVN_JAVA_DOC"), MVN_JAVA_DOC);
        MVN_SOURCE = getOrElse(clr.getValue("MVN_SOURCE"), MVN_SOURCE);
        MVN_TAG = getOrElse(clr.getValue("MVN_TAG"), MVN_TAG);
        MVN_TAG_BREAK = getOrElse(clr.getValue("MVN_TAG_BREAK"), MVN_TAG_BREAK);
        MVN_RELEASE = getOrElse(clr.getValue("MVN_RELEASE"), MVN_RELEASE);
        MVN_SKIP_TEST = getOrElse(clr.getValue("MVN_SKIP_TEST"), MVN_SKIP_TEST);
        MVN_REPORT = getOrElse(clr.getValue("MVN_REPORT"), MVN_REPORT);

        //DEOPLY (Nexus only currently)
        MVN_DEPLOY_ID = getOrElse(clr.getValue("MVN_DEPLOY_ID"), MVN_DEPLOY_ID);

        //GPG
        GPG_PASSPHRASE = getOrElse(clr.getValue("GPG_PASSPHRASE"), GPG_PASSPHRASE);
        GPG_PASSPHRASE_ALT = getOrElse(clr.getValue("GPG_PASSPHRASE_ALT"), GPG_PASSPHRASE_ALT);
        pom = parsePomFile(PROJECT_DIR);
        IS_POM = isPomArtifact(pom);

        PROJECT_VERSION = isEmpty(SEMANTIC_FORMAT) ?
                PROJECT_VERSION :
                new SemanticService(SEMANTIC_FORMAT, PROJECT_DIR).getNextSemanticVersion(pom.getVersion(),
                                                                                         PROJECT_VERSION);
    }

    protected String prepareMaven() {
        final StringBuilder mvnCommand = new StringBuilder();
        mvnCommand.append("mvn").append(" ");
        mvnCommand.append(ifDo(MVN_CLEAN_CACHE, CMD_MVN_CLEAN_CACHE, "MVN_CLEAN_CACHE"));
        mvnCommand.append(ifDo(MVN_CLEAN, "clean", "MVN_CLEAN"));
        mvnCommand.append(isEmpty(MVN_DEPLOY_ID) ? "verify" : "deploy").append(" ");
        mvnCommand.append(ifDo(MVN_SKIP_TEST, CMD_MVN_SKIP_TEST, "MVN_SKIP_TEST"));
        mvnCommand.append(ifDo(MVN_CLEAN, CMD_MVN_CLEAN));
        mvnCommand.append(ifDo(MVN_UPDATE, CMD_MVN_UPDATE, "MVN_UPDATE"));
        mvnCommand.append(ifDo(PROJECT_VERSION, CMD_MVN_VERSION_XX + PROJECT_VERSION, "PROJECT_VERSION"));
        mvnCommand.append(ifDo(!IS_POM && MVN_JAVA_DOC, CMD_MVN_JAVADOC, "MVN_JAVA_DOC"));
        mvnCommand.append(ifDo(!IS_POM && MVN_SOURCE, CMD_MVN_SOURCE, "MVN_SOURCE"));
        mvnCommand.append(ifDo(hasNewTag(), CMD_MVN_TAG_XX + PROJECT_VERSION, "MVN_TAG"));
        mvnCommand.append(ifDo(GPG_PASSPHRASE, CMD_MVN_GPG_SIGN_XX + GPG_PASSPHRASE, "GPG_PASSPHRASE"));
        mvnCommand.append(ifDo(GPG_PASSPHRASE_ALT, CMD_MVN_GPG_SIGN_ALT_XX + GPG_PASSPHRASE_ALT, "GPG_PASSPHRASE_ALT"));
        mvnCommand.append(ifDo(MVN_DEPLOY_ID, prepareNexusDeployUrl(), "MVN_DEPLOY_ID"));
        mvnCommand.append(ifDo(MVN_OPTIONS, MVN_OPTIONS, "MVN_OPTIONS"));
        mvnCommand.append(ifDo(ENCODING, "-Dproject.build.sourceEncoding=" + ENCODING, "ENCODING"));
        mvnCommand.append(ifDo(ENCODING, "-Dproject.reporting.outputEncoding=" + ENCODING));
        mvnCommand.append(ifDo(ENCODING, "-Dproject.encoding=" + ENCODING));
        mvnCommand.append(ifDo(JAVA_VERSION, "-Dmaven.compiler.source=" + JAVA_VERSION, "JAVA_VERSION"));
        mvnCommand.append(ifDo(JAVA_VERSION, "-Dmaven.compiler.target=" + JAVA_VERSION));
        mvnCommand.append(ifDo(MVN_PROFILES, prepareMavenProfileParam(), "MVN_PROFILES"));
        mvnCommand.append(ifDo(!MVN_SKIP_TEST, prepareSurFire(), "MVN_SKIP_TEST"));
        mvnCommand.append(ifDo(!MVN_SKIP_TEST, prepareFailSafe()));
        mvnCommand.append(ifDo(MVN_REPORT, CMD_MVN_REPORT, "MVN_REPORT"));

        if (!isEmpty(GPG_PASSPHRASE_ALT)) {
            new GpgUtil(LOG).downloadMavenGpgIfNotExists(PROJECT_DIR);
        }
        return mvnCommand.toString().trim();
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

    private boolean hasNewTag() {
        if ((MVN_TAG || MVN_TAG_BREAK) && !isEmpty(PROJECT_VERSION)) {
            final String lastGitTag = new GitService(PROJECT_DIR).getLastGitTag();
            printTagMessage(lastGitTag);
            return !PROJECT_VERSION.equalsIgnoreCase(lastGitTag);
        }
        return false;
    }

    private void printTagMessage(final String lastGitTag) {
        if (MVN_TAG_BREAK && PROJECT_VERSION.equalsIgnoreCase(lastGitTag)) {
            throw new RuntimeException(format("GIT_TAG [%s] already exists", PROJECT_VERSION));
        } else {
            LOG.info(format("New GIT_TAG [%s]", PROJECT_VERSION));
        }
    }

    private boolean isEmpty(final String test) {
        return test == null || test.trim().isEmpty();
    }

    private String prepareMavenProfileParam() {
        LOG.debug("Read maven profiles");
        final String command = "mvn help:all-profiles | grep \"Profile Id\" | cut -d' ' -f 5 | xargs | tr ' ' ','";
        final String mvnProfiles = newTerminal().timeoutMs(-1).execute(command).consoleInfo();
        LOG.info(format("Found maven profiles [%s]", mvnProfiles.trim()));
        return isEmpty(mvnProfiles) ? "" : "--activate-profiles=" + mvnProfiles.trim();
    }

    private Boolean isPomArtifact(final Model pom) {
        final String packaging = pom.getPackaging();
        return isEmpty(packaging) || packaging.trim().equals("pom");
    }

    private String ifDo(final String trigger, final String arg) {
        return ifDo(!isEmpty(trigger), arg);
    }

    private String ifDo(final boolean trigger, final String arg) {
        return trigger ? arg + " " : "";
    }

    private String ifDo(final String trigger, final String arg, final String description) {
        return ifDo(!isEmpty(trigger), arg, description);
    }

    private String ifDo(final boolean trigger, final String arg, final String description) {
        if (trigger) {
            LOG.debug(format("[%s] [true]", description));
            return arg + " ";
        }
        LOG.debug(format("[%s] [false]", description));
        return "";
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
                .consumerError(LOG::error)
                .dir(PROJECT_DIR).timeoutMs(32000);
    }
}
