package berlin.yuna.mavendeploy.config;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static berlin.yuna.mavendeploy.util.MojoUtil.deletePath;
import static java.lang.String.format;

public class PluginUpdater extends MojoBase {

    //TODO: use exclusionList
    public PluginUpdater(final PluginSession session) {
        super("berlin.yuna", "plugin-updater", "0.0.1", session);
    }

    public static PluginUpdater build(final PluginSession session) {
        return new PluginUpdater(session);
    }

    public PluginUpdater update() throws IOException, XmlPullParserException {
        final String goal = "update";
        logGoal(goal, true);

        final MavenProject project = session.getEnvironment().getMavenProject();
        final List<Plugin> plugins = project.getBuildPlugins();
        final Path tmpProjectPath = Files.createTempDirectory("plugin-updater_");
        final Path tmpPomFile = Paths.get(tmpProjectPath.toString(), "pom.xml");

        createPomFile(tmpPomFile, plugins, session);
        for (String versionsGoal : new String[]{"use-latest-releases", "use-latest-versions", "use-next-snapshots"}) {
            getTerminal(tmpPomFile).execute(mvnUpdate(versionsGoal));
        }

        updatePomFile(reportPluginUpdates(log, plugins, tmpPomFile.toFile()));

        logGoal(goal, false);
        deletePath(tmpProjectPath);
        return this;
    }

    private void updatePomFile(final HashMap<Plugin, String> pluginUpdates) throws IOException {
        final File pomFile = Optional.ofNullable(session.getProject().getFile()).orElse(new File(session.getParamPresent("base.dir").orElse(""), "pom.xml"));
        if (pomFile.exists()) {
            String pomXml = Files.readString(pomFile.toPath());
            for (Map.Entry<Plugin, String> entry : pluginUpdates.entrySet()) {
                final Plugin plugin = entry.getKey();
                final String pluginRegex = "(?<prefix>.*" + plugin.getArtifactId() + "(.|\\R)*?<version>)(?<version>" + plugin.getVersion() + ")(?<suffix><.*)";
                pomXml = pomXml.replaceAll(pluginRegex, "${prefix}" + entry.getValue() + "${suffix}");
            }
            Files.write(pomFile.toPath(), pomXml.getBytes());
            log.info("%s Updated pom file [file://%s]", unicode(0x1F516), pomFile.toURI().getRawPath());
        } else {
            log.error("Can't find pom file to update please provide at least [base.dir] property");
        }
    }

    private Terminal getTerminal(final Path pomFile) {
        return new Terminal().dir(pomFile.getParent()).consumerError(log::error);
    }

    private String mvnUpdate(final String goal) {
        final Versions versions = Versions.build(session);
        return format(
                "mvn %s:%s:%s:%s -DallowMajorUpdates=%s -DallowSnapshots=%s",
                versions.groupId(),
                versions.artifactId(),
                versions.version(),
                goal,
                session.getBoolean("update.major").orElse(false).toString(),
                session.getBoolean("allowSnapshots").orElse(true).toString()
        );
    }

    static HashMap<Plugin, String> reportPluginUpdates(
            final Logger log,
            final List<Plugin> mojoList,
            final File pomFile
    ) throws IOException, XmlPullParserException {
        final java.util.HashMap<Plugin, java.lang.String> newVersionAvailable = new HashMap<>();
        final Model pomXml = new MavenXpp3Reader().read(new FileReader(pomFile));
        pomXml.getDependencies().forEach(dependency -> {
            for (Plugin plugin : mojoList) {
                if (dependency.getGroupId().equalsIgnoreCase(plugin.getGroupId())
                        && dependency.getArtifactId().equalsIgnoreCase(plugin.getArtifactId())
                        && !dependency.getVersion().equalsIgnoreCase(plugin.getVersion())) {
                    newVersionAvailable.put(plugin, dependency.getVersion());
                    log.info("%s Update plugin [%s] [%s] -> [%s]",
                            unicode(0x1F4C8),
                            plugin.getArtifactId(),
                            plugin.getVersion(),
                            dependency.getVersion());
                }
            }
        });
        return newVersionAvailable;
    }

    static void createPomFile(final Path pomFile, final List<Plugin> mojoList, final PluginSession session) {
        final PluginUpdater updater = PluginUpdater.build(session);
        final Element project = new Element("project");
        project.addContent(new Element("modelVersion").addContent("4.0.0"));
        project.addContent(new Element("groupId").addContent(updater.groupId()));
        project.addContent(new Element("artifactId").addContent(updater.artifactId()));
        project.addContent(new Element("version").addContent(updater.version()));
        project.addContent(new Element("packaging").addContent("pom"));
        project.addContent(preparePlugins(mojoList, session.getLog()));
        overwriteTestPom(project, pomFile);
    }

    private static Element preparePlugins(final List<Plugin> mojoList, final Logger log) {
        final Element plugins = new Element("dependencies");
        mojoList.forEach(mojoBase -> {
            final Element plugin = new Element("dependency");
            plugin.addContent(new Element("groupId").addContent(mojoBase.getGroupId()));
            plugin.addContent(new Element("artifactId").addContent(mojoBase.getArtifactId()));
            plugin.addContent(new Element("version").addContent(mojoBase.getVersion()));
            plugins.addContent(plugin);
            log.debug("Prepare plugin [%s]", mojoBase.getArtifactId());
        });
        return plugins;
    }

    private static void overwriteTestPom(final Element project, final Path pomFile) {
        try {
            Files.write(pomFile, xmlToBytes(project));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] xmlToBytes(final Element project) {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + new XMLOutputter().outputString(project)
                .replace("<project>", "<project"
                        + " xmlns=\"http://maven.apache.org/POM/4.0.0\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">")
        ).getBytes();
    }

}
