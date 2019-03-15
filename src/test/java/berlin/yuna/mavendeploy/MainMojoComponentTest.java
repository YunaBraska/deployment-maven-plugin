package berlin.yuna.mavendeploy;

import berlin.yuna.mavendeploy.plugin.MainMojo;
import org.junit.Test;

import java.io.File;

import static java.util.Collections.singletonList;

public class MainMojoComponentTest {

    private static final File WORK_DIR = new File(System.getProperty("user.dir"));

    @Test
    public void pluginMojo() {
        final MainMojo mainMojo = new MainMojo();
        mainMojo.setBasedir(WORK_DIR);
        mainMojo.setArgs(singletonList(prepareArgs()));
        mainMojo.execute();
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
}