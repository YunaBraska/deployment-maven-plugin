package berlin.yuna.mavendeploy.plugin;

import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.Deploy;
import berlin.yuna.mavendeploy.config.Gpg;
import berlin.yuna.mavendeploy.config.Jar;
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
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.model.ThrowingFunction;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;

import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readDeveloperProperties;
import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readLicenseProperties;
import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readModuleProperties;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.executionEnvironment;
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
public class Application extends AbstractMojo {

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
    private PluginExecutor.ExecutionEnvironment ENVIRONMENT;
    private PluginSession SESSION;

    public Application() {}

    public void execute() {
        before();
        LOG.info("%s Preparing information [%s:%s]", unicode(0x1F453), plugin.getArtifactId(), plugin.getVersion());
        SESSION.setNewParam("maven.test.skip", SESSION.getBoolean("test.skip").orElse(false).toString());
        final String newProjectVersion = prepareProjectVersion();
        try {
            final boolean isLibrary = isLibrary();
            final String newTag = prepareNewTagVersion(newProjectVersion);
            final boolean hasNewTag = hasNewTag(newTag, GIT_SERVICE.getLastGitTag());

            LOG.info("%s STEP [1/6] SETUP MOJO PROPERTIES", unicode(0x1F4DD));
            //SET GIT PROPERTIES
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
            SESSION.setParameter("tag", newTag, !isEmpty(newTag));
            setWhen("gpg.passphrase", SESSION.getParamPresent("gpg.pass", "gpg.passphrase").orElse(null));
            setWhen("passphraseServerId", SESSION.getParamPresent("gpg.passphrase").orElse(null));

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
            runWhen(() -> Javadoc.build(SESSION).jar(), (!isLibrary() && isTrue("java.doc")));
            runWhen(() -> JavaSource.build(SESSION).jarNoFork(), (!isLibrary() && isTrue("java.source")));
            runWhen(() -> Jar.build(SESSION).jar(), hasText("package") || isTrue("deploy", "deploy.snapshot"));
            runWhen(() -> Gpg.build(SESSION).sign(), hasText("gpg.passphrase"));
            runWhen(() -> Scm.build(SESSION).tag(), hasNewTag);
            runWhen(() -> Deploy.build(SESSION).deploy(), isTrue("deploy", "deploy.snapshot"));

            //TODO: implement to push on changes && new parameter change version only on changes version.onchange && tag.onchange
//                if (GIT_SERVICE.gitHasChanges() && SESSION.getBoolean("changes.push").orElse(false)) {
//                    final String message = SESSION.getParamPresent("message").orElse(prepareCommitMessage(newProjectVersion, hasNewTag, isTrue("update.plugins", "update.minor", "update.major")));
//                    setWhen("message", message);
//                    GIT_SERVICE.push();
//                }


        } catch (
                Exception e) {
            throw new RuntimeException(e);
        } finally {
            cleanUps(newProjectVersion);
        }

    }

    private void cleanUps(final String newProjectVersion) {
        LOG.info("%s STEP [6/6] RUN PLUGINS WITH CLEANUPS", unicode(0x1F9E7));
        //remove snapshot if only added for deployment
        final boolean removeSnapshot = SESSION.getBoolean("snapshot.deployment").orElse(false);
        if (removeSnapshot) {
            SESSION.setParameter("oldVersion", newProjectVersion, true);
            SESSION.setParameter("newVersion", newProjectVersion.split("-SNAPSHOT")[0], true);
        }
        try {
            runWhen(() -> Versions.build(SESSION).set(), removeSnapshot);
            runWhen(() -> PropertyWriter.build(SESSION).write(), isTrue("properties.print") || hasText("properties.print"));
        } catch (Exception e) {
            LOG.error(e);
        }
        printJavaDoc();
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
        final String snapshotVersion = isEmpty(result) ? project.getVersion() : result;
        if ((isTrue("project.snapshot") || isTrue("deploy.snapshot")) && !snapshotVersion.endsWith("-SNAPSHOT")) {
            setWhen("snapshot.deployment", "true");
            return snapshotVersion + "-SNAPSHOT";
        }
        return result;
    }

    private void before() {
        LOG = new Logger(getLog());
        requireNonNull(pluginManager);

        PluginExecutor.setLogger(LOG);
        ENVIRONMENT = executionEnvironment(project, maven, pluginManager);
        SESSION = new PluginSession(ENVIRONMENT, LOG);

        GIT_SERVICE = new GitService(LOG, basedir, SESSION.getBoolean("fake").orElse(false));
        SEMANTIC_SERVICE = new SemanticService(SESSION, GIT_SERVICE, SESSION.getParamPresent("semantic.format").orElse(null));

        if (maven.getSettings().getServers() == null) {
            maven.getSettings().setServers(new ArrayList<>());
        }

        final MavenSession mavenSession = SESSION.getMavenSession();
        mavenSession.getUserProperties().putAll(SESSION.getProperties());
        mavenSession.getUserProperties().putAll(readModuleProperties(project.getModules()));
        mavenSession.getUserProperties().putAll(readLicenseProperties(project.getLicenses()));
        mavenSession.getUserProperties().putAll(readDeveloperProperties(project.getDevelopers()));
        mavenSession.getUserProperties().put("project.name", project.getName());
        mavenSession.getUserProperties().put("project.groupId", project.getGroupId());
        mavenSession.getUserProperties().put("project.artifactId", project.getArtifactId());
        mavenSession.getUserProperties().put("project.packaging", project.getPackaging());
        mavenSession.getUserProperties().put("project.description", project.getDescription());
        mavenSession.getUserProperties().put("project.url", project.getUrl());
        mavenSession.getUserProperties().put("project.id", project.getId());
        mavenSession.getUserProperties().put("project.defaultGoal", project.getDefaultGoal());
        mavenSession.getUserProperties().put("project.inceptionYear", project.getInceptionYear());
        mavenSession.getUserProperties().put("project.modelVersion", project.getModelVersion());
        GIT_SERVICE.getConfig().forEach((key, value) -> setWhen("git." + key, value));



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
                SESSION.setNewParam(key, value);
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
