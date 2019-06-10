package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.clu.logic.CommandLineReader;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.Gpg;
import berlin.yuna.mavendeploy.config.JavaSource;
import berlin.yuna.mavendeploy.config.Javadoc;
import berlin.yuna.mavendeploy.config.Resources;
import berlin.yuna.mavendeploy.config.Scm;
import berlin.yuna.mavendeploy.config.Surefire;
import berlin.yuna.mavendeploy.config.Versions;
import berlin.yuna.mavendeploy.logic.GitService;
import berlin.yuna.mavendeploy.logic.SemanticService;
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.model.ThrowingFunction;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executionEnvironment;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.isEmpty;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

//https://stackoverflow.com/questions/53954902/custom-maven-plugin-development-getartifacts-is-empty-though-dependencies-are
@Mojo(name = "run",
        threadSafe = true,
        defaultPhase = LifecyclePhase.COMPILE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class MojoRun extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;
    @Component
    private BuildPluginManager pluginManager;
    @Parameter(defaultValue = "${session}", readonly = false)
    private MavenSession session;
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;
    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    private MojoExecution mojo;
    @Parameter(defaultValue = "${plugin}", readonly = true) // Maven 3 only
    private PluginDescriptor plugin;
    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File target;

    @Parameter(property = "settings.xml", defaultValue = "", readonly = false)
    private List<String> SETTINGS;

    private Logger LOG;
    private GitService GIT_SERVICE;
    private SemanticService SEMANTIC_SERVICE;
    private boolean HAS_GIT_CHANGES;
    private MojoExecutor.ExecutionEnvironment ENVIRONMENT;

    public void execute() {
        before();

        setParameter("maven.test.skip", getParam("test.skip", false).toString());
        addServerToSettings(new CommandLineReader(SETTINGS.toArray(new String[0])));
        addGpgToSettings();

        try {
            LOG.info("Preparing information");
            try {
                LOG.debug("Project is library [%s]", isLibrary());
                final String newProjectVersion = prepareProjectVersion();
                final String newTag = prepareNewTagVersion(newProjectVersion);
                final boolean hasNewTag = hasNewTag(newTag, GIT_SERVICE.getLastGitTag());

                //SET PROPERTIES
                setWhen("newVersion", newProjectVersion, !isEmpty(newProjectVersion) && !newProjectVersion.equalsIgnoreCase(project.getVersion()));
                setWhen("removeSnapshot", "true", isTrue("remove.snapshot"));
                setWhen("generateBackupPoms", "false", true);
                setWhen("test.integration", getParam("test.int", null), hasText("test.int"));
                setWhen("source", getParam("java.version", null), hasText("java.version"));
                setWhen("target", getParam("java.version", null), hasText("java.version"));
                setWhen("compilerVersion", getParam("java.version", null), hasText("java.version"));
                setWhen("java.version", "1.8", !isEmpty(project.getProperties().getProperty("java.version")));
                setWhen("javadocVersion", project.getProperties().getProperty("java.version"), !isEmpty(project.getProperties().getProperty("java.version")));
                setWhen("project.encoding", project.getProperties().getProperty("project.encoding"), !isEmpty(project.getProperties().getProperty("project.encoding")));
                setWhen("encoding", getParam("project.encoding", null), hasText("project.encoding"));
                setWhen("project.build.sourceEncoding", getParam("project.encoding", null), hasText("project.encoding"));
                setWhen("project.reporting.outputEncoding", getParam("project.encoding", null), hasText("project.encoding"));
                setWhen("allowSnapshots", "true", isTrue("update.minor", "update.major"));
                setWhen("allowMajorUpdates", getParam("update.major", false).toString(), isTrue("update.minor", "update.major"));
                setWhen("scm.provider", "scm:git", !hasText("scm.provider"));
                setWhen("connectionUrl", getConnectionUrl(), !hasText("connectionUrl"));
                setWhen("project.scm.connection", getConnectionUrl(), !hasText("project.scm.connection"));
                overwriteWhen("tag", newTag, !isEmpty(newTag));
                setWhen("message", prepareCommitMessage(newProjectVersion, hasNewTag, isTrue("update.minor", "update.major")), (hasNewTag && !hasText("message")));

                //RUN MOJOS
                runWhen(() -> Clean.build(ENVIRONMENT, LOG).clean(), isTrue("clean", "clean.cache"));
                runWhen(() -> Dependency.build(ENVIRONMENT, LOG).resolvePlugins(), isTrue("clean", "clean.cache"));
                runWhen(() -> Dependency.build(ENVIRONMENT, LOG).purgeLocalRepository(), isTrue("clean.cache"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).updateParent(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).updateProperties(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).updateProperties(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).updateChildModules(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).useLatestReleases(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).useLatestVersions(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).useNextSnapshots(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).commit(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).set(), hasText("newVersion"), isTrue("removeSnapshot"));
                runWhen(() -> Javadoc.build(ENVIRONMENT, LOG).jar(), (!isLibrary() && isTrue("java.doc")));
                runWhen(() -> JavaSource.build(ENVIRONMENT, LOG).jarNoFork(), (!isLibrary() && isTrue("java.source")));

                //MOJO TEST
                runWhen(() -> Resources.build(ENVIRONMENT, LOG).resource(), isTrue("test.run", "test.unit", "test.integration"));
//                runWhen(() -> Compiler.build(ENVIRONMENT, LOG).compiler(), isTrue("test.run", "test.unit", "test.integration"));
                new Terminal().dir(basedir).consumerInfo(s -> LOG.info(s)).consumerError(s -> LOG.error(s)).execute("mvn compiler:compile -Denoding=UTF-8 -Dsource=1.8 -Dtarget=1.8 -DcompilerVersion=1.8");
                runWhen(() -> Resources.build(ENVIRONMENT, LOG).testResource(), isTrue("test.run", "test.unit", "test.integration"));
//                runWhen(() -> Compiler.build(ENVIRONMENT, LOG).testCompiler(), isTrue("test.run", "test.unit", "test.integration"));
                new Terminal().dir(basedir).consumerInfo(s -> LOG.info(s)).consumerError(s -> LOG.error(s)).execute("mvn compiler:testCompile -Denoding=UTF-8 -Dsource=1.8 -Dtarget=1.8 -DcompilerVersion=1.8");
                runWhen(() -> Surefire.build(ENVIRONMENT, LOG).test(), isTrue("test.run", "test.unit"));


                //Should stay at the end after everything is
                runWhen(() -> Gpg.build(ENVIRONMENT, LOG).sign(), hasText("gpg.passphrase"));
                runWhen(() -> Scm.build(ENVIRONMENT, LOG).tag(), hasNewTag);


                //TODO: git-commit-id-plugin
                //TODO: JACOCO
                //TODO: DUPLICATE FINDER (not test resources)
                //TODO: GIT CREDENTIALS
                //TODO: FAILSAFE
                //TODO: SURFIRE
                //TODO: REPORT

                //TODO: git push everything at the end when success?

                printJavaDoc();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            after();
        }
    }

    private String getTagVersion(final String property, final String newProjectVersion) {
        if (isTrue(property)) {
            return newProjectVersion;
        } else {
            final String param = getParam(property, null);
            if (!isEmpty(property) && !"false".equals(param)) {
                return param;
            }
        }
        return null;
    }

    private String prepareNewTagVersion(final String newProjectVersion) {
        final String tag = getTagVersion("tag", newProjectVersion);
        final String tagBreak = getTagVersion("tag.break", newProjectVersion);
        return isEmpty(tag) ? tagBreak : tag;
    }

    private String getConnectionUrl() {
        final String originUrl = GIT_SERVICE.getOriginUrl();
        final String scmProvider = getParam("scm.provider", "scm:git");
        final String connectionUrl = isEmpty(originUrl) ? basedir.toURI().toString() : originUrl;
        return connectionUrl.startsWith(scmProvider) ? connectionUrl : scmProvider + ":" + connectionUrl;
    }

    public String prepareCommitMessage(final String projectVersion, final boolean hasNewTag, final boolean update) {
        return format("[%s]", projectVersion)
                + format(" [%s]", getBranchName())
                + (hasNewTag ? " [TAG]" : "")
                + (update ? " [UPDATE]" : "")
                ;
    }

    private String getBranchName() {
        final String branchName = SEMANTIC_SERVICE.getBranchName();
        return branchName == null ? GIT_SERVICE.findOriginalBranchName(1) : "N/A";
    }

    private boolean hasNewTag(final String newTag, final String lastGitTag) {
        if (!isEmpty(newTag)) {
            LOG.debug("Tagging requested [%s], last tag was [%s]", newTag, lastGitTag);
            printTagMessage(isTrue("tag.break"), newTag, lastGitTag);
            return !isEmpty(newTag) && !newTag.equalsIgnoreCase(lastGitTag);
        }
        return false;
    }

    private void printTagMessage(final boolean tagBreak, final String newProjectVersion, final String lastGitTag) {
        if (tagBreak && newProjectVersion.equalsIgnoreCase(lastGitTag)) {
            throw new RuntimeException(format("Git tag [%s] already exists", newProjectVersion));
        } else if (newProjectVersion.equalsIgnoreCase(lastGitTag)) {
            LOG.info("Git tag [%s] already exists", newProjectVersion);
        } else {
            LOG.info("New git tag [%s]", newProjectVersion);
        }
    }

    private void printJavaDoc() {
        final File javaDocFile = new File(basedir, "target/apidocs/index.html");
        if (javaDocFile.exists()) {
            LOG.info("JavaDoc [file://%s]", javaDocFile.toURI().getRawPath());
        }
    }

    private String prepareProjectVersion() {
        String projectVersion = getParam("project.version", null);
        final String semanticFormat = getParam("semantic.format", null);
        projectVersion = isEmpty(semanticFormat) ?
                projectVersion : SEMANTIC_SERVICE.getNextSemanticVersion(project.getVersion(), GIT_SERVICE, projectVersion);
        LOG.debug("Prepared project version [%s]", projectVersion);
        return projectVersion;
    }

    private void addGpgToSettings() {
        final String gpgPassphrase = getParam("gpg.pass", getParam("gpg.passphrase", null));
        if (!isEmpty(gpgPassphrase)) {
            LOG.info("Creating GPG settings");
            final Profile profile = new Profile();
            final Activation activation = new Activation();
            final Properties properties = new Properties();
            activation.setActiveByDefault(true);
            properties.setProperty("gpg.executable", "gpg");
            properties.setProperty("gpg.passphrase", gpgPassphrase);
            profile.setActivation(activation);
            profile.setProperties(properties);
            session.getSettings().getProfiles().add(profile);
        }
    }

    private void addServerToSettings(final CommandLineReader clr) {
        final List<String> serverList = clr.getValues("SERVER");
        for (int i = 0; i < serverList.size(); i++) {
            final Server server = new Server();
            server.setId(serverList.get(i));
            server.setUsername(clr.getValue(i, "Username"));
            server.setPassword(clr.getValue(i, "Password"));
            server.setPrivateKey(clr.getValue(i, "PrivateKey"));
            server.setPassphrase(clr.getValue(i, "Passphrase"));
            server.setFilePermissions(clr.getValue(i, "FilePermissions"));
            server.setDirectoryPermissions(clr.getValue(i, "DirectoryPermissions"));
            LOG.info(
                    "+ Settings added [%s] id [%s] user [%s] pass [%s]",
                    Server.class.getSimpleName(),
                    server.getId(), server.getUsername(),
                    server.getPassword() == null ? null : server.getPassword().replaceAll(".?", "*")
            );
            session.getSettings().addServer(server);
        }
    }

    private void after() {
        if (HAS_GIT_CHANGES) {
            LOG.warn("Load uncommitted git changes");
            GIT_SERVICE.gitLoadStash();
        }
    }

    private void before() {
        LOG = new Logger(getLog());
        requireNonNull(pluginManager);

        MojoExecutor.setLogger(LOG);
        ENVIRONMENT = executionEnvironment(project, session, pluginManager);

        GIT_SERVICE = new GitService(LOG, basedir, getParam("fake", false));
        SEMANTIC_SERVICE = new SemanticService(getParam("semantic.format", "\\.:none"));
        HAS_GIT_CHANGES = GIT_SERVICE.gitHasChanges();

        if (HAS_GIT_CHANGES) {
            LOG.warn("Stashing uncommitted git changes");
            GIT_SERVICE.gitStash();
        }
    }

    private void runWhen(final ThrowingFunction consumer, final boolean... when) throws Exception {
        for (boolean trigger : when) {
            if (trigger) {
                consumer.run();
                break;
            }
        }
    }

    private void setWhen(final String key, final String value, final boolean... when) {
        for (boolean trigger : when) {
            if (trigger) {
                setParameter(key, value);
                break;
            }
        }
    }

    private void overwriteWhen(final String key, final String value, final boolean... when) {
        for (boolean trigger : when) {
            if (trigger) {
                LOG.debug("+ Config added key [tag] value [%s]", value);
                session.getUserProperties().setProperty(key, value);
                break;
            }
        }
    }

    private void setParameter(final String key, final String value) {
        requireNonNull(key, "setParameter key is null");
        final String cmdValue = session.getUserProperties().getProperty(key);
        if (isEmpty(cmdValue)) {
            LOG.info("+ Config added key [%s] value [%s]", key, value);
            session.getUserProperties().setProperty(key, value);
        } else {
            LOG.warn(format("- Config key [%s] already set with [%s] - won't take action", key, cmdValue));
        }
    }

    private boolean isLibrary() {
        return isEmpty(project.getPackaging()) || project.getPackaging().equals("pom");
    }

    private boolean isTrue(final String... keys) {
        for (String key : keys) {
            if (MojoHelper.getBoolean(session, key, false)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(final String... keys) {
        for (String key : keys) {
            if (!isEmpty(MojoHelper.getString(session, key, null))) {
                return true;
            }
        }
        return false;
    }

    private Boolean getParam(final String key, final boolean fallback) {
        return MojoHelper.getBoolean(session, key, fallback);
    }

    private String getParam(final String key, final String fallback) {
        return MojoHelper.getString(session, key, fallback);
    }

}
