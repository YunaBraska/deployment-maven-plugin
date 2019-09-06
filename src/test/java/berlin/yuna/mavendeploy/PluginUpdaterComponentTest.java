package berlin.yuna.mavendeploy;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.config.MojoBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

//TODO: move logic to real pluginUpdater and update also own plugins
public class PluginUpdaterComponentTest extends CustomMavenTestFramework {

    @Test
    public void displayPluginUpdates() throws IOException, XmlPullParserException {
        final File pomFile = TEST_POM.getPomFile();
        final List<MojoBase> mojoList = getAllMojos();
        createPomFile(pomFile, mojoList);

        getTerminal().execute(mvnCmd("-Dupdate.major"));
        final HashMap<MojoBase, String> availableVersions = reportPluginUpdates(mojoList, pomFile);

        availableVersions.forEach((mojo, newVersion) -> {
            System.err.println(format("[%s] [%s] -> [%s]", mojo.artifactId(), mojo.version(), newVersion));
            try {
                updateMojoVersions(mojo, newVersion, getPath(mojo.getClass()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private HashMap<MojoBase, String> reportPluginUpdates(final List<MojoBase> mojoList, final File pomFile) throws IOException, XmlPullParserException {
        final HashMap<MojoBase, String> newVersionAvailable = new HashMap<>();
        final Model pomXml = new MavenXpp3Reader().read(new FileReader(pomFile));
        pomXml.getDependencies().forEach(dependency -> {
            for (MojoBase mojo : mojoList) {
                if (dependency.getGroupId().equalsIgnoreCase(mojo.groupId())
                        && dependency.getArtifactId().equalsIgnoreCase(mojo.artifactId())
                        && !dependency.getVersion().equalsIgnoreCase(mojo.version())) {
                    newVersionAvailable.put(mojo, dependency.getVersion());
                }
            }
        });
        return newVersionAvailable;
    }

    private void createPomFile(final File pomFile, final List<MojoBase> mojoList) {
        final Element project = new Element("project");
        project.addContent(new Element("modelVersion").addContent("4.0.0"));
        project.addContent(new Element("groupId").addContent("berlin.yuna"));
        project.addContent(new Element("artifactId").addContent("plugin-updater"));
        project.addContent(new Element("version").addContent("0.0.1"));
        project.addContent(new Element("packaging").addContent("pom"));
        project.addContent(preparePlugins(mojoList));
        overwriteTestPom(project, pomFile);
    }

    private Element preparePlugins(final List<MojoBase> mojoList) {
        final Element plugins = new Element("dependencies");
        mojoList.forEach(mojoBase -> {
            final Element plugin = new Element("dependency");
            plugin.addContent(new Element("groupId").addContent(mojoBase.groupId()));
            plugin.addContent(new Element("artifactId").addContent(mojoBase.artifactId()));
            plugin.addContent(new Element("version").addContent(mojoBase.version()));
            plugins.addContent(plugin);
        });
        return plugins;
    }

    private void overwriteTestPom(final Element project, final File pomFile) {
        try {
            Files.write(pomFile.toPath(), xmlToBytes(project));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateMojoVersions(final MojoBase mojo, final String newVersion, final Path path) throws IOException {
        String content = new String(Files.readAllBytes(path), UTF_8);
        Files.write(path, content.replace(format("\"%s\",", mojo.version()), format("\"%s\",", newVersion)).getBytes(UTF_8));
    }

    private byte[] xmlToBytes(final Element project) {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + new XMLOutputter().outputString(project)
                .replace("<project>", "<project"
                        + " xmlns=\"http://maven.apache.org/POM/4.0.0\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">")
        ).getBytes();
    }

    private Terminal getTerminal() {
        return new Terminal().dir(TEST_POM.getPomFile().getParentFile()).consumerError(System.err::println).consumerInfo(System.out::println);
    }

    private Path getPath(final Class clazz) {
        final String classPath = clazz.getTypeName().replace(".", "/") + ".java";
        return new File(new File(System.getProperty("user.dir"), "src/main/java"), classPath).toPath();
    }
}
