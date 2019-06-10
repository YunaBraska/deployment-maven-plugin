package berlin.yuna.mavendeploy;

import berlin.yuna.mavendeploy.config.Clean;
import berlin.yuna.mavendeploy.config.Compiler;
import berlin.yuna.mavendeploy.config.Dependency;
import berlin.yuna.mavendeploy.config.JavaSource;
import berlin.yuna.mavendeploy.config.Javadoc;
import berlin.yuna.mavendeploy.config.Resources;
import berlin.yuna.mavendeploy.config.Scm;
import berlin.yuna.mavendeploy.config.Versions;
import org.junit.Test;

import java.io.File;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;

//FIXME: how to make a GPG test? ¯\_(ツ)_/¯
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
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.3")));

        mergeBranch("feature/shouldNotBeRecognized");

        terminal.execute(mvnCmd("-Dproject.version=1.0.0-fallBackVersion -Dsemantic.format='[.]::none::none::none'"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.0.0-fallBackVersion")));
        expectProperties(prop("newVersion", "1.0.0-fallBackVersion"));
    }

    @Test
    public void setProjectVersion_withNoSemanticMatch_shouldFallBackToOriginalVersion() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.3")));

        mergeBranch("feature/shouldNotBeRecognized");
        terminal.execute(mvnCmd("-Dsemantic.format='[.]::none::none::none'"));

        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.3")));
    }

    @Test
    public void setProjectVersion_withSemantic_shouldAutoSetNewSemanticMajorVersion() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.3")));

        mergeBranch("feature/MarketingJustWantsThis");
        mergeBranch("bugfix/bugsEverywhere");
        mergeBranch("major/newAge");
        terminal.execute(mvnCmd("-Dsemantic.format='[.]::major.*::feature.*::bugfix.*'"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("2.0.0")));
        expectProperties(prop("newVersion", "2.0.0"));
    }

    @Test
    public void setProjectVersion_withSemantic_shouldAutoSetNewSemanticFeatureVersion() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.3")));

        mergeBranch("major/newAge");
        mergeBranch("bugfix/bugsEverywhere");
        mergeBranch("feature/MarketingJustWantsThis");
        terminal.execute(mvnCmd("-Dproject.version=definedVersion -Dsemantic.format='[.]::major.*::feature.*::bugfix.*'"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.3.0")));
        expectProperties(prop("newVersion", "1.3.0"));
    }

    @Test
    public void setProjectVersion_withSemantic_shouldAutoSetNewSemanticBugFixVersion() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.3")));

        mergeBranch("major/newAge");
        mergeBranch("feature/MarketingJustWantsThis");
        mergeBranch("bugfix/bugsEverywhere");
        terminal.execute(mvnCmd("-Dproject.version=definedVersion -Dsemantic.format='[.]::major.*::feature.*::bugfix.*'"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.4")));
        expectProperties(prop("newVersion", "1.2.4"));
    }

    @Test
    public void removeSnapshot_shouldBeSuccessful() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3-SNAPSHOT"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.3-SNAPSHOT")));

        terminal.execute(mvnCmd("-Dremove.snapshot"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.3")));
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
        terminal.execute(mvnCmd("-Dproject.version=20.04.19 -Dtag"));

        expectMojoRun(
                g(Versions.class, "set"),
                g(Scm.class, "tag"));
        assertThat(terminal.consoleInfo(), containsString("Tagging requested [20.04.19]"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("20.04.19")));
        assertThat(terminalNoLog.execute("git describe --tag --always --abbrev=0").consoleInfo(), is(equalTo("20.04.19")));
    }

    @Test
    public void tagging_withTagBreak_shouldBeSuccessful() {
        terminal.execute(mvnCmd("-Dproject.version=20.04.19 -Dtag.break"));

        expectMojoRun(
                g(Versions.class, "set"),
                g(Scm.class, "tag"));
        assertThat(terminal.consoleInfo(), containsString("Tagging requested [20.04.19]"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("20.04.19")));
        assertThat(terminalNoLog.execute("git describe --tag --always --abbrev=0").consoleInfo(), is(equalTo("20.04.19")));
    }

    @Test
    public void tagging_twice_shouldBeSuccessfulAndNotTagTwice() {
        terminalNoLog.execute(mvnCmd("-Dtag=10.06.19"));
        terminalNoLog.execute(mvnCmd("-Dproject.version=1.2.3"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("1.2.3")));

        terminal.execute(mvnCmd("-Dproject.version=10.06.19 -Dtag"));

        expectMojoRun(g(Versions.class, "set"));
        assertThat(terminal.consoleInfo(), containsString("Tagging requested [10.06.19]"));
        assertThat(terminal.consoleInfo(), containsString("Git tag [10.06.19] already exists"));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("10.06.19")));
        assertThat(terminalNoLog.clearConsole().execute("git describe --tag --always --abbrev=0").consoleInfo(), is(equalTo("10.06.19")));
    }

    @Test
    public void tagging_twiceWithTagBreak_shouldFailAlreadyExistsError() {
        terminalNoLog.execute(mvnCmd("-Dproject.version=20.04.19 -Dtag.break"));
        terminal.execute(mvnCmd("-Dproject.version=20.04.19 -Dtag.break"));

        assertThat(terminal.consoleInfo(), containsString("Tagging requested [20.04.19]"));
        assertThat(terminal.consoleInfo(), containsString("Git tag [20.04.19] already exists"));
        assertThat(terminal.consoleInfo(), is(containsString("BUILD FAILURE")));
        assertThat(parse(TEST_POM).getVersion(), is(equalTo("20.04.19")));
        assertThat(terminalNoLog.clearConsole().execute("git describe --tag --always --abbrev=0").consoleInfo(), is(equalTo("20.04.19")));
    }

    @Test
    public void surfire_WithTestRun_shouldExecuteSuccessful() {
        terminal.execute(mvnCmd("-Dtest.run"));
        assertThat(terminal.consoleInfo(), not(containsString("Tests are skipped")));
        assertThat(terminal.consoleInfo(), not(containsString("No tests to run")));

        expectMojoRun(
                g(Resources.class, "resources"),
                g(Resources.class, "testResources"),
                g(Compiler.class, "compile"),
                g(Compiler.class, "testCompile"));
    }
}