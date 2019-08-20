package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

public class Surefire extends MojoBase {

    public Surefire(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("org.apache.maven.plugins", "maven-surefire-plugin", "2.22.1", environment, log);
    }

    public static Surefire build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new Surefire(environment, log);
    }

    public Surefire test() throws MojoExecutionException {
        final String goal = "test";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("maven.test.additionalClasspath"),
//                        prop("argLine", "-Xmx1024m -XX:MaxPermSize=256m"),
                        prop("childDelegation"),
                        prop("maven.test.dependency.excludes"),
                        prop("maven.surefire.debug"),
                        prop("dependenciesToScan"),
                        prop("disableXmlReport"),
                        prop("enableAssertions"),
                        prop("surefire.encoding"),
                        prop("excludedGroups"),
                        prop("includes",
                                prop("include", "**/*Test.java")),
                        prop("excludes",
                                prop("exclude", "**/*IntegrationTest.java"),
                                prop("exclude", "**/*IntTest.java"),
                                prop("exclude", "**/*ComponentTest.java"),
                                prop("exclude", "**/*CompTest.java"),
                                prop("exclude", "**/*ContractTest.java"),
                                prop("exclude", "**/*PactTest.java"),
                                prop("exclude", "**/*SmokeTest.java")),
                        prop("surefire.excludesFile"),
                        prop("surefire.failIfNoSpecifiedTests"),
                        prop("failIfNoTests", "true"),
                        prop("forkCount"),
                        prop("forkMode"),
                        prop("surefire.exitTimeout"),
                        prop("surefire.timeout"),
                        prop("groups"),
                        prop("surefire.includesFile"),
                        prop("junitArtifactName"),
                        prop("junitPlatformArtifactName"),
                        prop("jvm"),
                        prop("objectFactory"),
                        prop("parallel"),
                        prop("parallelOptimized"),
                        prop("surefire.parallel.forcedTimeout"),
                        prop("surefire.parallel.timeout"),
                        prop("perCoreThreadCount"),
                        prop("surefire.printSummary"),
                        prop("maven.test.redirectTestOutputToFile"),
                        prop("surefire.reportFormat"),
                        prop("surefire.reportNameSuffix"),
                        prop("surefire.reportsDirectory"),
                        prop("surefire.rerunFailingTestsCount"),
                        prop("reuseForks"),
                        prop("surefire.runOrder"),
                        prop("surefire.shutdown"),
//                        prop("maven.test.skip"),
                        prop("surefire.skipAfterFailureCount"),
                        prop("maven.test.skip.exec"),
                        prop("skipTests"),
                        prop("surefire.suiteXmlFiles"),
                        prop("test"),
                        prop("maven.test.failure.ignore"),
                        prop("testNGArtifactName"),
                        prop("threadCount"),
                        prop("threadCountClasses"),
                        prop("threadCountMethods"),
                        prop("threadCountSuites"),
                        prop("trimStackTrace"),
                        prop("surefire.useFile"),
                        prop("surefire.useManifestOnlyJar"),
                        prop("surefire.useModulePath"),
                        prop("useSystemClassLoader", "false"),
                        prop("useUnlimitedThreads"),
                        prop("basedir")
                ), environment
        );
        logGoal(goal, false);
        return this;
    }
}
