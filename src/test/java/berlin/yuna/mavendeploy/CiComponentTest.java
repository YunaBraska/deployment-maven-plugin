package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.Terminal;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CiComponentTest {

    private static final File WORK_DIR = new File(System.getProperty("user.dir"));
    private static final Terminal terminal = new Terminal().dir(WORK_DIR).timeoutMs(30000);
    private Model pomXml;

    @Before
    public void setUp() throws Exception {
        buildJar();
        pomXml = new MavenXpp3Reader().read(new FileReader(new File(WORK_DIR, "pom.xml")));
    }

    @Test
    public void runExecutableJar_shouldBeSuccessful() {
        final File artifactFile = new File(WORK_DIR, pomXml.getArtifactId() + "." + pomXml.getPackaging());
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


        final String consoleInfo = terminal
                .consumerInfo(System.out::println)
                .consumerError(System.err::println)
                .execute("java -jar " + artifactFile.getName() + args)
                .consoleInfo();
        assertThat(consoleInfo.trim(), is(equalTo("mvn verify -Dmaven.test.skip=true")));
        terminal.execute("rm -f " + artifactFile.getAbsolutePath());
    }

    private void buildJar() {
        terminal.execute("mvn clean package -Dmaven.test.skip=true");
    }
}