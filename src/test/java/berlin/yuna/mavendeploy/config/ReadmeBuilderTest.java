package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.logic.GitService;
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.DefaultBuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static berlin.yuna.mavendeploy.CustomMavenTestFramework.getPomFile;
import static berlin.yuna.mavendeploy.plugin.MojoRun.readDeveloperProperties;
import static berlin.yuna.mavendeploy.plugin.MojoRun.readLicenseProperties;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadmeBuilderTest {

    private static final Logger log = new Logger(null);
    private MojoExecutor.ExecutionEnvironment environment;

    @Before
    public void setUp() {
        final File pomFile = new File(System.getProperty("user.dir"), "pom.xml");
        final Model project = getPomFile(pomFile);
        project.setPomFile(pomFile);
        final MavenProject mavenProject = new MavenProject(project);
        mavenProject.setFile(project.getPomFile());
        final MavenSession mavenSession = mock(MavenSession.class);
        final Properties properties = prepareProperties(mavenProject);
        when(mavenSession.getUserProperties()).thenReturn(properties);
        environment = new MojoExecutor.ExecutionEnvironment(mavenProject, mavenSession, new DefaultBuildPluginManager());
    }

    @Test
    public void runBuilder() throws IOException {
        ReadmeBuilder.build(environment, log).render();
    }

    private Properties prepareProperties(final MavenProject mavenProject) {
        final Properties properties = new GitService(log, mavenProject.getBasedir(), true).getConfig();
        properties.put("project.basedir", mavenProject.getBasedir());
        properties.put("project.baseUri", mavenProject.getBasedir());
        properties.put("project.build.directory", new File(mavenProject.getBasedir(), "target"));
        properties.put("project.name", mavenProject.getArtifactId());
        properties.put("project.version", mavenProject.getVersion());
        properties.put("project.artifactId", mavenProject.getArtifactId());
        properties.put("project.groupId", mavenProject.getGroupId());
        properties.put("project.packaging", mavenProject.getPackaging());
        properties.put("project.description", mavenProject.getDescription().replaceAll(" +", " ").replaceAll("\n ", "\n"));
        properties.putAll(readDeveloperProperties(mavenProject));
        properties.putAll(readLicenseProperties(mavenProject));
        return properties;
    }
}
