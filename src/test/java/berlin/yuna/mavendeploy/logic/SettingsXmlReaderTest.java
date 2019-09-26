package berlin.yuna.mavendeploy.logic;


import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor.ExecutionEnvironment;
import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import static berlin.yuna.mavendeploy.helper.CustomMavenTestFramework.DEBUG;
import static berlin.yuna.mavendeploy.util.MojoUtil.isPresent;
import static java.util.Arrays.stream;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SettingsXmlReaderTest {

    private MavenSession mavenSession;
    private PluginSession pluginSession;

    @Before
    public void setUp() {
        mavenSession = mock(MavenSession.class);
        final ExecutionEnvironment environment = mock(ExecutionEnvironment.class);
        when(environment.getMavenSession()).thenReturn(mavenSession);
        pluginSession = new PluginSession(environment, new Logger().enableDebug(DEBUG));
    }

    @Test
    public void readSettings_fromEnvironmentAndProperties_ShouldBeSuccessful() {
        final String formatTwo = "-DSeRvEr1=servername1::username1::password1";
        final String formatThree = "-Dserver2.iD=servername2 -Dserver2_username=username2 -Dserver2.password=password2";
        final String formatThreeDuplicated = "-Dserver2.iD=servername2 -Dserver2_username=username2 -Dserver2.password=password2";

        final Properties userProperties = new Properties();
        final Properties systemProperties = new Properties();
        Stream.of(formatTwo, formatThree, formatThreeDuplicated).map(s -> s.split("-D")).forEach(s -> stream(s).forEach(sa -> {
                    final String[] split = sa.split("=");
                    if (isPresent(split[0])) {
                        userProperties.put(split[0], split[1].trim());
                    }
                })
        );
        systemProperties.put("settings.xml", "--ServerId=servername0 --Username=username0 --Password=password0");
        when(pluginSession.getProject()).thenReturn(new MavenProject());
        when(mavenSession.getUserProperties()).thenReturn(userProperties);
        when(mavenSession.getSystemProperties()).thenReturn(systemProperties);

        final List<Server> servers = SettingsXmlReader.read(pluginSession);
        final Optional<Server> server01 = servers.stream().filter(s -> s.getId().equals("servername1")).findFirst();
        assertThat(servers, hasSize(3));
        assertThat(server01.isPresent(), is(true));
        assertThat(server01.get().getId(), is(equalTo("servername1")));
        assertThat(server01.get().getUsername(), is(equalTo("username1")));
        assertThat(server01.get().getPassword(), is(equalTo("password1")));
    }

    @Test
    public void readGpg_fromEnvironmentAndProperties_ShouldBeSuccessful() {
        final Properties userProperties = new Properties();
        userProperties.put("gpg.passphrase", "12345");
        final Settings settings = new Settings();
        when(pluginSession.getProject()).thenReturn(new MavenProject());
        when(mavenSession.getUserProperties()).thenReturn(userProperties);
        when(mavenSession.getSystemProperties()).thenReturn(new Properties());
        when(mavenSession.getSettings()).thenReturn(settings);

        SettingsXmlReader.read(pluginSession);
        assertThat(settings.getProfiles(), hasSize(1));
        assertThat(settings.getProfiles().get(0).getProperties().get("gpg.passphrase"), is(equalTo("12345")));
        assertThat(settings.getProfiles().get(0).getProperties().get("gpg.executable"), is(equalTo("gpg")));

    }
}