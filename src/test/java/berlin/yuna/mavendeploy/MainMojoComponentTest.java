package berlin.yuna.mavendeploy;

import berlin.yuna.mavendeploy.plugin.MojoRun;
import org.junit.Test;

import java.io.File;

import static java.util.Collections.singletonList;

public class MainMojoComponentTest {

    private static final File WORK_DIR = new File(System.getProperty("user.dir"));

    @Test
    public void pluginMojo() {
        final MojoRun mainMojo = new MojoRun();
        mainMojo.setBasedir(WORK_DIR);
        mainMojo.setArgs(singletonList(prepareArgs()));
        mainMojo.execute();
    }

    private String prepareArgs() {
        return " --PROJECT_DIR=" + WORK_DIR
                + " --PROFILES=false"
                + " --CLEAN=true"
                + " --JAVA_DOC=false"
                + " --SOURCE=false";
    }
}