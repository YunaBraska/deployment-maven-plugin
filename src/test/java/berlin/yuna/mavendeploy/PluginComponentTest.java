package berlin.yuna.mavendeploy;

import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Compiler;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.Deploy;
import berlin.yuna.mavendeploy.config.Gpg;
import berlin.yuna.mavendeploy.config.Jar;
import berlin.yuna.mavendeploy.config.JavaSource;
import berlin.yuna.mavendeploy.config.Javadoc;
import berlin.yuna.mavendeploy.config.PluginUpdater;
import berlin.yuna.mavendeploy.config.PropertyWriter;
import berlin.yuna.mavendeploy.config.ReadmeBuilder;
import berlin.yuna.mavendeploy.config.Scm;
import berlin.yuna.mavendeploy.config.Surefire;
import berlin.yuna.mavendeploy.config.Versions;
import berlin.yuna.mavendeploy.helper.CustomMavenTestFramework;
import berlin.yuna.mavendeploy.helper.SettingsXmlBuilder;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsNot.not;

public class PluginComponentTest extends CustomMavenTestFramework {

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
    public void mojoClearCache_WithOutTrigger_shouldExecuteClearAndClearCache() {
        terminal.execute(mvnCmd("-Dclean.cache"));
        expectMojoRun(g(Clean.class, "clean"), g(Dependency.class, "resolve-plugins"), g(Dependency.class, "purge-local-repository"));
    }

    @Test
    public void mojoClear_WithTrigger_shouldExecuteClearAndClearCache() {
        terminal.execute(mvnCmd("-Dclean.cache=false"));
        expectMojoRun();
    }

    @Test
    public void updateMinor_shouldExecuteVersionsGoal() {
        terminal.execute(mvnCmd("-Dupdate.minor"));

        expectProperties(
                prop("allowSnapshots", "true"),
                prop("allowMajorUpdates", "false")
        );

        expectMojoRun(
                g(Versions.class, "update-parent"),
                g(Versions.class, "update-properties"),
                g(Versions.class, "update-child-modules"),
                g(Versions.class, "use-latest-releases"),
                g(Versions.class, "use-next-snapshots"),
                g(Versions.class, "use-latest-versions"),
                g(Versions.class, "commit")
        );
    }

    @Test
    public void updateMajor_shouldExecuteVersionsGoal() {
        terminal.execute(mvnCmd("-Dupdate.major"));

        expectProperties(
                prop("allowSnapshots", "true"),
                prop("allowMajorUpdates", "true")
        );

        expectMojoRun(
                g(Versions.class, "update-parent"),
                g(Versions.class, "update-properties"),
                g(Versions.class, "update-child-modules"),
                g(Versions.class, "use-latest-releases"),
                g(Versions.class, "use-next-snapshots"),
                g(Versions.class, "use-latest-versions"),
                g(Versions.class, "commit")
        );
    }

    @Test
    public void updatePlugins_shouldExecutePluginUpdater() {
        terminal.execute(mvnCmd("-Dupdate.plugins"));

        expectMojoRun(g(PluginUpdater.class, "update"));
    }

    @Test
    public void settingsSession_withServerFormatOne_shouldAddServerToSession() {
        terminal.execute(mvnCmd("-Ddeploy -Dsettings.xml=\"--ServerId=Server1 --Username=User1 --Password=Pass1 --ServerId=Server2 --Username=User2\""));
        assertThat(terminal.consoleInfo(), containsString("[Settings] added [Server] id [Server1] user [User1] pass [*****]"));
        assertThat(terminal.consoleInfo(), containsString("[Settings] added [Server] id [Server2] user [User2] pass [null]"));
    }

    @Test
    public void settingsSession_withServerFormatTwo_shouldAddServerToSession() {
        terminal.execute(mvnCmd(
                "-Ddeploy"
                        + " -Dserver='servername1::username1::null::privateKey1::passphrase1' "
                        + " -DSeRvEr0='servername2::username2::password2::::passphrase2' "
                        + " -Dserver1='servername3::username3::::' "
        ));
        assertThat(terminal.consoleInfo(), containsString("[Settings] added [Server] id [servername1] user [username1] pass [null]"));
        assertThat(terminal.consoleInfo(), containsString("[Settings] added [Server] id [servername2] user [username2] pass [*********]"));
        assertThat(terminal.consoleInfo(), containsString("[Settings] added [Server] id [servername3] user [username3] pass [null]"));
    }

    @Test
    public void settingsSession_withServerFormatThree_shouldAddServerToSession() {
        terminal.execute(mvnCmd(" -Ddeploy"
                + " -Dserver.Id='servername1' -Dserver.username='username1' -Dserver.password='null' "
                + " -Dserver0.iD='servername2' -Dserver0-username='username2' -Dserver0.password='password1' "
                + " -Dserver1.ID='servername3' -Dserver1_username='username3' -Dserver1.password='' "
        ));
        assertThat(terminal.consoleInfo(), containsString("[Settings] added [Server] id [servername1] user [username1] pass [null]"));
        assertThat(terminal.consoleInfo(), containsString("[Settings] added [Server] id [servername2] user [username2] pass [*********]"));
        assertThat(terminal.consoleInfo(), containsString("[Settings] added [Server] id [servername3] user [username3] pass [null]"));
    }

    @Test
    public void setParameter_manually_shouldNotBeOverwritten() {
        terminal.execute(mvnCmd("-Dproject.version=definedVersion -DnewVersion=manualSetVersion"));

        assertThat(terminal.consoleInfo(), containsString("(f) newVersion = manualSetVersion"));
        assertThat(getCurrentProjectVersion(), is(equalTo("manualSetVersion")));
    }

    @Test
    public void setParameter_auto_shouldBeOverwritten() {
        terminal.execute(mvnCmd("-Dproject.version=definedVersion"));

        expectProperties(prop("newVersion", "definedVersion"));
        assertThat(getCurrentProjectVersion(), is(equalTo("definedVersion")));
    }

    @Test
    public void setProjectVersion_manually_shouldSetNewVersion() {
        final String prevPomVersion = getCurrentProjectVersion();
        terminal.execute(mvnCmd("-Dproject.version=definedVersion"));

        expectProperties(prop("newVersion", "definedVersion"));
        final String newPomVersion = getCurrentProjectVersion();

        expectMojoRun(g(Versions.class, "set"));
        assertThat(newPomVersion, is(not(equalTo(prevPomVersion))));
        assertThat(newPomVersion, is(equalTo("definedVersion")));
    }

    @Test
    public void setProjectVersion_withNoSemanticMatch_shouldFallBackToDefinedVersion() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.3")));

        mergeBranch("feature/shouldNotBeRecognized");

        terminal.execute(mvnCmd("-Dproject.version=1.0.0-fallBackVersion -Dsemantic.format='[.]::none::none::none'"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.0.0-fallBackVersion")));
        expectProperties(prop("newVersion", "1.0.0-fallBackVersion"));
    }

    @Test
    public void setProjectVersion_withNoSemanticMatch_shouldFallBackToOriginalVersion() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.3")));

        mergeBranch("feature/shouldNotBeRecognized");
        terminal.execute(mvnCmd("-Dsemantic.format='[.]::none::none::none'"));

        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.3")));
    }

    @Test
    public void setProjectVersion_withSemantic_shouldAutoSetNewSemanticMajorVersion() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.3")));

        mergeBranch("feature/MarketingJustWantsThis");
        mergeBranch("bugfix/bugsEverywhere");
        mergeBranch("major/newAge");
        terminal.execute(mvnCmd("-Dsemantic.format='[.]::major.*::feature.*::bugfix.*'"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(getCurrentProjectVersion(), is(equalTo("2.0.0")));
        expectProperties(prop("newVersion", "2.0.0"));
    }

    @Test
    public void setProjectVersion_withSemantic_shouldAutoSetNewSemanticFeatureVersion() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.3")));

        mergeBranch("major/newAge");
        mergeBranch("bugfix/bugsEverywhere");
        mergeBranch("feature/MarketingJustWantsThis");
        terminal.execute(mvnCmd("-Dproject.version=definedVersion -Dsemantic.format='[.]::major.*::feature.*::bugfix.*'"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.3.0")));
        expectProperties(prop("newVersion", "1.3.0"));
    }

    @Test
    public void setProjectVersion_withSemantic_shouldAutoSetNewSemanticBugFixVersion() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.3")));

        mergeBranch("major/newAge");
        mergeBranch("feature/MarketingJustWantsThis");
        mergeBranch("bugfix/bugsEverywhere");
        terminal.execute(mvnCmd("-Dproject.version=definedVersion -Dsemantic.format='[.]::major.*::feature.*::bugfix.*'"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.4")));
        expectProperties(prop("newVersion", "1.2.4"));
    }

    @Test
    public void removeSnapshot_shouldBeSuccessful() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3-SNAPSHOT"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.3-SNAPSHOT")));

        terminal.execute(mvnCmd("-Dremove.snapshot"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.3")));
    }

    @Test
    public void createJavaDoc_shouldBeSuccessful() {
        terminal.execute(mvnCmd("-Djava.doc"));

        final File indexHtml = new File(TEST_DIR.toFile(), "target/apidocs/index.html");
        final File javaDoc = new File(TEST_DIR.toFile(), "target/" + TEST_POM.getArtifactId() + "-" + TEST_POM.getVersion() + "-javadoc.jar");
        assertThat(format("Cant find [%s]", indexHtml), indexHtml.exists(), is(true));
        assertThat(format("Cant find [%s]", javaDoc), javaDoc.exists(), is(true));

        expectMojoRun(g(Javadoc.class, "jar"));
    }

    @Test
    public void createJavaDocBreak_shouldBeSuccessful() {
        terminal.execute(mvnCmd("-Djava.doc.break"));

        final File indexHtml = new File(TEST_DIR.toFile(), "target/apidocs/index.html");
        final File javaDoc = new File(TEST_DIR.toFile(), "target/" + TEST_POM.getArtifactId() + "-" + TEST_POM.getVersion() + "-javadoc.jar");
        assertThat(format("Cant find [%s]", indexHtml), indexHtml.exists(), is(true));
        assertThat(format("Cant find [%s]", javaDoc), javaDoc.exists(), is(true));

        expectMojoRun(g(Javadoc.class, "jar"));
    }

    @Test
    public void createJavaSource_shouldBeSuccessful() {
        terminal.execute(mvnCmd("-Djava.source"));

        expectMojoRun(g(JavaSource.class, "jar-no-fork"));
        final File javaSource = new File(TEST_DIR.toFile(), "target/" + TEST_POM.getArtifactId() + "-" + TEST_POM.getVersion() + "-sources.jar");
        assertThat(format("Cant find [%s]", javaSource), javaSource.exists(), is(true));
    }

    @Test
    public void detectLibrary_shouldBeSuccessful() {
        setPackaging("pom");
        terminal.execute(mvnCmd(""));
        expectProperties(prop("project.library", "true"));

        setPackaging("jar");
        terminal.execute(mvnCmd(""));
        expectProperties(prop("project.library", "false"));
    }

    @Test
    public void tagging_shouldBeSuccessful() {
        terminal.execute(mvnCmd("-Dproject.version=18.09.19 -Dtag"));

        expectMojoRun(
                g(Versions.class, "set"),
                g(Scm.class, "tag"));
        assertThat(terminal.consoleInfo(), containsString("Tagging requested [18.09.19]"));
        assertThat(getCurrentProjectVersion(), is(equalTo("18.09.19")));
        assertThat(getCurrentGitTag(), is(equalTo("18.09.19")));
    }

    @Test
    public void tagging_withTagBreak_shouldBeSuccessful() {
        terminal.execute(mvnCmd("-Dproject.version=21.04.19 -Dtag.break"));

        expectMojoRun(
                g(Versions.class, "set"),
                g(Scm.class, "tag"));
        assertThat(terminal.consoleInfo(), containsString("Tagging requested [21.04.19]"));
        assertThat(getCurrentProjectVersion(), is(equalTo("21.04.19")));
        assertThat(getCurrentGitTag(), is(equalTo("21.04.19")));
    }

    @Test
    public void tagging_twice_shouldBeSuccessfulAndNotTagTwice() {
        terminalNoLog.execute(mvnCmd("-Dtag=10.06.19"));
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(getCurrentProjectVersion(), is(equalTo("1.2.3")));

        terminal.execute(mvnCmd("-Dproject.version=10.06.19 -Dtag"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(terminal.consoleInfo(), containsString("Tagging requested [10.06.19]"));
        assertThat(terminal.consoleInfo(), containsString("Git tag [10.06.19] already exists"));
        assertThat(getCurrentProjectVersion(), is(equalTo("10.06.19")));
        assertThat(gitService.getLastGitTag(), is(equalTo("10.06.19")));
    }

    @Test
    public void tagging_twiceWithTagBreak_shouldFailAlreadyExistsError() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=19.09.19 -Dtag.break"));
        terminal.execute(mvnCmd("-Dproject.version=19.09.19 -Dtag.break"));

        assertThat(terminal.consoleInfo(), containsString("Tagging requested [19.09.19]"));
        assertThat(terminal.consoleInfo(), containsString("Git tag [19.09.19] already exists"));
        assertThat(terminal.consoleInfo(), is(containsString("BUILD FAILURE")));
        assertThat(getCurrentProjectVersion(), is(equalTo("19.09.19")));
        assertThat(gitService.getLastGitTag(), is(equalTo("19.09.19")));
    }

    @Test
    public void surfire_WithTestRun_shouldExecuteSuccessful() {
        terminal.execute(mvnCmd("-Dtest.run"));

        expectMojoRun(
                g(Compiler.class, "compile"),
                g(Compiler.class, "testCompile"),
                g(Surefire.class, "test")
        );
        assertThat(terminal.consoleInfo(), not(containsString("Tests are skipped")));
        assertThat(terminal.consoleInfo(), not(containsString("No tests to run")));
        assertThat(terminal.consoleInfo(), containsString("Running berlin.yuna.project.logic.TimeServiceTest"));
        assertThat(terminal.consoleInfo(), containsString("Running berlin.yuna.project.controller.WebControllerUnitTest"));
    }

    @Test
    public void readmeBuilder_shouldExecuteSuccessful() {
        terminal.execute(mvnCmd("-Dbuilder"));

        expectMojoRun(g(ReadmeBuilder.class, "render"));
    }

    @Test
    public void deploySnapshot_shouldAddSnapshotToProjectVersion() {
        final String oldPomVersion = getCurrentProjectVersion();
        terminal.execute(mvnCmd("-Ddeploy.snapshot"));

        expectProperties(prop("newVersion", oldPomVersion + "-SNAPSHOT"));
        expectProperties(prop("newVersion", oldPomVersion));
        assertThat(getCurrentProjectVersion(), is(equalTo(oldPomVersion)));
        expectMojoRun(true, g(Versions.class, "set"), g(Jar.class, "jar"), g(Deploy.class, "deploy"));
    }

    @Test
    public void deploy_withEmptyDeployUrl_shouldNotStartDeployment() {
        terminal.execute(mvnCmd("-Dclean -Ddeploy -Ddeploy.url=''"));

        assertThat(terminal.consoleInfo(), containsString("[altDeploymentRepository] value [default::default::http://deploy.url-not.found]"));
        expectMojoRun(true, g(Clean.class, "clean"), g(Dependency.class, "resolve-plugins"), g(Jar.class, "jar"), g(Deploy.class, "deploy"));
    }

    @Test
    public void deploy_withEmptySettings_shouldNotStartDeployment() {
        terminal.execute(mvnCmd("-Dclean -Ddeploy -Ddeploy.url='https://aa.bb' --settings=" + new SettingsXmlBuilder().create()));
        assertThat(terminal.consoleInfo(), containsString("[deploy.id] not set"));
        assertThat(terminal.consoleInfo(), containsString("Cant find [deploy.id] by [deploy.url] [https://aa.bb]"));

        expectMojoRun(true, g(Clean.class, "clean"), g(Dependency.class, "resolve-plugins"), g(Jar.class, "jar"), g(Deploy.class, "deploy"));
    }

    @Test
    public void deploy_withDeployIdAndDeployUrlButEmptySettings_shouldNotStartDeployment() {
        terminal.execute(mvnCmd("-Dclean -Ddeploy -Ddeploy.id='invalid' -Ddeploy.url='https://nexus.invalid' --settings=" + new SettingsXmlBuilder().create()));

        assertThat(terminal.consoleInfo(), containsString("Config added key [altDeploymentRepository] value [invalid::default::https://nexus.invalid]"));
        expectMojoRun(true, g(Clean.class, "clean"), g(Dependency.class, "resolve-plugins"), g(Jar.class, "jar"), g(Deploy.class, "deploy"));
    }

    @Test
    public void deploy_withDeployIdAndDeployUrlAndSettings_shouldStartDeployment() {
        final SettingsXmlBuilder sxb = new SettingsXmlBuilder();
        sxb.addServer("validId", "username", "password");

        terminal.execute(mvnCmd("-Ddeploy -Ddeploy.id='validId' -Ddeploy.url='https://nexus.com' --settings=" + sxb.create()));

        assertThat(terminal.consoleInfo(), containsString("Config added key [altDeploymentRepository] value [validId::default::https://nexus.com]"));
    }

    @Test
    public void deploy_withoutDeployId_shouldFindServerByUrl() {
        final SettingsXmlBuilder sxb = new SettingsXmlBuilder();
        for (String server : getServerVariants()) {
            sxb.addServer(server, "username", "password");
        }
        final File settingsXml = sxb.create();

        for (String server : getServerVariants()) {
            terminal.execute(mvnCmd("-Ddeploy -Ddeploy.url='https://aa-" + server + "-bb.com' --settings=" + settingsXml));

            assertThat(terminal.consoleInfo(), containsString("Config added key [altDeploymentRepository] value [" + server + "::default::https://aa-" + server + "-bb.com]"));
            terminal.clearConsole();
        }
    }

    @Test
    public void deploy_withoutDeployID_shouldFindServerByFirstName() {
        for (String server : getServerVariants()) {
            final SettingsXmlBuilder sxb = new SettingsXmlBuilder();
            sxb.addServer("aa", "bb", "cc");
            sxb.addServer("dd", "ee", "ff");
            sxb.addServer(server, "11", "22");
            sxb.addServer("gg", "hh", "ii");
            sxb.addServer("jj", "kk", "ll");

            terminal.execute(mvnCmd("--settings=" + sxb.create()) + " -Ddeploy -Ddeploy.url='https://aa.bb'");

            assertThat(terminal.consoleInfo(), containsString("Config added key [altDeploymentRepository] value [" + server + "::default::https://aa.bb]"));
            terminal.clearConsole();
        }
    }

    @Test
    public void gpgTest() throws IOException {
        try {
            setupGpgTestKey(terminal, log);
            this.terminal.execute(mvnCmd("-Djava.doc -Djava.source -Dgpg.pass=mySecret"));
        } catch (Exception e) {
            log.error("GPG test failed cause [%s]", e);
        } finally {
            teardownGpgTestKey(terminal, log);
        }

        //FIXME: Travis has different GPG version, this test wont work
        //FIXME: how set GPG key as default? ¯\_(ツ)_/¯
        if (DEBUG) {
            final File target = new File(TEST_DIR.toFile(), "target");
            assertThat(target.exists(), is(true));
            final List<Path> ascFiles = Files.walk(target.toPath()).filter(f -> f.getFileName().toString().endsWith(".asc")).collect(toList());
            assertThat(ascFiles, hasSize(3));
            expectMojoRun(g(Javadoc.class, "jar"), g(JavaSource.class, "jar-no-fork"), g(Gpg.class, "sign"));
        }
    }

    @Test
    public void writeAllProperties_withBoolean_ShouldBeSuccessful() throws IOException {
        terminal.execute(mvnCmd("-Dproperties.print -Dsomepassword=b3rl1n -Dsomesecret=iAmAHero"));

        final File allProps = new File(TEST_DIR.toFile(), "target/all.properties");
        expectPropertyFile(allProps);
    }

    @Test
    public void writeAllProperties_withFileString_ShouldBeSuccessful() throws IOException {
        final File allProps = new File(TEST_DIR.toFile(), "myFolder/my.properties");
        terminal.execute(mvnCmd("-Dproperties.print=" + allProps + " -Dsomepassword=b3rl1n -Dsomesecret=iAmAHero"));

        expectPropertyFile(allProps);
    }

    private void expectPropertyFile(final File allPropsFile) throws IOException {
        assertThat(allPropsFile.exists(), is(true));
        final String content = Files.readString(allPropsFile.toPath());
        assertThat(content, containsString("somepassword = ******"));
        assertThat(content, containsString("somesecret = ********"));
        assertThat(content, not(containsString("iAmAHero")));
        expectMojoRun(g(PropertyWriter.class, "write"));
    }

    private String[] getServerVariants() {
        return new String[]{
                "my-nexus",
                "artifactsGoesHere",
                "archivaIsNow",
                "some-repository",
                "whatASnapshot"
        };
    }
}