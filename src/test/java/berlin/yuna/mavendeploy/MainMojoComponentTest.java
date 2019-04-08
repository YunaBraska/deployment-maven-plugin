package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.MojoBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

public class MainMojoComponentTest {

    private static final File WORK_DIR = new File(System.getProperty("user.dir"));
    private Terminal terminal;
    private final List<ActiveGoal> definedMojoList = asList(
            g(Clean.class, "clean"),
            g(Dependency.class, "resolve-plugins"),
            g(Dependency.class, "purge-local-repository")
    );


    @BeforeClass
    public static void setUpClass() {
        System.out.println(format("Start preparing [%s]", MainMojoComponentTest.class.getSimpleName()));
        getTerminal().execute("mvn -Dmaven.test.skip=true install");
        System.out.println(format("End preparing [%s]", MainMojoComponentTest.class.getSimpleName()));
    }

    @Before
    public void setUp() {
        terminal = getTerminal();
    }

    @Test
    public void mojoClear_WithTrigger_shouldExecute() {
        terminal.execute(mvnCmd("-Dclean"));
        expectMojoRun(g(Clean.class, "clean"), g(Dependency.class, "resolve-plugins"));
    }


    @Test
    public void mojoClear_WithOutTrigger_shouldExecute() {
        terminal.execute(mvnCmd("-Dclean=false"));
        expectMojoRun();
    }

    @Test
    public void mojoClearCache_WithTrigger_shouldExecuteClearAndClearCache() {
        terminal.execute(mvnCmd("-Dclean.cache"));
        expectMojoRun(g(Clean.class, "clean"), g(Dependency.class, "resolve-plugins"), g(Dependency.class, "purge-local-repository"));
    }

    @Test
    public void mojoClear_WithOutTrigger_shouldExecuteClearAndClearCache() {
        terminal.execute(mvnCmd("-Dclean.cache=false"));
        expectMojoRun();
    }

    @Test
    public void settingsBuilder_withServer_shouldAddServerToSession() {
        terminal.execute(mvnCmd("-Dsettings.xml=--Server=\"myNewServer\""));
        assertThat(terminal.consoleInfo(), containsString("+ [Settings] adding [Server] [myNewServer]"));
    }


    private String mvnCmd(final String parameter) {
        final Model pom = getPomFile();
        final String mvnCmd = "mvn"
                + " --offline --file=" + getClass().getClassLoader().getResource("testApplication/pom.xml").getFile()
                + " " + pom.getGroupId()
                + ":" + pom.getArtifactId()
                + ":" + pom.getVersion()
                + ":run -Dfake -X " + parameter;
        System.out.println(format("Running maven command [%s]", mvnCmd));
        return mvnCmd;
    }

    private Model getPomFile() {
        try {
            return new MavenXpp3Reader().read(new FileReader(new File(WORK_DIR, "pom.xml")));
        } catch (IOException | XmlPullParserException e) {
            throw new RuntimeException("could not read pom.xml \n ", e);
        }
    }

    private static Terminal getTerminal() {
        return new Terminal().dir(WORK_DIR).consumerError(System.err::println);
    }

    private void expectMojoRun(final ActiveGoal... expectedMojos) {
        final String console = terminal.consoleInfo();
        assertThat(console, containsString("Building example-maven-project"));
        final List<ActiveGoal> expectedMojoList = expectedMojos == null ? new ArrayList<>() : asList(expectedMojos);
        for (ActiveGoal definedMojo : definedMojoList) {
            if (expectedMojoList.contains(definedMojo)) {
                System.out.println("Expected: " + definedMojo.toString());
                assertThat(format("Mojo did not start [%s]", definedMojo), console, containsString("-<=[ Start " + definedMojo.toString()));
                assertThat(format("Mojo did not run [%s]", definedMojo), console, containsString("-<=[ End " + definedMojo.toString()));
            } else {
                System.out.println("Not Expected: " + definedMojo.toString());
                assertThat(format("Mojo unexpectedly start [%s]", definedMojo), console, is(not(containsString("-<=[ Start " + definedMojo.toString()))));
                assertThat(format("Mojo unexpectedly run [%s]", definedMojo), console, is(not(containsString("-<=[ End " + definedMojo.toString()))));
            }
        }
    }

    private ActiveGoal g(final Class<? extends MojoBase> activeMojo, final String activeGoal) {
        return new ActiveGoal(activeMojo, activeGoal);
    }
}