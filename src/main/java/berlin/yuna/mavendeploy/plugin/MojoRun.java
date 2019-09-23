package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.Deploy;
import berlin.yuna.mavendeploy.config.Gpg;
import berlin.yuna.mavendeploy.config.JavaSource;
import berlin.yuna.mavendeploy.config.Javadoc;
import berlin.yuna.mavendeploy.config.PluginUpdater;
import berlin.yuna.mavendeploy.config.ReadmeBuilder;
import berlin.yuna.mavendeploy.config.Scm;
import berlin.yuna.mavendeploy.config.Surefire;
import berlin.yuna.mavendeploy.config.Versions;
import berlin.yuna.mavendeploy.logic.GitService;
import berlin.yuna.mavendeploy.logic.SemanticService;
import berlin.yuna.mavendeploy.logic.SettingsXmlReader;
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
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readDeveloperProperties;
import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readLicenseProperties;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executionEnvironment;
import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;
import static berlin.yuna.mavendeploy.util.MojoUtil.isPresent;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

//https://stackoverflow.com/questions/53954902/custom-maven-plugin-development-getartifacts-is-empty-though-dependencies-are
@Mojo(name = "run",
        threadSafe = true,
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyResolution = ResolutionScope.TEST)
public class MojoRun extends AbstractMojo {

    private static final String JAVA_VERSION = "12";
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;
    @Component
    private BuildPluginManager pluginManager;
    @Parameter(defaultValue = "${session}")
    private MavenSession maven;
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

    @Parameter(property = "settings.xml")
    private List<String> SETTINGS;

    private Logger LOG;
    private GitService GIT_SERVICE;
    private SemanticService SEMANTIC_SERVICE;
    private boolean HAS_GIT_CHANGES;
    private boolean SNAPSHOT_DEPLOYMENT = false;
    private MojoExecutor.ExecutionEnvironment ENVIRONMENT;
    private PluginSession SESSION;

    public void execute() {
        before();

        setParameter("maven.test.skip", SESSION.getBoolean("test.skip").orElse(false).toString());
        final List<Server> serverList = SettingsXmlReader.read(SESSION);
        serverList.forEach(server -> LOG.info("+ [%s] added %s", Settings.class.getSimpleName(), SESSION.toString(server)));
        serverList.forEach(server -> SESSION.getMavenSession().getSettings().addServer(server));

        try {
            LOG.info("Preparing information");
            try {
                final boolean isLibrary = isLibrary();
                LOG.debug("Project is library [%s]", isLibrary());
                final String newProjectVersion = prepareProjectVersion();
                final String newTag = prepareNewTagVersion(newProjectVersion);
                final boolean hasNewTag = hasNewTag(newTag, GIT_SERVICE.getLastGitTag());
                final String deployUrl = isTrue("deploy", "deploy.snapshot") ? prepareDeployUrl() : null;

                //SET GIT PROPERTIES
                for (Map.Entry<Object, Object> config : GIT_SERVICE.getConfig().entrySet()) {
                    setParameter("git." + config.getKey().toString(), config.getValue().toString());
                }

                //SET PROJECT DEVELOPER PROPERTIES
                for (Map.Entry<Object, Object> config : readDeveloperProperties(project.getDevelopers()).entrySet()) {
                    setParameter(config.getKey().toString(), config.getValue().toString());
                }

                //SET PROJECT LICENSE PROPERTIES
                for (Map.Entry<Object, Object> config : readLicenseProperties(project.getLicenses()).entrySet()) {
                    setParameter(config.getKey().toString(), config.getValue().toString());
                }

                //SET PROPERTIES
                setWhen("project.library", String.valueOf(isLibrary));
                setWhen("newVersion", newProjectVersion, !isEmpty(newProjectVersion) && !newProjectVersion.equalsIgnoreCase(project.getVersion()));
                setWhen("removeSnapshot", "true", isTrue("remove.snapshot"));
                setWhen("generateBackupPoms", "false", true);
                setWhen("test.integration", SESSION.getParamPresent("test.int").orElse(null));
                setWhen("java.version", JAVA_VERSION, !hasText("java.version"));
                final Optional<String> javaVersion = SESSION.getParamPresent("java.version");
                setWhen("source", prepareSourceVersion(javaVersion.orElse(null)));
                setWhen("target", prepareSourceVersion(javaVersion.orElse(null)));
                setWhen("compilerVersion", javaVersion.orElse(null));
                setWhen("javadocVersion", javaVersion.orElse(null));
                setWhen("project.encoding", UTF_8.toString(), SESSION.getParamPresent("project.encoding").isEmpty());
                setWhen("encoding", SESSION.getParamPresent("project.encoding").orElse(null));
                setWhen("project.build.sourceEncoding", SESSION.getParamPresent("project.encoding").orElse(null));
                setWhen("project.reporting.outputEncoding", SESSION.getParamPresent("project.encoding").orElse(null));
                setWhen("allowSnapshots", "true", isTrue("update.minor", "update.major"));
                setWhen("allowMajorUpdates", SESSION.getBoolean("update.major").orElse(false).toString());
                setWhen("scm.provider", "scm:git", !hasText("scm.provider"));
                setWhen("connectionUrl", getConnectionUrl(), !hasText("connectionUrl"));
                setWhen("project.scm.connection", getConnectionUrl(), !hasText("project.scm.connection"));
                overwriteWhen("tag", newTag, !isEmpty(newTag));
                setWhen("altDeploymentRepository", deployUrl, !isEmpty(deployUrl));
                setWhen("message", prepareCommitMessage(newProjectVersion, hasNewTag, isTrue("update.minor", "update.major")), (hasNewTag && !hasText("message")));

                //RUN MOJOS
                runWhen(() -> Clean.build(SESSION).clean(), isTrue("clean", "clean.cache"));
                runWhen(() -> ReadmeBuilder.build(SESSION).render(), isTrue("builder"));
                runWhen(() -> Dependency.build(SESSION).resolvePlugins(), isTrue("clean", "clean.cache"));
                runWhen(() -> Dependency.build(SESSION).purgeLocalRepository(), isTrue("clean.cache"));
                runWhen(() -> Versions.build(SESSION).updateParent(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(SESSION).updateProperties(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(SESSION).updateProperties(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(SESSION).updateChildModules(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(SESSION).useLatestReleases(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(SESSION).useLatestVersions(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(SESSION).useNextSnapshots(), isTrue("update.major", "update.minor"));
                runWhen(() -> PluginUpdater.build(SESSION).update(), SESSION.getBoolean("update.plugins").orElse(false));
                runWhen(() -> Versions.build(SESSION).commit(), isTrue("update.major", "update.minor"));
                runWhen(() -> Versions.build(SESSION).set(), hasText("newVersion"), isTrue("removeSnapshot"));
                runWhen(() -> Javadoc.build(SESSION).jar(), (!isLibrary() && isTrue("java.doc")));
                runWhen(() -> JavaSource.build(SESSION).jarNoFork(), (!isLibrary() && isTrue("java.source")));

                //MOJO TEST
//                runWhen(() -> Resources.build(SESSION).resource(), isTrue("test.run", "test.unit", "test.integration"));
                runWhen(() -> berlin.yuna.mavendeploy.config.Compiler.build(SESSION).compiler(), isTrue("test.run", "test.unit", "test.integration"));
//                runWhen(() -> Resources.build(SESSION).testResource(), isTrue("test.run", "test.unit", "test.integration"));
                runWhen(() -> berlin.yuna.mavendeploy.config.Compiler.build(SESSION).testCompiler(), isTrue("test.run", "test.unit", "test.integration"));
                runWhen(() -> Surefire.build(SESSION).test(), isTrue("test.run", "test.unit"));

                //Should stay at the end after everything is done
                runWhen(() -> Gpg.build(SESSION).sign(), hasText("gpg.passphrase"));
                runWhen(() -> Scm.build(SESSION).tag(), hasNewTag);
                runWhen(() -> Deploy.build(SESSION).deploy(), hasText("altDeploymentRepository"));

                //remove snapshot if only added for deployment
                if (SNAPSHOT_DEPLOYMENT) {
                    overwriteWhen("oldVersion", newProjectVersion, true);
                    overwriteWhen("newVersion", newProjectVersion.split("-SNAPSHOT")[0], true);
                    runWhen(() -> Versions.build(SESSION).set(), true);
                }

                printJavaDoc();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            after();
        }
    }

    private String prepareSourceVersion(final String javaVersion) {
        if (isPresent(javaVersion)) {
            final int index = javaVersion.indexOf('.');
            switch (index) {
                case -1:
                    //failed
                    return javaVersion;
                case 1:
                    //old versions like 1.8
                    return javaVersion.substring(index + 1);
                default:
                    //new versions like 10.11.2
                    return javaVersion.substring(0, index);
            }
        }
        return null;
    }

    private String prepareDeployUrl() {
        final Optional<String> deployUrl = SESSION.getParamPresent("deploy.url");
        if (deployUrl.isPresent()) {
            final String paramName = "deploy.id";
            final Optional<String> deployId = SESSION.getParamPresent(paramName);
            LOG.debug("DeployUrl [%s]", deployUrl.get());
            if (deployId.isEmpty()) {
                final Optional<Server> server = findServerByDeployUrl(deployUrl.get(), paramName);
                if (server.isPresent()) {
                    LOG.info("Fallback to deployId [%s]", server.get().getId());
                    return server.get().getId() + "::default::" + deployUrl.get();
                }
                LOG.error("Cant find any credentials for deploy.id [%s] deploy.url [%s]", deployId.orElse(null), deployUrl.get());
            } else if (maven.getSettings().getServer(deployId.get()) != null && !isEmpty(maven.getSettings().getServer(deployId.get()).getId())) {
                LOG.info("DeployId [%s] deployUrl [%s]", deployId.get(), deployUrl.get());
                return deployId.get() + "::default::" + deployUrl.get();
            }
            LOG.error("Cant find any credentials for deploy.id [%s] deploy.url [%s]", deployId.orElse(null), deployUrl.get());
        }
        return null;
    }

    private Optional<Server> findServerByDeployUrl(final String deployUrl, final String paramName) {
        final String[] artifactRepositories = Arrays
                .stream(new String[]{"nexus", "artifact", "archiva", "repository", "snapshot"})
                .sorted(Comparator.comparingInt(o -> (deployUrl.toLowerCase().contains(o.toLowerCase()) ? -1 : 1)))
                .toArray(String[]::new);
        LOG.warn("[%s] not set", paramName);
        return getServerContains(artifactRepositories);
    }

    private Optional<Server> getServerContains(final String... names) {
        final List<Server> servers = maven.getSettings().getServers();
        if (servers != null && !servers.isEmpty())
            for (String name : names) {
                final Optional<Server> server = servers.stream()
                        .filter(s -> !isEmpty(s.getId()))
                        .filter(s -> s.getId().toLowerCase().contains(name) || (!isEmpty(s.getId()) && s.getUsername().toLowerCase().contains(name)))
                        .findFirst();
                if (server.isPresent()) {
                    return server;
                }
            }
        return Optional.empty();
    }

    private String getTagVersion(final String property, final String newProjectVersion) {
        if (isTrue(property)) {
            return newProjectVersion;
        } else {
            final Optional<String> param = SESSION.getParamPresent(property);
            if (param.isPresent() && !param.get().equalsIgnoreCase("false")) {
                return param.get();
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
        final String scmProvider = SESSION.getParamPresent("scm.provider").orElse("scm:git");
        final String connectionUrl = isEmpty(originUrl) ? basedir.toURI().toString() : originUrl;
        return connectionUrl.startsWith(scmProvider) ? connectionUrl : scmProvider + ":" + connectionUrl;
    }

    private String prepareCommitMessage(final String projectVersion, final boolean hasNewTag, final boolean update) {
        return format("[%s]", projectVersion)
                + format(" [%s]", getBranchName())
                + (hasNewTag ? " [TAG]" : "")
                + (update ? " [UPDATE]" : "")
                ;
    }

    private Optional<String> getBranchName() {
        return SEMANTIC_SERVICE.getBranchName();
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
        final String projectVersion = SESSION.getParamPresent("project.version").orElse(null);
        final String result = SESSION.getParamPresent("semantic.format").isEmpty() ?
                projectVersion : SEMANTIC_SERVICE.getNextSemanticVersion(project.getVersion(), projectVersion);
        LOG.debug("Prepared project version [%s]", projectVersion);

        //ADD SNAPSHOT
        final String snapshotVersion = isEmpty(projectVersion) ? project.getVersion() : projectVersion;
        if ((isTrue("project.snapshot") || isTrue("deploy.snapshot")) && !snapshotVersion.endsWith("-SNAPSHOT")) {
            SNAPSHOT_DEPLOYMENT = true;
            return snapshotVersion + "-SNAPSHOT";
        }
        return result;
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
        ENVIRONMENT = executionEnvironment(project, maven, pluginManager);
        SESSION = new PluginSession(ENVIRONMENT, LOG);

        GIT_SERVICE = new GitService(LOG, basedir, SESSION.getBoolean("fake").orElse(false));
        SEMANTIC_SERVICE = new SemanticService(GIT_SERVICE, SESSION.getParamPresent("semantic.format").orElse(null));

        HAS_GIT_CHANGES = GIT_SERVICE.gitHasChanges();

        if (HAS_GIT_CHANGES) {
            LOG.warn("Stashing uncommitted git changes");
            GIT_SERVICE.gitStash();
        }
        if (maven.getSettings().getServers() == null) {
            maven.getSettings().setServers(new ArrayList<>());
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

    private void setWhen(final String key, final String value) {
        if (isPresent(value)) {
            setWhen(key, value, true);
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
                LOG.debug("+ Config added key [%s] value [%s]", key, value);
                maven.getUserProperties().setProperty(key, value);
                break;
            }
        }
    }

    private void setParameter(final String key, final String value) {
        requireNonNull(key, "setParameter key is null");
        final String cmdValue = maven.getUserProperties().getProperty(key);
        if (isEmpty(cmdValue)) {
            LOG.info("+ Config added key [%s] value [%s]", key, value);
            maven.getUserProperties().setProperty(key, value);
        } else {
            LOG.warn(format("- Config key [%s] already set with [%s] - won't take action", key, cmdValue));
        }
    }

    private boolean isLibrary() {
        return isEmpty(project.getPackaging()) || project.getPackaging().equals("pom");
    }

    private boolean isTrue(final String... keys) {
        return SESSION.isTrue(keys);
    }

    private boolean hasText(final String... keys) {
        return SESSION.hasText(keys);
    }
}
