package berlin.yuna.mavendeploy.config;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.helper.CustomMavenTestFramework;
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static berlin.yuna.mavendeploy.config.PluginUpdater.createPomFile;
import static berlin.yuna.mavendeploy.config.PluginUpdater.reportPluginUpdates;
import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static java.lang.String.format;

public class PluginUpdateGeneratorTest extends CustomMavenTestFramework {

    final HashMap<String, String> fixedVersions = new HashMap<>();

    @Test
    public void displayPluginUpdates() throws IOException, XmlPullParserException {
        fixedVersions.put("maven-javadoc-plugin", "3.1.0");

        final File pomFile = TEST_POM.getPomFile();
        final List<MojoBase> mojoBases = getAllMojos();
        final List<Plugin> mojoList = mojoBases.stream()
                .filter(mojo -> !mojo.equals(new PluginUpdater(new PluginSession(null, log))))
                .filter(mojo -> !mojo.equals(new ReadmeBuilder(new PluginSession(null, log))))
                .filter(mojo -> !mojo.equals(new PropertyWriter(new PluginSession(null, log))))
                .map(MojoBase::toPlugin)
                .collect(Collectors.toList());
        createPomFile(pomFile.toPath(), mojoList);

        updatePluginUpdaterClass();
        getTerminal().execute(mvnCmd("-Dupdate.major"));
        final HashMap<Plugin, String> availableVersions = reportPluginUpdates(log, mojoList, pomFile);

        availableVersions.forEach((mojo, newVersion) -> {
            try {
                if (fixedVersions.containsKey(mojo.getArtifactId())) {
                    final String fixedVersion = fixedVersions.get(mojo.getArtifactId());
                    log.error("%s Update block for [%s] fall back to [%s]", unicode(0x1F940), mojo.getArtifactId(), fixedVersion);
                    updateInCodePluginVersions(mojo, fixedVersion, getPath(mojoBases, mojo));
                } else {
                    updateInCodePluginVersions(mojo, newVersion, getPath(mojoBases, mojo));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void updateInCodePluginVersions(final Plugin mojo, final String newVersion, final Path path) throws IOException {
        final String content = Files.readString(path);
        Files.writeString(path,
                content.replace(format("\"%s\",", mojo.getVersion()), format("\"%s\",", newVersion)));
    }

    //TODO: move target to MojoHelper and use it as environment variable
    private void updatePluginUpdaterClass() throws IOException {
        final Path path = getPath(PluginUpdater.class);
        final String content = Files.readString(path);
        //final String mvnCmd
        Files.write(path, content.replaceFirst(
                "final String mvnCmd.*;",
                "final String mvnCmd = \"mvn "
                        + PROJECT_POM.getGroupId()
                        + ":"
                        + PROJECT_POM.getArtifactId()
                        + ":"
                        + PROJECT_POM.getVersion()
                        + ":run -Dupdate.plugins=false \" + parameter;"
        ).getBytes());
    }

    private Terminal getTerminal() {
        return new Terminal().dir(TEST_POM.getPomFile().getParentFile()).consumerError(log::error).consumerInfo(log::info);
    }

    private Path getPath(final List<MojoBase> mojoBases, final Plugin plugin) {
        final Optional<MojoBase> mojoBase = mojoBases.stream()
                .filter(m -> m.groupId().equalsIgnoreCase(plugin.getGroupId()))
                .filter(m -> m.artifactId().equalsIgnoreCase(plugin.getArtifactId()))
                .findFirst();
        if (mojoBase.isPresent()) {
            return getPath(mojoBase.get().getClass());
        } else {
            throw new RuntimeException("Mojo not found [" + plugin + "]");
        }
    }
}
