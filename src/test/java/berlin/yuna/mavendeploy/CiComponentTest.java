package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.Terminal;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;

import static java.util.Collections.singletonList;

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
        new Terminal().dir(WORK_DIR).execute("rm -f " + artifactFile.getAbsolutePath());
    }

    @Test
    public void pluginMojo() {
        final PluginMojo pluginMojo = new PluginMojo();
        pluginMojo.setBasedir(WORK_DIR);
        pluginMojo.setArgs(singletonList(prepareArgs()));
        pluginMojo.execute();
    }

    private String prepareArgs() {
        return " --PROJECT_DIR=" + WORK_DIR
                + " --MVN_PROFILES=false"
                + " --MVN_CLEAN=true"
                + " --MVN_CLEAN_CACHE=false"
                + " --MVN_SKIP_TEST=true"
                + " --MVN_UPDATE=false"
                + " --MVN_JAVA_DOC=false"
                + " --MVN_SOURCE=false"
                + " --MVN_TAG=false"
                + " --MVN_TAG_BREAK=false";
    }

    private void buildJar() {
        new Terminal().dir(WORK_DIR).execute("mvn clean package -Dmaven.test.skip=true");
    }
}