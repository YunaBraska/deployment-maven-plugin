package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.Terminal;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CiComponentTest {

    private static final File WORK_DIR = new File(System.getProperty("user.dir"));
    private Model pomXml;

    @Before
    public void setUp() throws Exception {
        buildJar();
        pomXml = new MavenXpp3Reader().read(new FileReader(new File(WORK_DIR, "pom.xml")));
    }

    @After
    public void tearDown() {
        final File artifactFile = new File(WORK_DIR, pomXml.getArtifactId() + "." + pomXml.getPackaging());
        new Terminal(CiComponentTest.class).dir(WORK_DIR).execute("rm -f " + artifactFile.getAbsolutePath());
    }

    @Test
    public void runExecutableJar_shouldBeSuccessful() {

        final String args = " --PROJECT_DIR=" + WORK_DIR
                + " --MVN_PROFILES=false"
                + " --MVN_CLEAN=false"
                + " --MVN_CLEAN_CACHE=false"
                + " --MVN_SKIP_TEST=true"
                + " --MVN_UPDATE=false"
                + " --MVN_JAVA_DOC=false"
                + " --MVN_SOURCE=false"
                + " --MVN_TAG=false"
                + " --MVN_TAG_BREAK=false";

        final String consoleInfo = new Terminal(CiComponentTest.class).dir(WORK_DIR)
                .timeoutMs(30000).execute("./ci.sh " + args)
                .consoleInfo();
        assertThat(consoleInfo.trim(), is(equalTo("mvn verify -Dmaven.test.skip=true")));
    }

    private void buildJar() {
        new Terminal(CiTest.class).dir(WORK_DIR).execute("mvn clean package -Dmaven.test.skip=true");
    }
}