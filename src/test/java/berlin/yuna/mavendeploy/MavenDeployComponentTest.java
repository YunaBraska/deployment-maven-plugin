package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.clu.logic.Terminal;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MavenDeployComponentTest {

    static final Logger LOG = LoggerFactory.getLogger(Terminal.class);
    final File buildFile = new File(Paths.get("target").toAbsolutePath().getParent().toFile(), "ci.bash");
    Terminal terminal;

    @Before
    public void setUp() {
        assertThat(buildFile.exists(), is(true));
        terminal = new Terminal()
                .dir(prepareTestProject())
                .consumerInfo(System.out::println)
                .consumerError(System.err::println);
    }

    @Ignore
    @Test
    public void ciBuildTest() {
        final String command = buildFile.getAbsolutePath()
                + " --PROJECT_VERSION=3.2.1.2.3"
                + " --JAVA_VERSION=1.8"
                + " --ENCODING=UTF-8"
                + " --MVN_PROFILES=true"
                + " --MVN_CLEAN=true"
                + " --MVN_UPDATE=true"
                + " --MVN_JAVA_DOC=true"
                + " --MVN_SOURCE=true"
                + " --MVN_RELEASE=false"
                + " --MVN_TAG=true";

        LOG.debug("Project dir [{}]", terminal.dir());
        LOG.debug("Build Command [{}]", command);

        final String console = terminal.execute(command).consoleInfo();
        assertConsoleOutput(console);
        assertThat(terminal.status(), is(0));


        final File pomFile = new File(terminal.dir(), "pom.xml");
        final File targetDir = new File(terminal.dir(), "target");
        assertThat(pomFile.exists(), is(true));
        assertThat(targetDir.exists(), is(true));
        assertThat(SystemUtil.readFile(pomFile.toPath()), containsString("<version>3.2.1.2.3</version>"));

        final List<String> targetFiles = Arrays.asList(requireNonNull(targetDir.list()));
        assertThat(targetFiles.stream().filter(s -> s.endsWith("javadoc.jar")).count(), is(1L));
        assertThat(targetFiles.stream().filter(s -> s.endsWith("sources.jar")).count(), is(1L));
    }

    private void assertConsoleOutput(final String console) {
        assertThat(console, containsString("PROJECT_VERSION [3.2.1.2.3]"));
        assertThat(console, containsString("JAVA_VERSION [1.8]"));
        assertThat(console, containsString("ENCODING [UTF-8]"));
        assertThat(console, containsString("IS_POM [false]"));
        assertThat(console, containsString("MVN_PROFILES []"));
        assertThat(console, containsString("MVN_CLEAN [true]"));
        assertThat(console, containsString("MVN_UPDATE [true]"));
        assertThat(console, containsString("MVN_JAVA_DOC [true]"));
        assertThat(console, containsString("MVN_SOURCE [true]"));
        assertThat(console, containsString("[INFO] Scanning for projects"));
        assertThat(console, containsString("[INFO] Building"));
        assertThat(console, containsString("Downloaded from central"));
        assertThat(console, containsString("maven-clean-plugin"));
        assertThat(console, containsString("T E S T S"));
        assertThat(console, containsString("maven-jar-plugin"));
        assertThat(console, containsString("[INFO] Building"));
        assertThat(console, containsString("versions-maven-plugin"));
        assertThat(console, containsString("update-parent"));
        assertThat(console, containsString("update-properties"));
        assertThat(console, containsString("update-child-modules"));
        assertThat(console, containsString("use-latest-releases"));
        assertThat(console, containsString("use-next-snapshots"));
        assertThat(console, containsString("[WARN] Tagging failed cause PROJECT_VERSION [3.2.1.2.3] is not set or the GIT_TAG [3.2.1.2.3] already exists"));
        assertThat(console, containsString("[INFO] BUILD SUCCESS"));
    }

    private File prepareTestProject() {
        final File testDir = new File(System.getProperty("java.io.tmpdir"), getClass().getSimpleName());
        if (!testDir.exists()) {
            assertThat(testDir.mkdirs(), is(true));
            assertThat(testDir.exists(), is(true));
            new Terminal()
                    .dir(testDir)
                    .consumerInfo(System.out::println)
                    .consumerError(System.err::println)
                    .execute("git clone https://github.com/YunaBraska/command-line-util");
            assertThat(testDir.list(), is(notNullValue()));
            assertThat(requireNonNull(testDir.list()).length, is(not(0)));
        }
        return new File(testDir, "command-line-util");
    }
}
