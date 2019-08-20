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
import java.util.List;

import static java.lang.String.format;

public class PluginUpdaterComponentTest extends CustomMavenTestFramework {

    @Test
    public void displayPluginUpdates() throws IOException, XmlPullParserException {
        File pomFile = TEST_POM.getPomFile();
        List<MojoBase> mojoList = getAllMojos();
        Element project = new Element("project");
        project.addContent(new Element("modelVersion").addContent("4.0.0"));
        project.addContent(new Element("groupId").addContent("berlin.yuna"));
        project.addContent(new Element("artifactId").addContent("plugin-updater"));
        project.addContent(new Element("version").addContent("0.0.1"));
        project.addContent(new Element("packaging").addContent("pom"));
        project.addContent(new Element("build").addContent(preparePlugins(mojoList)));

        writePomFile(project, pomFile);
        new Terminal().consumerError(System.err::println).consumerInfo(System.out::println)
                .execute(mvnCmd("-Dupdate.major -f=" + pomFile.getAbsolutePath()));
        reportPluginUpdates(mojoList, pomFile);

    }

    private void reportPluginUpdates(final List<MojoBase> mojoList, final File pomFile) throws IOException, XmlPullParserException {
        final Model pomXml = new MavenXpp3Reader().read(new FileReader(pomFile));
        pomXml.getBuild().getPlugins().forEach(plugin -> {
            for (MojoBase mojo : mojoList) {
                if (plugin.getGroupId().equalsIgnoreCase(mojo.groupId())
                        && plugin.getArtifactId().equalsIgnoreCase(mojo.artifactId())
                        && !plugin.getVersion().equalsIgnoreCase(mojo.version())) {
                    System.err.println(format("[%s] [10%s] -> [%s]", plugin.getVersion(), mojo.version(), plugin.getVersion()));
                }
            }
        });
    }

    private Element preparePlugins(List<MojoBase> mojoList) {
        Element plugins = new Element("plugins");
        mojoList.forEach(mojoBase -> {
            final Element plugin = new Element("plugin");
            plugin.addContent(new Element("groupId").addContent(mojoBase.groupId()));
            plugin.addContent(new Element("artifactId").addContent(mojoBase.artifactId()));
            plugin.addContent(new Element("version").addContent(mojoBase.version()));
            plugins.addContent(plugin);
        });
        return plugins;
    }

    private void writePomFile(final Element project, final File pomFile) {
        try {
            Files.write(pomFile.toPath(), xmlToBytes(project));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] xmlToBytes(Element project) {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + new XMLOutputter().outputString(project)
                .replace("<project>", "<project"
                        + " xmlns=\"http://maven.apache.org/POM/4.0.0\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">")
        ).getBytes();
    }
}
