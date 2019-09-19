package berlin.yuna.mavendeploy.config;

import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.helper.CustomMavenTestFramework;
import berlin.yuna.mavendeploy.model.Logger;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static berlin.yuna.mavendeploy.config.PluginUpdater.createPomFile;
import static berlin.yuna.mavendeploy.config.PluginUpdater.reportPluginUpdates;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class PluginUpdateGeneratorTest extends CustomMavenTestFramework {

    @Test
    public void displayPluginUpdates() throws IOException, XmlPullParserException {
        final File pomFile = TEST_POM.getPomFile();
        final List<Plugin> mojoList = getAllMojos().stream()
                        .filter(mojo -> !mojo.equals(new PluginUpdater(null, null)))
                        .filter(mojo -> !mojo.equals(new ReadmeBuilder(null, null)))
                .map(MojoBase::toPlugin)
                        .collect(Collectors.toList());
        createPomFile(pomFile.toPath(), mojoList);

        updatePluginUpdater();
        getTerminal().execute(mvnCmd("-Dupdate.major"));
        final HashMap<Plugin, String> availableVersions = reportPluginUpdates(new Logger(), mojoList, pomFile);

        availableVersions.forEach((mojo, newVersion) -> {
            try {
                updateInCodePluginVersions(mojo, newVersion, getPath(mojo.getClass()));
                getPath(mojo.getClass());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void updateInCodePluginVersions(final Plugin mojo, final String newVersion, final Path path) throws IOException {
        final String content = new String(Files.readAllBytes(path), UTF_8);
        Files.write(path,
                content.replace(format("\"%s\",", mojo.getVersion()), format("\"%s\",", newVersion)).getBytes(UTF_8));
    }

    //TODO: move target to MojoHelper and use it as environment variable
    private void updatePluginUpdater() throws IOException {
        final Path path = getPath(PluginUpdater.class);
        final String content = new String(Files.readAllBytes(path), UTF_8);
        PROJECT_POM.getVersion();
        //final String mvnCmd
        Files.write(path, content.replaceFirst(
                "final String mvnCmd.*;",
                "final String mvnCmd = \"mvn "
                        + PROJECT_POM.getGroupId()
                        + ":"
                        + PROJECT_POM.getArtifactId()
                        + ":"
                        + PROJECT_POM.getVersion()
                        + ":run\" + parameter;"
        ).getBytes());
    }

    private Terminal getTerminal() {
        return new Terminal().dir(TEST_POM.getPomFile().getParentFile()).consumerError(log::error).consumerInfo(log::info);
    }

    private Path getPath(final Class clazz) {
        final String classPath = clazz.getTypeName().replace(".", "/") + ".java";
        return new File(new File(System.getProperty("user.dir"), "src/main/java"), classPath).toPath();
    }
}
