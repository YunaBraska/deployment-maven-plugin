package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

public class Surfire extends MojoBase {

    public Surfire(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
        super("org.apache.maven.plugins", "maven-surefire-plugin", environment, log);
        version = "2.22.1";
    }

    public static Surfire build(final MojoExecutor.ExecutionEnvironment environment, final Log log) {
        return new Surfire(environment, log);
    }

    public Surfire test() throws MojoExecutionException {
        final String goal = "test";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
                        prop("maven.test.additionalClasspath"),
                        prop("argLine", "-Xmx1024m -XX:MaxPermSize=256m"),
                        prop("childDelegation"),
                        prop("maven.test.dependency.excludes"),
                        prop("maven.surefire.debug"),
                        prop("dependenciesToScan"),
                        prop("disableXmlReport"),
                        prop("enableAssertions"),
                        prop("surefire.encoding"),
                        prop("excludedGroups"),
                        //TODO: prop("surefire.excludesXML"),
                        prop("surefire.excludesFile"),
                        prop("surefire.failIfNoSpecifiedTests"),
                        prop("failIfNoTests", "true"),
                        prop("forkCount"),
                        prop("forkMode"),
                        prop("surefire.exitTimeout"),
                        prop("surefire.timeout"),
                        prop("groups"),
                        //TODO: prop("surefire.includesXML"),
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
                        prop("maven.test.skip"),
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
                        prop("surefire.useSystemClassLoader", "false"),
                        prop("useUnlimitedThreads"),
                        prop("basedir")
                ), environment
        );
        logGoal(goal, false);
        return this;
    }
}
