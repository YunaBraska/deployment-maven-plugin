package berlin.yuna.mavendeploy.config;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
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
import java.util.Properties;

import static java.lang.String.format;

public class PluginUpdater extends MojoBase {

    //TODO: use exclusionList
    public PluginUpdater(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        super("berlin.yuna", "plugin-updater", "0.0.1", environment, log);
    }

    public static PluginUpdater build(final MojoExecutor.ExecutionEnvironment environment, final Logger log) {
        return new PluginUpdater(environment, log);
    }

    public PluginUpdater update() throws IOException, XmlPullParserException {
        final String goal = "update";
        logGoal(goal, true);

        final MavenProject project = environment.getMavenProject();
        final List<Plugin> plugins = project.getBuildPlugins();
        final Path tmpProjectPath = Files.createTempDirectory("plugin-updater_");
        final Path pomFile = Paths.get(tmpProjectPath.toString(), "pom.xml");

        createPomFile(pomFile, plugins);
        getTerminal(pomFile).execute(mvnUpdate());
        reportPluginUpdates(log, plugins, pomFile.toFile());

        logGoal(goal, false);
        Files.deleteIfExists(pomFile);
        Files.deleteIfExists(tmpProjectPath);
        return this;
    }

    private Terminal getTerminal(final Path pomFile) {
        return new Terminal().dir(pomFile.getParent()).consumerError(log::error).consumerInfo(log::info);
    }

    //TODO: mojo execution
    private String mvnUpdate() {
        final Properties prop = environment.getMavenSession().getUserProperties();
        final boolean major = prop.containsKey("update.major");
        final boolean minor = prop.containsKey("update.minor");
        final String parameter = (major ? " -Dupdate.major" : "") + (minor ? " -Dupdate.minor" : "");
        final String mvnCmd = "mvn berlin.yuna:deployment-maven-plugin:0.0.1:run" + parameter;
        log.info(format("Running maven command [%s]", mvnCmd));
        return mvnCmd;
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
                    log.warn(format("Update plugin [%s] [%s] -> [%s]",
                            plugin.getArtifactId(),
                            plugin.getVersion(),
                            dependency.getVersion()));
                }
            }
        });
        return newVersionAvailable;
    }

    static void createPomFile(final Path pomFile, final List<Plugin> mojoList) {
        final Element project = new Element("project");
        project.addContent(new Element("modelVersion").addContent("4.0.0"));
        project.addContent(new Element("groupId").addContent("berlin.yuna"));
        project.addContent(new Element("artifactId").addContent("plugin-updater"));
        project.addContent(new Element("version").addContent("0.0.1"));
        project.addContent(new Element("packaging").addContent("pom"));
        project.addContent(preparePlugins(mojoList));
        overwriteTestPom(project, pomFile);
    }

    private static Element preparePlugins(final List<Plugin> mojoList) {
        final Element plugins = new Element("dependencies");
        mojoList.forEach(mojoBase -> {
            final Element plugin = new Element("dependency");
            plugin.addContent(new Element("groupId").addContent(mojoBase.getGroupId()));
            plugin.addContent(new Element("artifactId").addContent(mojoBase.getArtifactId()));
            plugin.addContent(new Element("version").addContent(mojoBase.getVersion()));
            plugins.addContent(plugin);
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
