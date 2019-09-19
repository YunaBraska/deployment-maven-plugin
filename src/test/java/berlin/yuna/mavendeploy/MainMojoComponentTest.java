package berlin.yuna.mavendeploy;

import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Compiler;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.JavaSource;
import berlin.yuna.mavendeploy.config.Javadoc;
import berlin.yuna.mavendeploy.config.PluginUpdater;
import berlin.yuna.mavendeploy.config.ReadmeBuilder;
import berlin.yuna.mavendeploy.config.Scm;
import berlin.yuna.mavendeploy.config.Surefire;
import berlin.yuna.mavendeploy.config.Versions;
import berlin.yuna.mavendeploy.helper.CustomMavenTestFramework;
import berlin.yuna.mavendeploy.logic.SettingsXmlBuilder;
import org.junit.Test;

import java.io.File;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

//FIXME: how to do a GPG test? ¯\_(ツ)_/¯
public class MainMojoComponentTest extends CustomMavenTestFramework {

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
                g(PluginUpdater.class, "update"),
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
                g(PluginUpdater.class, "update"),
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
    public void settingsSession_withServer_shouldAddServerToSession() {
        terminal.execute(mvnCmd("-Dsettings.xml='--Server=\"Server1\" --Username=User1 --Password=Pass1 --Server=\"Server2\" --Username=User2'"));
        assertThat(terminal.consoleInfo(), containsString("+ Settings added [Server] id [Server1] user [User1] pass [******]"));
        assertThat(terminal.consoleInfo(), containsString("+ Settings added [Server] id [Server2] user [User2] pass [null]"));
    }

    @Test
    public void setParameter_manually_shouldNotBeOverwritten() {
        terminal.execute(mvnCmd("-Dproject.version=definedVersion -DnewVersion=manualSetVersion"));

        expectPropertiesOverwrite(prop("newVersion", "manualSetVersion"));
        assertThat(getPomFile(TEST_POM.getPomFile()).getVersion(), is(equalTo("manualSetVersion")));
    }

    @Test
    public void setParameter_auto_shouldBeOverwritten() {
        terminal.execute(mvnCmd("-Dproject.version=definedVersion"));

        expectProperties(prop("newVersion", "definedVersion"));
        assertThat(getPomFile(TEST_POM.getPomFile()).getVersion(), is(equalTo("definedVersion")));
    }

    @Test
    public void setProjectVersion_manually_shouldSetNewVersion() {
        final String prevPomVersion = getPomFile(TEST_POM.getPomFile()).getVersion();
        terminal.execute(mvnCmd("-Dproject.version=definedVersion"));

        expectProperties(prop("newVersion", "definedVersion"));
        final String newPomVersion = getPomFile(TEST_POM.getPomFile()).getVersion();

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

        expectMojoRun(g(Javadoc.class, "jar"));
        final File indexHtml = new File(TEST_POM.getPomFile().getParent(), "target/apidocs/index.html");
        final File javaDoc = new File(TEST_POM.getPomFile().getParent(), "target/" + TEST_POM.getArtifactId() + "-" + TEST_POM.getVersion() + "-javadoc.jar");
        assertThat(format("Cant find [%s]", indexHtml), indexHtml.exists(), is(true));
        assertThat(format("Cant find [%s]", javaDoc), javaDoc.exists(), is(true));

        expectMojoRun(g(Javadoc.class, "jar"));
    }

    @Test
    public void createJavaSource_shouldBeSuccessful() {
        terminal.execute(mvnCmd("-Djava.source"));

        expectMojoRun(g(JavaSource.class, "jar-no-fork"));
        final File javaSource = new File(TEST_POM.getPomFile().getParent(), "target/" + TEST_POM.getArtifactId() + "-" + TEST_POM.getVersion() + "-sources.jar");
        assertThat(format("Cant find [%s]", javaSource), javaSource.exists(), is(true));
    }

    @Test
    public void detectLibrary_shouldBeSuccessful() {
        setPackaging("pom");
        terminal.execute(mvnCmd(""));
        assertThat(terminal.consoleInfo(), is(containsString("Project is library [true]")));

        setPackaging("jar");
        terminal.execute(mvnCmd(""));
        assertThat(terminal.consoleInfo(), is(containsString("Project is library [false]")));
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
        terminal.execute(mvnCmd("-Dproject.version=20.04.19 -Dtag.break"));

        expectMojoRun(
                g(Versions.class, "set"),
                g(Scm.class, "tag"));
        assertThat(terminal.consoleInfo(), containsString("Tagging requested [20.04.19]"));
        assertThat(getCurrentProjectVersion(), is(equalTo("20.04.19")));
        assertThat(getCurrentGitTag(), is(equalTo("20.04.19")));
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
        assertThat(terminalNoLog.clearConsole().execute("git describe --tag --always --abbrev=0").consoleInfo(), is(equalTo("10.06.19")));
    }

    @Test
    public void tagging_twiceWithTagBreak_shouldFailAlreadyExistsError() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=19.09.19 -Dtag.break"));
        terminal.execute(mvnCmd("-Dproject.version=19.09.19 -Dtag.break"));

        assertThat(terminal.consoleInfo(), containsString("Tagging requested [19.09.19]"));
        assertThat(terminal.consoleInfo(), containsString("Git tag [19.09.19] already exists"));
        assertThat(terminal.consoleInfo(), is(containsString("BUILD FAILURE")));
        assertThat(getCurrentProjectVersion(), is(equalTo("19.09.19")));
        assertThat(terminalNoLog.clearConsole().execute("git describe --tag --always --abbrev=0").consoleInfo(), is(equalTo("20.04.19")));
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
        final String oldPomVersion = getPomFile(TEST_POM.getPomFile()).getVersion();
        terminal.execute(mvnCmd("-Ddeploy.snapshot"));

        expectProperties(prop("newVersion", oldPomVersion + "-SNAPSHOT"));
        expectProperties(prop("newVersion", oldPomVersion));
        assertThat(getPomFile(TEST_POM.getPomFile()).getVersion(), is(equalTo(oldPomVersion)));
        expectMojoRun(g(Versions.class, "set"));
    }

    @Test
    public void deploy_withEmptyDeployUrl_shouldNotStartDeployment() {
        terminal.execute(mvnCmd("-Dclean -Ddeploy -Ddeploy.url=''"));

        assertThat(terminal.consoleInfo(), not(containsString("Config added key [altDeploymentRepository]")));
        expectMojoRun(g(Clean.class, "clean"), g(Dependency.class, "resolve-plugins"));
    }

    @Test
    public void deploy_withEmptySettings_shouldNotStartDeployment() {
        terminal.execute(mvnCmd("-Dclean -Ddeploy -Ddeploy.url='https://aa.bb' --settings=" + new SettingsXmlBuilder().create()));
        assertThat(terminal.consoleInfo(), containsString("[deploy.id] not set"));
        assertThat(terminal.consoleInfo(), containsString("Cant find any credentials for deploy.id [null] deploy.url [https://aa.bb]"));

        expectMojoRun(g(Clean.class, "clean"), g(Dependency.class, "resolve-plugins"));
    }

    @Test
    public void deploy_withDeployIdAndDeployUrlButEmptySettings_shouldNotStartDeployment() {
        terminal.execute(mvnCmd("-Dclean -Ddeploy -Ddeploy.id='invalid' -Ddeploy.url='https://nexus.com' --settings=" + new SettingsXmlBuilder().create()));

        assertThat(terminal.consoleInfo(), containsString("DeployUrl [https://nexus.com]"));
        assertThat(terminal.consoleInfo(), containsString("Cant find any credentials for deploy.id [invalid] deploy.url [https://nexus.com]"));
        expectMojoRun(g(Clean.class, "clean"), g(Dependency.class, "resolve-plugins"));
    }

    //TODO: test settings as parameter
    @Test
    public void deploy_withDeployIdAndDeployUrlAndSettings_shouldStartDeployment() {
        final SettingsXmlBuilder sxb = new SettingsXmlBuilder();
        sxb.addServer("validId", "username", "password");

        terminal.execute(mvnCmd("-Ddeploy -Ddeploy.id='validId' -Ddeploy.url='https://nexus.com' --settings=" + sxb.create()));

        assertThat(terminal.consoleInfo(), containsString("DeployId [validId] deployUrl [https://nexus.com]"));
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

            assertThat(terminal.consoleInfo(), containsString("[deploy.id] not set"));
            assertThat(terminal.consoleInfo(), containsString("Fallback to deployId [" + server + "]"));
            assertThat(terminal.consoleInfo(), containsString(" The packaging for this project did not assign a file to the build artifact"));
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

            terminal.execute(mvnCmd("-Ddeploy -Ddeploy.url='https://aa.bb' --settings=" + sxb.create()));

            assertThat(terminal.consoleInfo(), containsString("[deploy.id] not set"));
            assertThat(terminal.consoleInfo(), containsString("Fallback to deployId [" + server + "]"));
            assertThat(terminal.consoleInfo(), containsString(" The packaging for this project did not assign a file to the build artifact"));
            terminal.clearConsole();
        }
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