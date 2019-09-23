package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;

public class Compiler extends MojoBase {

    public Compiler(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-compiler-plugin", "3.8.1", session);
    }

    public static Compiler build(final PluginSession session) {
        return new Compiler(session);
    }

    public Compiler compiler() throws MojoExecutionException {
        final String goal = "compile";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("compilerId"),
                        prop("compilerReuseStrategy"),
                        prop("compilerVersion"),
                        prop("debug"),
                        prop("debuglevel"),
                        prop("executable"),
                        prop("failOnError"),
                        prop("failOnWarning"),
                        prop("forceJavacCompilerUse"),
                        prop("fork"),
                        prop("maxmem"),
                        prop("meminitial"),
                        prop("optimize"),
                        prop("parameters"),
                        prop("release"),
                        prop("showDeprecation"),
                        prop("showWarnings"),
                        prop("skipMultiThreadWarning"),
                        prop("source"),
                        prop("lastModGranularityMs"),
                        prop("target"),
                        prop("testRelease"),
                        prop("testSource"),
                        prop("testTarget"),
                        prop("useIncrementalCompilation"),
                        prop("verbose"),
                        prop("encoding")
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }

    public Compiler testCompiler() throws MojoExecutionException {
        final String goal = "testCompile";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
                        prop("compilerId"),
                        prop("compilerReuseStrategy"),
                        prop("compilerVersion"),
                        prop("debug"),
                        prop("debuglevel"),
                        prop("executable"),
                        prop("failOnError"),
                        prop("failOnWarning"),
                        prop("forceJavacCompilerUse"),
                        prop("fork"),
                        prop("maxmem"),
                        prop("meminitial"),
                        prop("optimize"),
                        prop("parameters"),
                        prop("release"),
                        prop("showDeprecation"),
                        prop("showWarnings"),
                        prop("skipMultiThreadWarning"),
                        prop("source"),
                        prop("lastModGranularityMs"),
                        prop("target"),
                        prop("testRelease"),
                        prop("testSource"),
                        prop("testTarget"),
                        prop("useIncrementalCompilation"),
                        prop("verbose"),
                        prop("encoding")
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
