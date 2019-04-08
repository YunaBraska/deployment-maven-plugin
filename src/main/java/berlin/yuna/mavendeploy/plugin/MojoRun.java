package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.clu.logic.CommandLineReader;
import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.MojoHelper;
import berlin.yuna.mavendeploy.logic.GitService;
import berlin.yuna.mavendeploy.model.ThrowingFunction;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executionEnvironment;
import static java.lang.String.format;
//
//import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
//import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
//import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
//import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
//import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
//import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
//import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
//import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
//import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
//import static org.twdata.maven.mojoexecutor.MojoExecutor.version;


@Mojo(name = "run")
public class MojoRun extends AbstractMojo {

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;
    @Component
    private BuildPluginManager pluginManager;
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

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

    private Log log;
    private GitService gitService;
    private boolean gitChanges;
    private MojoExecutor.ExecutionEnvironment environment;

    public void execute() {
        before();

        session.getUserProperties().setProperty("maven.test.skip", getBoolean("test.skip", true).toString());
        initSettings(new CommandLineReader(SETTINGS.toArray(new String[0])));

        try {
            log.info("Preparing information");
            try {
                runOnBoolean(() -> Clean.build(environment, log).clean(), "clean", "clean.cache");
                runOnBoolean(() -> Dependency.build(environment, log).resolvePlugins(), "clean", "clean.cache");
                runOnBoolean(() -> Dependency.build(environment, log).purgeLocalRepository(), "clean.cache");
            } catch (Exception e) {
                log.error(e);
            }
        } finally {
            after();
        }
    }

    private void initSettings(final CommandLineReader clr) {
        final List<String> serverList = clr.getValues("SERVER");
        for (int i = 0; i < serverList.size(); i++) {
            log.info(format("+ [Settings] adding [%s] [%s]", Server.class.getSimpleName(), serverList.get(i)));
            final Server server = new Server();
            server.setId(serverList.get(i));
            server.setUsername(clr.getValue(i, "USERNAME"));
            server.setUsername(clr.getValue(i, "PASSWORD"));
            session.getSettings().addServer(server);
        }
    }

    private void after() {
        if (gitChanges) {
            log.warn("Load uncommitted git changes");
            gitService.gitLoadStash();
        }
    }

    private void before() {
        log = getLog();
        Objects.requireNonNull(pluginManager);

        MojoExecutor.setLogger(log);
        environment = executionEnvironment(project, session, pluginManager);

        gitService = new GitService(log, basedir, getBoolean("fake", false));
        gitChanges = gitService.gitHasChanges();

        if (gitChanges) {
            log.warn("Stashing uncommitted git changes");
            gitService.gitStash();
        }
    }

    private void runOnBoolean(final ThrowingFunction consumer, final String... keys) throws Exception {
        for (String key : keys) {
            if (getBoolean(key, false)) {
                consumer.run();
                break;
            }
        }
    }

    private Boolean getBoolean(final String key, final boolean fallback) {
        return MojoHelper.getBoolean(session, key, fallback);
    }

}
