package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

public class Compiler extends MojoBase {

    public Compiler(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("org.apache.maven.plugins", "maven-compiler-plugin", environment, log);
        version = "3.8.0";
    }

    public static Compiler build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new Compiler(environment, log);
    }

    public Compiler compiler() throws MojoExecutionException {
        final String goal = "compile";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
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
                ), environment
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
                prepareXpp3Dom(log, environment,
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
                ), environment
        );
        logGoal(goal, false);
        return this;
    }
}
