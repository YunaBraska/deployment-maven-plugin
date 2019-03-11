package berlin.yuna.mavendeploy;

import org.junit.Test;

import java.io.File;

public class CiComponentTest {


    @Test
    public void prepareMavenCommand() {
        final String command = "PROJECT_DIR=" + new File(System.getProperty("user.dir"))
                + " --PROJECT_VERSION=3.2.1.2.3"
                + " --JAVA_VERSION=1.8"
                + " --ENCODING=UTF-8"
                + " --MVN_PROFILES=true"
                + " --MVN_CLEAN=true"
                + " --MVN_UPDATE=true"
                + " --MVN_JAVA_DOC=true"
                + " --MVN_SOURCE=true"
                + " --MVN_RELEASE=false"
                + " --MVN_TAG=true"
                + " --GPG_PASSPHRASE=7576Simba";
//                + " --MVN_DEPLOY_ID=myserver";
        final String mavenCommand = new Ci(command).prepareMavenCommand();
        System.out.println(mavenCommand);
        new Ci(command).run();
    }
}