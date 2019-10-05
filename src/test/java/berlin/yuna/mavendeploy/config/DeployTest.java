package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.settings.Server;
import org.junit.Test;

import static berlin.yuna.mavendeploy.helper.PluginUnitBase.createTestSession;
import static berlin.yuna.mavendeploy.helper.PluginUnitBase.getServerVariants;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployTest {

    @Test
    public void deploy_withoutDeployID_shouldFindServerByFirstName() {
        for (String server : getServerVariants()) {
            final StringBuilder console = new StringBuilder();
            final PluginSession session = createTestSession();
            session.getLog().setConsumer(console::append);
            session.setNewParam("deploy", null);
            session.setNewParam("deploy.url", "https://aa.bb");

            session.getMavenSession().getSettings().addServer(prepareServer("aa", "bb"));
            session.getMavenSession().getSettings().addServer(prepareServer("dd", "ee"));
            session.getMavenSession().getSettings().addServer(prepareServer(server, "11"));
            session.getMavenSession().getSettings().addServer(prepareServer("gg", "hh"));
            session.getMavenSession().getSettings().addServer(prepareServer("jj", "kk"));

            new Deploy(session).prepareSettingsServer();
            new Deploy(session).configureDeployment();

            assertThat(console.toString(), containsString("Config added key [altDeploymentRepository] value [" + server + "::default::https://aa.bb]"));
        }
    }

    private Server prepareServer(final String id, final String username) {
        final Server server1 = mock(Server.class);
        when(server1.getId()).thenReturn(id);
        when(server1.getUsername()).thenReturn(username);
        return server1;
    }
}
