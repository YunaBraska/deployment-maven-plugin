package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.plugin.MojoExecutionException;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.goal;

public class Javadoc extends MojoBase {

    public Javadoc(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-javadoc-plugin", "3.1.0", session);
    }

    public static Javadoc build(final PluginSession session) {
        return new Javadoc(session);
    }

    public Javadoc jar() throws MojoExecutionException {
        final String goal = "jar";
        logGoal(goal, true);
        executeMojo(
                getPlugin(),
                goal(goal),
                session.prepareXpp3Dom(
//                        prop("additionalparam", "-Xdoclint:none"),
                        prop("doclint", session.getBoolean("java.doc.break").orElse(false) ? null : "none"),
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
                        prop("finalName"),
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
                        prop("source", "!8"),
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
                ), session.getEnvironment()
        );
        logGoal(goal, false);
        return this;
    }
}
