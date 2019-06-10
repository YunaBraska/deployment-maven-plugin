package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Compiler;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.Gpg;
import berlin.yuna.mavendeploy.config.JavaSource;
import berlin.yuna.mavendeploy.config.Javadoc;
import berlin.yuna.mavendeploy.config.MojoBase;
import berlin.yuna.mavendeploy.config.Resources;
import berlin.yuna.mavendeploy.config.Scm;
import berlin.yuna.mavendeploy.config.Surefire;
import berlin.yuna.mavendeploy.config.Versions;
import berlin.yuna.mavendeploy.model.Prop;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.String.format;
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

    private static final boolean DEBUG = true;
    static Model TEST_POM;
    private static Model PROJECT_POM;

    Terminal terminal;
    Terminal terminalNoLog;

    private final List<ActiveGoal> definedMojoList = asList(
            g(Clean.class, "clean"),
            g(Dependency.class, "resolve-plugins"),
            g(Dependency.class, "purge-local-repository"),
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
            g(Compiler.class, "testCompile")
    );

    @BeforeClass
    public static void setUpClass() {
        System.out.println(format("Start preparing [%s]", CustomMavenTestFramework.class.getSimpleName()));
        getTerminal().execute("mvn -Dmaven.test.skip=true install");
        System.out.println(format("End preparing [%s]", CustomMavenTestFramework.class.getSimpleName()));
    }

    @Before
    public void setUp() throws IOException, URISyntaxException {
        final Path tmpDir = prepareTestProject("testApplication");
        PROJECT_POM = getPomFile(new File(System.getProperty("user.dir"), "pom.xml"));
        TEST_POM = getPomFile(new File(tmpDir.toFile(), "pom.xml"));
        terminal = getTerminal().dir(tmpDir);
        terminalNoLog = getTerminalNoLog().dir(tmpDir);
        assertThat(format("Terminal does not point to test project [%s]", terminal.dir()),
                terminal.dir().getAbsolutePath().startsWith(System.getProperty("user.dir")), is(false));
        System.out.println(format("Work dir [%s]", tmpDir));
    }

    @After
    public void tearDown() throws IOException {
        deleteDir(TEST_POM.getPomFile().getParentFile().toPath());
    }

    void mergeBranch(final String branchName) {
        try {
            final Path filePath = Paths.get(TEST_POM.getPomFile().getParentFile().toString(), new File(branchName).getName());
            terminal.execute("git checkout -b " + branchName);
            Files.write(filePath, singletonList(branchName), StandardCharsets.UTF_8, StandardOpenOption.CREATE, APPEND);
            terminal.execute(format("git add .; git commit -a -m '%s'; git checkout master; git merge %s", branchName, branchName));
            //FIXME try more Git parameter - bug: multiple merges in the same second are ordered alphabetic - see 'git reflog show --all' maybe "--date=iso"?
            Thread.sleep(1000);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // TODO: generate plugins to pom file, update them and parse it on runtime
    String mvnCmd(final String parameter) {
        final String mvnCmd = "mvn"
                + " " + PROJECT_POM.getGroupId()
                + ":" + PROJECT_POM.getArtifactId()
                + ":" + PROJECT_POM.getVersion()
                + ":run -Dfake -X " + parameter
                + " -Djava.version=1.8 " + parameter;
        System.out.println(format("Running maven command [%s]", mvnCmd));
        return mvnCmd;
    }

    Model parse(final Model pom) {
        return getPomFile(pom.getPomFile());
    }

    Model getPomFile(final File pom) {
        assertThat("pom file [%s] does not exist", pom.exists(), is(true));
        assertThat("pom file [%s] is not a file", pom.isFile(), is(true));
        try {
            final Model pomModel = new MavenXpp3Reader().read(new FileReader(pom));
            pomModel.setPomFile(pom);
            return pomModel;
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("could not read pom.xml \n ", e);
        }
    }

    private static Terminal getTerminal() {
        return DEBUG ? getTerminalNoLog().consumerInfo(System.out::println) : getTerminalNoLog();
    }

    private static Terminal getTerminalNoLog() {
        return new Terminal().dir(System.getProperty("user.dir")).consumerError(System.err::println);
    }

    void expectMojoRun(final ActiveGoal... expectedMojos) {
        final String console = terminal.consoleInfo();
        assertThat(console, containsString("Building example-maven-test-project"));
        assertThat(console, not(containsString("Unable to invoke plugin")));
        final List<ActiveGoal> expectedMojoList = expectedMojos == null ? new ArrayList<>() : asList(expectedMojos);
        for (ActiveGoal definedMojo : definedMojoList) {
            if (expectedMojoList.contains(definedMojo)) {
                System.out.println("[INFO] Plugin expected: " + definedMojo.toString());
                assertThat(format("Mojo did not start [%s]", definedMojo), console, containsString("-<=[ Start " + definedMojo.toString()));
                assertThat(format("Mojo did not run [%s]", definedMojo), console, containsString("-<=[ End " + definedMojo.toString()));
            } else {
                assertThat(format("Mojo unexpectedly started [%s]", definedMojo), console, is(not(containsString("-<=[ Start " + definedMojo.toString()))));
            }
        }
    }

    void expectProperties(final Prop... configs) {
        final String consoleInfo = terminal.consoleInfo();
        for (Prop config : configs) {
            System.out.println(format("[INFO] Config expected key [%s] value [%s] ", config.key, config.value));
            assertThat(format("Config [%s] is dropped", config.key), consoleInfo, not(containsString(format("- Config key [%s] already set", config.key))));
            assertThat(format("Config [%s] is not set", config.key), consoleInfo, containsString(format("+ Config added key [%s]", config.key)));
            assertThat(format("Config [%s] has wrong value", config.key), consoleInfo, containsString(format("+ Config added key [%s] value [%s]", config.key, config.value)));
        }
    }

    void expectPropertiesOverwrite(final Prop... configs) {
        final String consoleInfo = terminal.consoleInfo();
        for (Prop config : configs) {
            System.out.println("[INFO] Config not expected: " + config.key);
            assertThat(format("Config [%s] is set but not overwritten", config.key), consoleInfo, not(containsString(format("+ Config added key [%s]", config.key))));
            assertThat(format("Config [%s] was not set at all", config.key), consoleInfo, containsString(format("- Config key [%s] already set with [%s]", config.key, config.value)));
        }
    }

    ActiveGoal g(final Class<? extends MojoBase> activeMojo, final String activeGoal) {
        return new ActiveGoal(activeMojo, activeGoal);
    }

    private Path prepareTestProject(final String testSource) throws IOException, URISyntaxException {
        final Path src = Paths.get(requireNonNull(getClass().getClassLoader().getResource(testSource)).toURI());
        assertThat(format("directory does not exists [%s]", src.toUri().toString()), Files.exists(src), is(true));
        assertThat(format("[%s] is not a directory", src.toUri().toString()), Files.isDirectory(src), is(true));
        final Path tempDirectory = Files.createTempDirectory(getClass().getSimpleName() + "_" + src.getFileName().toString() + "_");
        copyFolder(src, tempDirectory);
        getTerminal().dir(tempDirectory).execute("git init; git add .; git commit -a -m 'init'");
        return tempDirectory;
    }

    private void deleteDir(final Path dir) throws IOException {
        assertThat(format("directory does not exists [%s]", dir.toUri().toString()), Files.exists(dir), is(true));
        assertThat(format("[%s] is not a directory", dir.toUri().toString()), Files.isDirectory(dir), is(true));
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
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

    void setPackaging(final String packaging) {
        replaceInPom("<packaging>.*<\\/packaging>", "<packaging>" + packaging + "</packaging>");
    }

    void replaceInPom(final String regex, final String replacement) {
        try {
            final Path path = TEST_POM.getPomFile().toPath();
            final Charset charset = StandardCharsets.UTF_8;

            String content = new String(Files.readAllBytes(path), charset);
            content = content.replaceAll(regex, replacement);
            Files.write(path, content.getBytes(charset));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}