package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.clu.logic.CommandLineReader;
import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.JavaSource;
import berlin.yuna.mavendeploy.config.Javadoc;
import berlin.yuna.mavendeploy.config.Versions;
import berlin.yuna.mavendeploy.logic.GitService;
import berlin.yuna.mavendeploy.logic.SemanticService;
import berlin.yuna.mavendeploy.model.ThrowingFunction;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executionEnvironment;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.isEmpty;
import static java.lang.String.format;

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

    //Versioning
    @Parameter(property = "project.version", defaultValue = "", readonly = false)
    private String PROJECT_VERSION;
    @Parameter(property = "semantic.format", defaultValue = "", readonly = false)
    private String SEMANTIC_FORMAT;
    @Parameter(property = "tag", defaultValue = "", readonly = false)
    private String TAG;
    @Parameter(property = "tag.break", defaultValue = "", readonly = false)
    private String TAG_BREAK;
    @Parameter(property = "update.major", defaultValue = "", readonly = false)
    private String UPDATE_MAJOR;
    @Parameter(property = "update.minor", defaultValue = "", readonly = false)
    private String UPDATE_MINOR;
    @Parameter(property = "commit", defaultValue = "", readonly = false)
    private String COMMIT;

    //building
    @Parameter(property = "java.doc", defaultValue = "", readonly = false)
    private String JAVA_DOC;
    @Parameter(property = "java.src", defaultValue = "", readonly = false)
    private String SOURCE;
    @Parameter(property = "profiles", defaultValue = "", readonly = false)
    private String PROFILES;
    @Parameter(property = "gpg.pass", defaultValue = "", readonly = false)
    private String GPG_PASS;
    @Parameter(property = "gpg.pass.alt", defaultValue = "", readonly = false)
    private String GPG_PASS_ALT;

    //deployment
    @Parameter(property = "deploy.id", defaultValue = "", readonly = false)
    private String DEPLOY_ID;
    @Parameter(property = "release", defaultValue = "", readonly = false)
    private String RELEASE;

    //Settings.xml FIXME: one line arg?
    @Parameter(property = "settings.xml", defaultValue = "", readonly = false)
    private List<String> SETTINGS;

    //misc
    @Parameter(property = "report", defaultValue = "", readonly = false)
    private String REPORT;
    @Parameter(property = "encoding", defaultValue = "", readonly = false)
    private String ENCODING;
    @Parameter(property = "java.version", defaultValue = "", readonly = false)
    private String JAVA_VERSION;

    private Log LOG;
    private GitService GIT_SERVICE;
    private SemanticService SEMANTIC_SERVICE;
    private boolean HAS_GIT_CHANGES;
    private MojoExecutor.ExecutionEnvironment ENVIRONMENT;

    public void execute() {
        before();

        setParameter("maven.test.skip", getParam("test.skip", true).toString());
        addServerToSettings(new CommandLineReader(SETTINGS.toArray(new String[0])));

        try {
            LOG.info("Preparing information");
            try {
                LOG.debug(format("Project is library [%s]", isLibrary()));
                final String newProjectVersion = prepareProjectVersion();
                setWhen("newVersion", newProjectVersion, newProjectVersion != null && !newProjectVersion.equalsIgnoreCase(project.getVersion()));
                setWhen("removeSnapshot", "true", isTrue("remove.snapshot"));

                setWhen("generateBackupPoms", "false", true);
                runWhen(() -> Clean.build(ENVIRONMENT, LOG).clean(), isTrue("clean", "clean.cache"));
                runWhen(() -> Dependency.build(ENVIRONMENT, LOG).resolvePlugins(), isTrue("clean", "clean.cache"));
                runWhen(() -> Dependency.build(ENVIRONMENT, LOG).purgeLocalRepository(), isTrue("clean.cache"));

                setWhen("allowSnapshots", "true", isTrue("update.minor", "update.major"));
                setWhen("allowMajorUpdates", getParam("update.major", false).toString(), isTrue("update.minor", "update.major"));

                runWhen(() -> Versions.build(ENVIRONMENT, LOG).updateParent(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).updateProperties(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).updateProperties(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).updateChildModules(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).useLatestReleases(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).useLatestVersions(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).useNextSnapshots(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(ENVIRONMENT, LOG).commit(), isTrue("update.major", "update.minor"));

                runWhen(() -> Versions.build(ENVIRONMENT, LOG).set(), hasText("newVersion"), hasText("removeSnapshot"));

                if (!isLibrary()) {
                    runWhen(() -> Javadoc.build(ENVIRONMENT, LOG).jar(), isTrue("java.doc"));
                    runWhen(() -> JavaSource.build(ENVIRONMENT, LOG).jarNoFork(), isTrue("java.source"));
                }


            } catch (Exception e) {
                LOG.error(e);
            }
        } finally {
            after();
        }
    }

    private String prepareProjectVersion() {
        String projectVersion = getParam("project.version", null);
        final String semanticFormat = getParam("semantic.format", null);
        projectVersion = isEmpty(semanticFormat) ?
                projectVersion : SEMANTIC_SERVICE.getNextSemanticVersion(project.getVersion(), GIT_SERVICE, projectVersion);
        return projectVersion;
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
            LOG.info(format(
                    "+ Settings added [%s] id [%s] user [%s] pass [%s]",
                    Server.class.getSimpleName(),
                    server.getId(), server.getUsername(),
                    server.getPassword() == null ? null : server.getPassword().replaceAll(".?", "*")
            ));
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
        LOG = getLog();
        Objects.requireNonNull(pluginManager);

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

    private void setParameter(final String key, final String value) {
        final String cmdValue = session.getUserProperties().getProperty(key);
        if (isEmpty(cmdValue)) {
            session.getUserProperties().setProperty(key, value);
            LOG.info(format("+ Config added key [%s] value [%s]", key, value));
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
