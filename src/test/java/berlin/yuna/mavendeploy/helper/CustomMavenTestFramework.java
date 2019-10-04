package berlin.yuna.mavendeploy.helper;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Compiler;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.Deploy;
import berlin.yuna.mavendeploy.config.Gpg;
import berlin.yuna.mavendeploy.config.Jar;
import berlin.yuna.mavendeploy.config.JavaSource;
import berlin.yuna.mavendeploy.config.Javadoc;
import berlin.yuna.mavendeploy.config.MojoBase;
import berlin.yuna.mavendeploy.config.PluginUpdater;
import berlin.yuna.mavendeploy.config.PropertyWriter;
import berlin.yuna.mavendeploy.config.ReadmeBuilder;
import berlin.yuna.mavendeploy.config.Resources;
import berlin.yuna.mavendeploy.config.Scm;
import berlin.yuna.mavendeploy.config.Surefire;
import berlin.yuna.mavendeploy.config.Versions;
import berlin.yuna.mavendeploy.logic.GitService;
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.model.Prop;
import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.reflections.Reflections;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static berlin.yuna.mavendeploy.config.Gpg.getGpgPath;
import static berlin.yuna.mavendeploy.helper.PluginUnitBase.createTestSession;
import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static berlin.yuna.mavendeploy.util.MojoUtil.deletePath;
import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class CustomMavenTestFramework {

    private static final String name = CustomMavenTestFramework.class.getSimpleName();
    protected static Model TEST_POM;
    protected static Model PROJECT_POM;
    protected static Path TEST_DIR;
    protected Terminal terminal;
    protected Terminal terminalNoLog;

    private static String TMP_DIR = System.getProperty("java.io.tmpdir");
    public static final String GPG_KEY_FILE = TMP_DIR + "/gpg.keyfile";
    public static final String GPG_SEC = TMP_DIR + "/gpg.sec";
    public static final String GPG_PUB = TMP_DIR + "/gpg.pub";
    private static final String DEBUG_ENV = System.getenv("DEBUG");
    public static final boolean DEBUG = isEmpty(DEBUG_ENV) || parseBoolean(DEBUG_ENV);

    protected GitService gitService;
    protected Logger log;

    private final List<ActiveGoal> definedMojoList = asList(
            g(Clean.class, "clean"),
            g(Dependency.class, "resolve-plugins"),
            g(Dependency.class, "purge-local-repository"),
            g(PluginUpdater.class, "update"),
            g(ReadmeBuilder.class, "render"),
            g(PropertyWriter.class, "write"),
            g(Versions.class, "update-parent"),
            g(Versions.class, "update-properties"),
            g(Versions.class, "update-child-modules"),
            g(Versions.class, "use-latest-releases"),
            g(Versions.class, "use-next-snapshots"),
            g(Versions.class, "use-latest-versions"),
            g(Versions.class, "commit"),
            g(Versions.class, "set"),
            g(Javadoc.class, "jar"),
            g(JavaSource.class, "jar-no-fork"),
            g(Gpg.class, "sign"),
            g(Scm.class, "tag"),
            g(Surefire.class, "test"),
            g(Resources.class, "resources"),
            g(Resources.class, "testResources"),
            g(Compiler.class, "compile"),
            g(Compiler.class, "testCompile"),
            g(Jar.class, "jar"),
            g(Deploy.class, "deploy")
    );

    @BeforeClass
    public static void setUpClass() {
        final PluginSession testSession = createTestSession();
        testSession.getLog().debug("Start preparing [%s]", name);
        getTerminal(testSession.getLog()).execute("mvn -Dmaven.test.skip=true install --quiet");
        testSession.getLog().debug("End preparing [%s]", name);
    }

    @Before
    public void setUp() throws IOException, URISyntaxException, GitAPIException {
        log = createTestSession().getLog();
        TEST_DIR = prepareTestProject();
        PROJECT_POM = getPomFile(new File(System.getProperty("user.dir"), "pom.xml"));
        TEST_POM = getPomFile(new File(TEST_DIR.toFile(), "pom.xml"));
        terminal = getTerminal(log).dir(TEST_DIR);
        terminalNoLog = getTerminalNoLog(log).dir(TEST_DIR);
        gitService = new GitService(log, TEST_DIR.toFile(), true);
        assertThat(
                format("Terminal does not point to test project [%s]", terminal.dir()),
                terminal.dir().getAbsolutePath().startsWith(System.getProperty("user.dir")),
                is(false)
        );
        log.debug("Work dir [%s]", TEST_DIR);
    }

    @After
    public void tearDown() {
        deleteDir(TEST_DIR);
    }

    public static Path getPath(final Class clazz) {
        final String classPath = clazz.getTypeName().replace(".", "/") + ".java";
        return new File(new File(System.getProperty("user.dir"), "src/main/java"), classPath).toPath();
    }

    protected void mergeBranch(final String branchName) {
        try {
            final Path filePath = Paths.get(TEST_DIR.toString(), new File(branchName).getName());
            terminal.execute("git checkout -b " + branchName);
            Files.write(filePath, singletonList(branchName), StandardCharsets.UTF_8, StandardOpenOption.CREATE, APPEND);
            terminal.execute(format("git add .; git commit -a -m '%s'; git checkout master; git merge %s", branchName, branchName));
            //FIXME try more Git parameter - bug: multiple merges in the same second are ordered alphabetic - see 'git reflog show --all' maybe "--date=iso"?
            Thread.sleep(1000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String mvnCmd(final String parameter) {
        final String mvnCmd = "mvn"
                + " " + PROJECT_POM.getGroupId()
                + ":" + PROJECT_POM.getArtifactId()
                + ":" + PROJECT_POM.getVersion()
                + ":run -Dfake -X "
                + parameter;
        log.debug("Running maven command [%s]", mvnCmd.trim());
        return mvnCmd;
    }

    protected Model parse(final Model pom) {
        return getPomFile(pom.getPomFile());
    }

    protected String getCurrentProjectVersion() {
        return parse(TEST_POM).getVersion();
    }

    protected String getCurrentGitTag() {
        return gitService.getLastGitTag();
    }

    public static Model getPomFile(final File pom) {
        assertThat("pom file [%s] does not exist", pom.exists(), is(true));
        assertThat("pom file [%s] is not a file", pom.isFile(), is(true));
        try {
            final Model pomModel = new MavenXpp3Reader().read(new ByteArrayInputStream(readAllBytes(pom.toPath())));
            pomModel.setPomFile(pom);
            return pomModel;
        } catch (Exception e) {
            throw new RuntimeException("could not read pom.xml \n ", e);
        }
    }

    private static Terminal getTerminal(final Logger log) {
        return DEBUG ? getTerminalNoLog(log).consumerInfo(log::info) : getTerminalNoLog(log);
    }

    private static Terminal getTerminalNoLog(final Logger log) {
        return new Terminal().dir(System.getProperty("user.dir")).consumerError(log::error);
    }

    protected void expectMojoRun(final ActiveGoal... expectedMojos) {
        expectMojoRun(false, expectedMojos);
    }

    protected void expectMojoRun(final boolean brokenMojo, final ActiveGoal... expectedMojos) {
        final String console = terminal.consoleInfo() + terminal.consoleError();
        assertThat(console, containsString("Building example-maven-test-project"));
        assertThat(console, not(containsString("Unable to invoke plugin")));
        final List<ActiveGoal> expectedMojoList = expectedMojos == null ? new ArrayList<>() : asList(expectedMojos);
        for (ActiveGoal definedMojo : definedMojoList) {
            if (expectedMojoList.contains(definedMojo)) {
                log.debug("Plugin expected: " + definedMojo.toString());
                assertThat(format("Mojo did not start [%s]", definedMojo), console, containsString("-<=[ Start " + definedMojo.toString()));
                if (!brokenMojo) {
                    assertThat(format("Mojo did not run [%s]", definedMojo), console, containsString("-<=[ End " + definedMojo.toString()));
                }
            } else {
                assertThat(format("Mojo unexpectedly started [%s]", definedMojo), console, is(not(containsString("-<=[ Start " + definedMojo.toString()))));
            }
        }
    }

    protected void expectProperties(final Prop... configs) {
        final String consoleInfo = terminal.consoleInfo();
        for (Prop config : configs) {
            log.info("Config expected key [%s] value [%s] ", config.key, config.value);
            assertThat(format("Config [%s] is dropped", config.key), consoleInfo, not(containsString(format("Config key [%s] already set", config.key))));
            assertThat(format("Config [%s] is not set", config.key), consoleInfo, containsString(format("Config added key [%s]", config.key)));
            assertThat(format("Config [%s] has wrong value", config.key), consoleInfo, containsString(format("Config added key [%s] value [%s]", config.key, config.value)));
        }
    }

    protected void expectPropertiesOverwrite(final Prop... configs) {
        final String consoleInfo = terminal.consoleInfo();
        for (Prop config : configs) {
            log.info("Config not expected: " + config.key);
            assertThat(format("Config [%s] is set but not overwritten", config.key), consoleInfo, not(containsString(format("Config added key [%s]", config.key))));
            assertThat(format("Config [%s] was not set at all", config.key), consoleInfo, containsString(format("Config key [%s] already set with [%s]", config.key, config.value)));
        }
    }

    protected ActiveGoal g(final Class<? extends MojoBase> activeMojo, final String activeGoal) {
        return new ActiveGoal(activeMojo, activeGoal);
    }

    protected void gitInit(final Path tempDirectory) throws GitAPIException {
        final Git git = Git.init().setDirectory(tempDirectory.toFile()).call();
        git.add().addFilepattern(".").call();
        git.commit().setMessage("init").call();
    }

    private Path prepareTestProject() throws IOException, URISyntaxException, GitAPIException {
        final Path src = Paths.get(requireNonNull(getClass().getClassLoader().getResource("testApplication")).toURI());
        deletePath(Paths.get(src.toString(), "target"));
        deletePath(Paths.get(src.toString().replace("target/test-classes", "src/test/resources"), "target"));
        assertThat(format("directory does not exists [%s]", src.toUri().toString()), Files.exists(src), is(true));
        assertThat(format("[%s] is not a directory", src.toUri().toString()), Files.isDirectory(src), is(true));
        final Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName() + "_" + src.getFileName().toString() + "_");
        copyFolder(src, tempDirectory);
        gitInit(tempDirectory);
        return tempDirectory;
    }

    private void deleteDirIfExists(final Path dir) {
        if (Files.exists(dir)) {
            log.info("%s Deleting [%s]", unicode(0x1F4CD), dir.toString());
            deleteDir(dir);
        }
    }

    private void deleteDir(final Path dir) {
        assertThat(format("directory does not exists [%s]", dir.toUri().toString()), Files.exists(dir), is(true));
        assertThat(format("[%s] is not a directory", dir.toUri().toString()), Files.isDirectory(dir), is(true));
        deletePath(dir);
        assertThat(format("Unable to delete [%s]", dir.toString()), Files.exists(dir), is(false));
    }

    private void copyFolder(final Path src, final Path dest) throws IOException {
        Files.walk(src)
                .forEach(source -> copyFile(source, dest.resolve(src.relativize(source))));
    }

    private void copyFile(final Path source, final Path dest) {
        try {
            Files.copy(source, dest, REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected void setPackaging(final String packaging) {
        replaceInPom("<packaging>.*<\\/packaging>", "<packaging>" + packaging + "</packaging>");
    }

    void replaceInPom(final String regex, final String replacement) {
        try {
            final Path path = TEST_POM.getPomFile().toPath();
            final Charset charset = StandardCharsets.UTF_8;

            String content = Files.readString(path, charset);
            content = content.replaceAll(regex, replacement);
            Files.writeString(path, content, charset);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static List<MojoBase> getAllMojos() {
        final List<MojoBase> mojoList = new ArrayList<>();
        try {
            final Reflections reflections = new Reflections(MojoBase.class.getPackage().getName());
            final Set<Class<? extends MojoBase>> classes = reflections.getSubTypesOf(MojoBase.class);

            for (Class<? extends MojoBase> mojo : classes) {
                mojoList.add(mojo.getDeclaredConstructor(PluginSession.class).newInstance(new PluginSession(null)));
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return mojoList;
    }

    protected static void setupGpgTestKey(final Terminal terminal, final Logger log) {
        final String gpgCmd = getGpgPath(log);
        terminal.execute(
                "cat > " + GPG_KEY_FILE + " <<EOF\n"
                        + "     %echo Generating a basic OpenPGP key\n"
                        + "     Key-Type: RSA\n"
                        + "     Key-Length: 2048\n"
                        + "     Subkey-Type: ELG-E\n"
                        + "     Subkey-Length: 1024\n"
                        + "     Name-Real: gpgTestName\n"
                        + "     Name-Comment: keep it simple\n"
                        + "     Name-Email: mail@example.com\n"
                        + "     Expire-Date: 0\n"
                        + "     Passphrase: mySecret\n"
                        + "     %pubring "
                        + GPG_PUB + "\n"
                        + "     %secring "
                        + GPG_SEC + "\n"
                        + "     %key created\n"
                        + "     %echo done\n"
                        + "EOF"
        );
        sleep();
        terminal.execute(gpgCmd + " --batch --gen-key " + GPG_KEY_FILE);
        sleep();
        terminal.execute(gpgCmd + " --import " + GPG_PUB);
        sleep();
        terminal.execute(gpgCmd + " --list-keys");
    }

    protected static void teardownGpgTestKey(final Terminal terminal, final Logger log) {
        final String gpgCmd = getGpgPath(log);
        terminal.execute(gpgCmd + " --batch --yes --delete-secret-keys $(" + gpgCmd + " --no-default-keyring --secret-keyring " + GPG_SEC + " --keyring " + GPG_PUB + " --list-secret-keys | grep \"      \" | head -n1 | xargs)");
        terminal.execute(gpgCmd + " --batch --yes --delete-keys $(" + gpgCmd + " --no-default-keyring --secret-keyring " + GPG_SEC + " --keyring " + GPG_PUB + " --list-keys | grep \"      \" | head -n1 | xargs)");
        terminal.execute(gpgCmd + " --list-keys");
        terminal.execute("rm -rf " + GPG_PUB + " " + GPG_SEC + " " + GPG_KEY_FILE);
    }

    private static void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}