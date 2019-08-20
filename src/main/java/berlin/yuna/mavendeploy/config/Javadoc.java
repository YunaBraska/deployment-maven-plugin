package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.MojoExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.MojoHelper.prepareXpp3Dom;

public class Javadoc extends MojoBase {

    public Javadoc(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("org.apache.maven.plugins", "maven-javadoc-plugin", "3.0.1", environment, log);
    }

    public static Javadoc build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new Javadoc(environment, log);
    }

    public Javadoc jar() throws MojoExecutionException {
        final String goal = "jar";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                prepareXpp3Dom(log, environment,
//                        prop("additionalparam", "-Xdoclint:none"),
                        prop("doclint", "none"),
                        prop("maven.javadoc.classifier"),
                        prop("destDir"),
                        prop("additionalJOption"),
                        prop("maven.javadoc.applyJavadocSecurityFix"),
                        prop("attach"),
                        prop("author"),
                        prop("bootclasspath"),
                        prop("bootclasspathArtifacts"),
                        prop("bottom"),
                        prop("breakiterator"),
                        prop("charset"),
                        prop("debug"),
                        prop("detectJavaApiLink"),
                        prop("detectLinks"),
                        prop("detectOfflineLinks"),
                        prop("docencoding"),
                        prop("docfilessubdirs"),
                        prop("doclet"),
                        prop("docletArtifact"),
                        prop("docletArtifacts"),
                        prop("docletPath"),
                        prop("doclint"),
                        prop("encoding"),
                        prop("doctitle"),
                        prop("excludePackageNames"),
                        prop("excludedocfilessubdir"),
                        prop("extdirs"),
                        prop("maven.javadoc.failOnError"),
                        prop("maven.javadoc.failOnWarnings"),
                        prop("project.build.finalName"),
                        prop("header"),
                        prop("footer"),
                        prop("helpfile"),
                        prop("project.build.directory"),
                        prop("javadocExecutable"),
                        prop("javadocVersion"),
                        prop("keywords"),
                        prop("links"),
                        prop("linksource"),
                        prop("localRepository"),
                        prop("locale"),
                        prop("maxmemory"),
                        prop("minmemory"),
                        prop("nocomment"),
                        prop("nodeprecated"),
                        prop("nodeprecatedlist", "true"),
                        prop("nohelp"),
                        prop("noindex"),
                        prop("nonavbar"),
                        prop("nooverview"),
                        prop("noqualifier"),
                        prop("nosince"),
                        prop("notimestamp"),
                        prop("notree"),
                        prop("offlineLinks"),
                        prop("overview"),
                        prop("packagesheader"),
                        prop("quiet", "true"),
                        prop("resourcesArtifacts"),
                        prop("serialwarn"),
                        prop("show"),
                        prop("maven.javadoc.skip"),
//                        prop("source", getString(environment.getMavenSession(), "javadoc-source", "222222")),
                        prop("sourcepath"),
                        prop("sourcetab"),
                        prop("linksourcetab"),
                        prop("splitindex"),
                        prop("stylesheet"),
                        prop("stylesheetfile"),
                        prop("subpackages"),
                        prop("taglet"),
                        prop("tagletArtifact"),
                        prop("tagletArtifacts"),
                        prop("tagletpath"),
                        prop("tags"),
                        prop("top"),
                        prop("use"),
                        prop("useStandardDocletOptions"),
                        prop("validateLinks"),
                        prop("verbose"),
                        prop("version"),
                        prop("windowtitle")
                ), environment
        );
        logGoal(goal, false);
        return this;
    }
}
