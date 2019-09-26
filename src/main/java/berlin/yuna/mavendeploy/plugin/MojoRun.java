package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.Deploy;
import berlin.yuna.mavendeploy.config.Gpg;
import berlin.yuna.mavendeploy.config.JavaSource;
import berlin.yuna.mavendeploy.config.Javadoc;
import berlin.yuna.mavendeploy.config.PluginUpdater;
import berlin.yuna.mavendeploy.config.PropertyWriter;
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
import java.util.Optional;

import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readDeveloperProperties;
import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readLicenseProperties;
import static berlin.yuna.mavendeploy.logic.SettingsXmlReader.getGpgPath;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executionEnvironment;
import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
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
    @Parameter(defaultValue = "${project.basedir}")
    private File basedir;
    @Component
    private BuildPluginManager pluginManager;
    @Parameter(defaultValue = "${session}")
    private MavenSession maven;
    @Parameter(defaultValue = "${project}")
    private MavenProject project;
    @Parameter(defaultValue = "${mojoExecution}")
    private MojoExecution plugin;

    private Logger LOG;
    private GitService GIT_SERVICE;
    private SemanticService SEMANTIC_SERVICE;
    private MojoExecutor.ExecutionEnvironment ENVIRONMENT;
    private PluginSession SESSION;

    public MojoRun() {}

    public void execute() {
        before();
        SESSION.setParameter("maven.test.skip", SESSION.getBoolean("test.skip").orElse(false).toString());
        final List<Server> serverList = SettingsXmlReader.read(SESSION);
        serverList.forEach(server -> LOG.info("+ [%s] added %s", Settings.class.getSimpleName(), SESSION.toString(server)));
        serverList.forEach(server -> SESSION.getMavenSession().getSettings().addServer(server));

        LOG.info("%s Preparing information", unicode(0x1F453));
        try {
            final boolean isLibrary = isLibrary();
            final String newProjectVersion = prepareProjectVersion();
            final String newTag = prepareNewTagVersion(newProjectVersion);
            final boolean hasNewTag = hasNewTag(newTag, GIT_SERVICE.getLastGitTag());
            final String deployUrl = isTrue("deploy", "deploy.snapshot") ? prepareDeployUrl() : null;

            LOG.info("%s STEP [1/6] SETUP MOJO PROPERTIES", unicode(0x1F4DD));
            //SET GIT PROPERTIES
            GIT_SERVICE.getConfig().forEach((key, value) -> setWhen("git." + key, value));
            readDeveloperProperties(project.getDevelopers()).forEach(this::setWhen);
            readLicenseProperties(project.getLicenses()).forEach(this::setWhen);
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
            SESSION.overwriteWhen("tag", newTag, !isEmpty(newTag));
            setWhen("altDeploymentRepository", deployUrl, !isEmpty(deployUrl));
            setWhen("gpg.passphrase", SESSION.getParamPresent("gpg.pass", "gpg.passphrase").orElse(null));
            setWhen("passphraseServerId", SESSION.getParamPresent("gpg.passphrase").orElse(null));
            setWhen("gpg.executable", getGpgPath(LOG), hasText("gpg.passphrase"));

            LOG.info("%s STEP [2/6] RUN PLUGINS WITH SETUP", unicode(0x2699));
            runWhen(() -> Clean.build(SESSION).clean(), isTrue("clean", "clean.cache"));
            runWhen(() -> Dependency.build(SESSION).resolvePlugins(), isTrue("clean", "clean.cache"));
            runWhen(() -> Dependency.build(SESSION).purgeLocalRepository(), isTrue("clean.cache"));

            LOG.info("%s STEP [3/6] RUN PLUGINS WITH MODIFIERS", unicode(0x1F3D7));
            runWhen(() -> ReadmeBuilder.build(SESSION).render(), isTrue("builder"));
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

            LOG.info("%s STEP [4/6] RUN PLUGINS WITH VERIFIERS", unicode(0x1F50E));
            runWhen(() -> berlin.yuna.mavendeploy.config.Compiler.build(SESSION).compiler(), isTrue("test.run", "test.unit", "test.integration"));
            runWhen(() -> berlin.yuna.mavendeploy.config.Compiler.build(SESSION).testCompiler(), isTrue("test.run", "test.unit", "test.integration"));
            runWhen(() -> Surefire.build(SESSION).test(), isTrue("test.run", "test.unit"));

            LOG.info("%s STEP [5/6] RUN PLUGINS WITH ACTIONS", unicode(0x1F3AC));
            runWhen(() -> PropertyWriter.build(SESSION).write(), hasText("properties.print"));
            runWhen(() -> Javadoc.build(SESSION).jar(), (!isLibrary() && isTrue("java.doc")));
            runWhen(() -> JavaSource.build(SESSION).jarNoFork(), (!isLibrary() && isTrue("java.source")));
            runWhen(() -> Gpg.build(SESSION).sign(), hasText("gpg.passphrase"));
            runWhen(() -> Scm.build(SESSION).tag(), hasNewTag);
            runWhen(() -> Deploy.build(SESSION).deploy(), hasText("altDeploymentRepository"));

            //TODO: implement to push on changes && new parameter change version only on changes version.onchange && tag.onchange
//                if (GIT_SERVICE.gitHasChanges() && SESSION.getBoolean("changes.push").orElse(false)) {
//                    final String message = SESSION.getParamPresent("message").orElse(prepareCommitMessage(newProjectVersion, hasNewTag, isTrue("update.plugins", "update.minor", "update.major")));
//                    setWhen("message", message);
//                    GIT_SERVICE.push();
//                }

            LOG.info("%s STEP [5/6] RUN PLUGINS WITH CLEANUPS", unicode(0x1F9E7));
            //remove snapshot if only added for deployment
            if (SESSION.getBoolean("snapshot.deployment").orElse(false)) {
                SESSION.overwriteWhen("oldVersion", newProjectVersion, true);
                SESSION.overwriteWhen("newVersion", newProjectVersion.split("-SNAPSHOT")[0], true);
                runWhen(() -> Versions.build(SESSION).set(), true);
            }
            printJavaDoc();

        } catch (Exception e) {
            throw new RuntimeException(e);
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
                    LOG.info("%s Fallback to deployId [%s]", unicode(0x1F511), server.get().getId());
                    return server.get().getId() + "::default::" + deployUrl.get();
                }
                LOG.error("%s Cant find any credentials for deploy.id [%s] deploy.url [%s]", unicode(0x1F940), deployId.orElse(null), deployUrl.get());
            } else if (maven.getSettings().getServer(deployId.get()) != null && !isEmpty(maven.getSettings().getServer(deployId.get()).getId())) {
                LOG.info("%s DeployId [%s] deployUrl [%s]", unicode(0x1F511), deployId.get(), deployUrl.get());
                return deployId.get() + "::default::" + deployUrl.get();
            }
            LOG.error("%s Cant find any credentials for deploy.id [%s] deploy.url [%s]", unicode(0x1F940), deployId.orElse(null), deployUrl.get());
        }
        return null;
    }

    private Optional<Server> findServerByDeployUrl(final String deployUrl, final String paramName) {
        final String[] artifactRepositories = Arrays
                .stream(new String[]{"nexus", "artifact", "archiva", "repository", "snapshot"})
                .sorted(Comparator.comparingInt(o -> (deployUrl.toLowerCase().contains(o.toLowerCase()) ? -1 : 1)))
                .toArray(String[]::new);
        LOG.warn("%s [%s] not set", unicode(0x26A0), paramName);
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
        final String scmProvider = SESSION.getParamPresent("scm.provider").orElse("scm:git");
        final String connectionUrl = GIT_SERVICE.getOriginUrl().orElseGet(() -> basedir.toURI().toString());
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
        return SEMANTIC_SERVICE.getBranchNameRefLog();
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
            LOG.info("%s Git tag [%s] already exists", unicode(0x1F3F7), newProjectVersion);
        } else {
            LOG.info("%s New git tag [%s]", unicode(0x1F3F7), newProjectVersion);
        }
    }

    private void printJavaDoc() {
        final File javaDocFile = new File(basedir, "target/apidocs/index.html");
        if (javaDocFile.exists()) {
            LOG.info("%s JavaDoc [file://%s]", unicode(0x1F516), javaDocFile.toURI().getRawPath());
        }
    }

    private String prepareProjectVersion() {
        final String projectVersion = SESSION.getParamPresent("project.version").orElse(null);
        final Optional<String> semanticFormat = SESSION.getParamPresent("semantic.format");
        LOG.debug("Versioning [project.version] " + (isEmpty(projectVersion) ? "not given" : "[" + projectVersion + "]"));
        LOG.debug("Versioning [semantic.format] " + (semanticFormat.isEmpty() ? "not given" : "[" + semanticFormat.get() + "]"));
        final String result = semanticFormat.isEmpty() ? projectVersion : SEMANTIC_SERVICE.getNextSemanticVersion(project.getVersion(), projectVersion);
        LOG.debug("Versioning [result.version] " + (isEmpty(result) ? "not found" : "[" + result + "]"));
        LOG.debug("Prepared project version [%s]", projectVersion);
        setWhen("branch.name.ref", SEMANTIC_SERVICE.getBranchNameRefLog().orElse(null));
        setWhen("branch.name", GIT_SERVICE.getBranchName().orElse(null));

        //ADD SNAPSHOT
        final String snapshotVersion = isEmpty(projectVersion) ? project.getVersion() : projectVersion;
        if ((isTrue("project.snapshot") || isTrue("deploy.snapshot")) && !snapshotVersion.endsWith("-SNAPSHOT")) {
            setWhen("snapshot.deployment", "true");
            return snapshotVersion + "-SNAPSHOT";
        }
        return result;
    }

    private void before() {
        LOG = new Logger(getLog());
        requireNonNull(pluginManager);

        MojoExecutor.setLogger(LOG);
        ENVIRONMENT = executionEnvironment(project, maven, pluginManager);
        SESSION = new PluginSession(ENVIRONMENT, LOG);

        GIT_SERVICE = new GitService(LOG, basedir, SESSION.getBoolean("fake").orElse(false));
        SEMANTIC_SERVICE = new SemanticService(SESSION, GIT_SERVICE, SESSION.getParamPresent("semantic.format").orElse(null));

        if (maven.getSettings().getServers() == null) {
            maven.getSettings().setServers(new ArrayList<>());
        }
        setWhen("base.dir", basedir.toString(), !hasText("base.dir"));
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
                SESSION.setParameter(key, value);
                break;
            }
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
