package berlin.yuna.mavendeploy;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class CiComponentTest {


    @Test
    public void prepareMavenCommand_shouldBeSuccessful() {
        final String command = "PROJECT_DIR=" + new File(System.getProperty("user.dir"))
                + " --PROJECT_VERSION=3.2.1.2.3"
                + " --JAVA_VERSION=1.8"
                + " --ENCODING=UTF-8"
                + " --MVN_PROFILES=true"
                + " --MVN_CLEAN=true"
                + " --MVN_UPDATE=true"
                + " --MVN_JAVA_DOC=true"
                + " --MVN_SOURCE=true"
                + " --MVN_TAG=true"
                + " --MVN_TAG_BREAK=true"
                + " --GPG_PASSPHRASE=XXX"
                + " --MVN_DEPLOY_ID=nexus"
                + " --PROJECT_DIR=/Users/yunamorgenstern/Documents/projects/system-util";
//                + " --MVN_DEPLOY_ID=myserver";
        final String mavenCommand = new Ci(command).prepareMavenCommand();
        System.out.println(mavenCommand);
        //FIXME compare with MavenCommands.java
        assertThat(mavenCommand, containsString("dependency:purge-local-repository"));
        assertThat(mavenCommand, containsString("clean"));
        assertThat(mavenCommand, containsString("deploy"));
        assertThat(mavenCommand, containsString("generate-sources"));
        assertThat(mavenCommand, containsString("generate-resources"));
        assertThat(mavenCommand, containsString("dependency:resolve-plugins"));
        assertThat(mavenCommand, containsString("versions:update-parent"));
        assertThat(mavenCommand, containsString("versions:update-properties"));
        assertThat(mavenCommand, containsString("versions:update-child-modules"));
        assertThat(mavenCommand, containsString("versions:use-latest-releases"));
        assertThat(mavenCommand, containsString("versions:use-next-snapshots"));
        assertThat(mavenCommand, containsString("versions:commit"));
        assertThat(mavenCommand, containsString("-DallowSnapshots=true"));
        assertThat(mavenCommand, containsString("-DgenerateBackupPoms=false"));
        assertThat(mavenCommand, containsString("javadoc:jar"));
        assertThat(mavenCommand, containsString("source:jar-no-fork"));
        assertThat(mavenCommand, containsString("scm:tag -Dtag="));
        assertThat(mavenCommand, containsString("berlin.yuna:maven-gpg-plugin:sign"));
        assertThat(mavenCommand, containsString("-DserverId"));
        assertThat(mavenCommand, containsString("-Dproject.encoding"));
        assertThat(mavenCommand, containsString("-Dmaven.compiler.source"));
    }

    @Test
    public void run_shouldBeSuccessful() {
        final String command = "PROJECT_DIR=" + new File(System.getProperty("user.dir"))
                + " --PROJECT_VERSION=3.2.1.2.3"
                + " --JAVA_VERSION=1.8"
                + " --ENCODING=UTF-8"
                + " --MVN_PROFILES=false"
                + " --MVN_CLEAN=false"
                + " --MVN_UPDATE=false"
                + " --MVN_JAVA_DOC=false"
                + " --MVN_SOURCE=false"
                + " --MVN_TAG_BREAK=false"
                + " --MVN_RELEASE=false"
                + " --MVN_TAG=false"
                + " --PROJECT_DIR=/Users/yunamorgenstern/Documents/projects/system-util";
//                + " --MVN_DEPLOY_ID=myserver";
        new Ci(command).run();
    }
}