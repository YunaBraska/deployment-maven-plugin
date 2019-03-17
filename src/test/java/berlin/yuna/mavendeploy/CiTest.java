package berlin.yuna.mavendeploy;

import berlin.yuna.mavendeploy.logic.Ci;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Test;

import java.io.File;

import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_CLEAN;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_CLEAN_CACHE;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_FAILSAFE_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_GPG_SIGN_ALT_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_GPG_SIGN_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_JAVADOC;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_REPORT;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_SOURCE;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_SURFIRE_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_TAG_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_UPDATE_MAJOR;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_UPDATE_MINOR;
import static berlin.yuna.mavendeploy.config.MavenCommands.CMD_MVN_VERSION_XX;
import static berlin.yuna.mavendeploy.config.MavenCommands.SONATYPE_PLUGIN;
import static berlin.yuna.mavendeploy.config.MavenCommands.SONATYPE_STAGING_URL;
import static berlin.yuna.mavendeploy.config.MavenCommands.SONATYPE_URL;
import static berlin.yuna.mavendeploy.config.MavenCommands.XX_CMD_MVN_SNAPSHOT;
import static berlin.yuna.mavendeploy.config.MavenCommands.XX_CMD_MVN_TAG_MSG;
import static berlin.yuna.mavendeploy.config.MavenCommands.XX_CMD_MVN_VERSION;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class CiTest {

    private static final File WORK_DIR = new File(System.getProperty("user.dir"));

    @Test
    public void prepareMaven_WithKeyAndWithoutValue_shouldResolve() {
        final String args = " --PROJECT_DIR=" + WORK_DIR
                + " --UPDATE_MINOR"
                + " --CLEAN_CACHE=true"
                + " --JAVA_DOC=false"
                + " --SOURCE=false"
                + " --COMMIT=false"
                + " --PROFILES=false";
        final String mavenCommand = new Ci(new SystemStreamLog(), args).prepareMaven();
        assertThat(mavenCommand, containsString(CMD_MVN_CLEAN_CACHE));
        assertThat(mavenCommand, containsString(CMD_MVN_UPDATE_MINOR));
    }

    @Test
    public void prepareMaven_WithAllFalse_shouldGetMinimalCommand() {
        final String args = " --PROJECT_DIR=" + WORK_DIR
                + " --PROFILES=false"
                + " --JAVA_DOC=false"
                + " --COMMIT=false"
                + " --SOURCE=false";
        assertThat(new Ci(new SystemStreamLog(), args).prepareMaven(),
                is(equalTo("mvn verify -Dmaven.test.skip=true")));
    }

    @Test
    public void prepareMaven_withAllParameters_shouldBeSuccessful() {
        final String args = "--PROJECT_DIR=" + new File(System.getProperty("user.dir"))
                + " --PROJECT_VERSION=3.2.1.2.3"
                + " --JAVA_VERSION=1.8"
                + " --ENCODING=UTF-8"
                + " --PROFILES=true"
                + " --REMOVE_SNAPSHOT"
                + " --CLEAN=true"
                + " --CLEAN_CACHE=true"
                + " --SKIP_TEST=false"
                + " --UPDATE_MINOR=true"
                + " --UPDATE_MAJOR=true"
                + " --JAVA_DOC=true"
                + " --SOURCE=true"
                + " --COMMIT=false"
                + " --TAG=true"
                + " --REPORT=true"
                + " --TAG_BREAK=true"
                + " --SEMANTIC_FORMAT=\"\\.::release.*::feature.*::bugfix.*|hotfix.*\""
                + " --GPG_PASS=${gppPassword-1}"
                + " --GPG_PASS_ALT=${gppPassword-2}"
                + " --DEPLOY_ID=nexus"
                + " --PROJECT_DIR=/Users/yunamorgenstern/Documents/projects/system-util"
                + " --S_SERVER=server-1"
                + " --S_USERNAME=server-1-user"
                + " --S_PASSWORD=server-1-pass"
                + " --S_SERVER=server-2"
                + " --S_USERNAME=server-2-user"
                + " --S_PASSWORD=server-2-pass"
                + " --S_SERVER=server-3";

        final Ci ci = new Ci(new SystemStreamLog(), args);
        final String mavenCommand = ci.prepareMaven();
        assertThat(mavenCommand, containsString(CMD_MVN_CLEAN));
        assertThat(mavenCommand, containsString("clean"));
        assertThat(mavenCommand, containsString("deploy"));
        assertThat(mavenCommand, containsString(CMD_MVN_REPORT));
        assertThat(mavenCommand, containsString(CMD_MVN_UPDATE_MAJOR));
        assertThat(mavenCommand, containsString(CMD_MVN_UPDATE_MINOR));
        assertThat(mavenCommand, containsString(CMD_MVN_CLEAN_CACHE));
        assertThat(mavenCommand, containsString(CMD_MVN_JAVADOC));
        assertThat(mavenCommand, containsString(CMD_MVN_SOURCE));
        assertThat(mavenCommand, containsString(CMD_MVN_TAG_XX));
        assertThat(mavenCommand, containsString(XX_CMD_MVN_TAG_MSG));
        assertThat(mavenCommand, containsString(SONATYPE_URL));
        assertThat(mavenCommand, containsString(SONATYPE_PLUGIN));
        assertThat(mavenCommand, containsString(SONATYPE_STAGING_URL));
        assertThat(mavenCommand, containsString(CMD_MVN_VERSION_XX));
        assertThat(mavenCommand, containsString(XX_CMD_MVN_VERSION));
        assertThat(mavenCommand, containsString(XX_CMD_MVN_SNAPSHOT));
        assertThat(mavenCommand, not(containsString("3.2.1.2.3")));
        assertThat(mavenCommand, containsString(CMD_MVN_VERSION_XX));
        assertThat(mavenCommand, containsString(CMD_MVN_GPG_SIGN_XX + "${gppPassword-1}"));
        assertThat(mavenCommand, containsString(CMD_MVN_GPG_SIGN_ALT_XX + "${gppPassword-2}"));
        assertThat(mavenCommand, containsString(CMD_MVN_SURFIRE_XX));
        assertThat(mavenCommand, containsString("mvnSurFireExcludes_"));
        assertThat(mavenCommand, containsString(CMD_MVN_FAILSAFE_XX));
        assertThat(mavenCommand, containsString("mvnFailSafeIncludes_"));
        assertThat(mavenCommand, containsString(" -Dproject.build.sourceEncoding=UTF-8"));
        assertThat(mavenCommand, containsString(" -Dproject.encoding=UTF-8"));
        assertThat(mavenCommand, containsString(" -Dproject.reporting.outputEncoding=UTF-8"));
        assertThat(mavenCommand, containsString(" -Dmaven.compiler.source=1.8"));
        assertThat(mavenCommand, containsString(" -Dmaven.compiler.target=1.8"));
    }
}