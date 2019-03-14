package berlin.yuna.mavendeploy;

import org.junit.Test;

import java.io.File;

import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_CLEAN;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_CLEAN_CACHE;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_FAILSAFE_XX;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_GPG_SIGN_XX;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_JAVADOC;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_REPORT;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_SKIP_TEST;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_SOURCE;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_SURFIRE_XX;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_TAG_XX;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_UPDATE;
import static berlin.yuna.mavendeploy.MavenCommands.CMD_MVN_VERSION_XX;
import static berlin.yuna.mavendeploy.MavenCommands.FILE_MVN_FAILSAFE;
import static berlin.yuna.mavendeploy.MavenCommands.SONATYPE_PLUGIN;
import static berlin.yuna.mavendeploy.MavenCommands.SONATYPE_STAGING_URL;
import static berlin.yuna.mavendeploy.MavenCommands.SONATYPE_URL;
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
                + " --MVN_SKIP_TEST=true"
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
        assertThat(mavenCommand, containsString(CMD_MVN_CLEAN));
        assertThat(mavenCommand, containsString("clean"));
        assertThat(mavenCommand, containsString("deploy"));
        assertThat(mavenCommand, containsString(CMD_MVN_REPORT));
        assertThat(mavenCommand, containsString(CMD_MVN_UPDATE));
        assertThat(mavenCommand, containsString(CMD_MVN_CLEAN_CACHE));
        assertThat(mavenCommand, containsString(CMD_MVN_JAVADOC));
        assertThat(mavenCommand, containsString(CMD_MVN_SOURCE));
        assertThat(mavenCommand, containsString(CMD_MVN_TAG_XX));
        assertThat(mavenCommand, containsString(CMD_MVN_SKIP_TEST));
        assertThat(mavenCommand, containsString(CMD_MVN_VERSION_XX));
        assertThat(mavenCommand, containsString(CMD_MVN_GPG_SIGN_XX));
        assertThat(mavenCommand, containsString(CMD_MVN_SURFIRE_XX));
        assertThat(mavenCommand, containsString(CMD_MVN_FAILSAFE_XX));
        assertThat(mavenCommand, containsString(SONATYPE_URL));
        assertThat(mavenCommand, containsString(SONATYPE_PLUGIN));
        assertThat(mavenCommand, containsString(SONATYPE_STAGING_URL));
        assertThat(mavenCommand, containsString(" -Dproject.build.sourceEncoding=UTF-8"));
        assertThat(mavenCommand, containsString(" -Dproject.encoding=UTF-8"));
        assertThat(mavenCommand, containsString(" -Dproject.reporting.outputEncoding=UTF-8"));
        assertThat(mavenCommand, containsString(" -Dmaven.compiler.source=1.8"));
        assertThat(mavenCommand, containsString(" -Dmaven.compiler.target=1.8"));
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