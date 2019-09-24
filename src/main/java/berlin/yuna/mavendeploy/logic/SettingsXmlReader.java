package berlin.yuna.mavendeploy.logic;

import berlin.yuna.clu.logic.SystemUtil;
import berlin.yuna.clu.logic.Terminal;
import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.PluginSession;
import berlin.yuna.mavendeploy.util.MojoUtil;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.IdentifiableBase;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;
import static berlin.yuna.mavendeploy.util.MojoUtil.isPresent;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class SettingsXmlReader {

    private final PluginSession session;
    private final Logger log;
    private final MavenSession maven;


    private SettingsXmlReader(final PluginSession session) {
        this.session = session;
        this.log = session.getLog();
        this.maven = session.getMavenSession();
    }

    private List<Server> read() {
        final HashMap<String, Server> result = new HashMap<>();
        addGpgToSettings();
        parseFormatOne().forEach(server -> result.put(server.getId(), server));
        parseFormatTwo().forEach(server -> result.put(server.getId(), server));
        parseFormatThree().forEach(server -> result.put(server.getId(), server));
        return result.values().stream().filter(s -> isPresent(s.getId())).sorted(comparing(IdentifiableBase::getId)).collect(toList());
    }

    public static List<Server> read(final PluginSession session) {
        return new SettingsXmlReader(session).read();
    }

    public static String getGpgPath(final Logger log) {
        final String result;
        //TODO: implement copy terminal
        final Terminal t = new Terminal().consumerError(log::error).timeoutMs(5000).breakOnError(false);
        if (SystemUtil.isWindows()) {
            //FIXME: test on windows if installed, gpg || gpg2, same as with unix
            result = t.execute("where gpg").consoleInfo();
        } else {
            result = t.execute("if which gpg2 >/dev/null 2>&1; then which gpg2; "
                    + "elif which gpg >/dev/null 2>&1; then which gpg; else echo \"gpg\"; fi"
            ).consoleInfo();
            return result;
        }
        return isPresent(result) ? result : "gpg";
    }

    private void addGpgToSettings() {
        session.getParamPresent("gpg.pass", "gpg.passphrase").ifPresent(gpgPassphrase -> {
            log.info("%s Creating GPG profile", unicode(0x1F4D1));
            final Profile profile = new Profile();
            final Activation activation = new Activation();
            final Properties properties = new Properties();
            activation.setActiveByDefault(true);
            profile.setId("gpg");
            profile.setActivation(activation);
            properties.setProperty("gpg.executable", session.getParamFallback("gpg.executable", "gpg"));
            properties.setProperty("gpg.passphrase", gpgPassphrase);
            profile.setProperties(properties);
            maven.getSettings().getProfiles().add(profile);
        });
    }

    private List<Server> parseFormatOne() {
        final List<Server> result = new ArrayList<>();
        session.getParamPresent("settings.xml").ifPresent(settings -> {
            final List<String> servers = stream(settings.split("(?i)--serverId=")).filter(MojoUtil::isPresent).map(s -> "--serverId=" + s).collect(toList());
            for (String server : servers) {
                result.add(newServer(
                        parseFormatOne(server, "serverId"),
                        parseFormatOne(server, "username"),
                        parseFormatOne(server, "password"),
                        parseFormatOne(server, "privateKey"),
                        parseFormatOne(server, "passphrase"),
                        parseFormatOne(server, "filePermissions"),
                        parseFormatOne(server, "directoryPermissions")
                ));
            }
        });
        return result;
    }

    private List<Server> parseFormatTwo() {
        final List<Server> result = new ArrayList<>();
        int tries = 0;
        int server = -1;
        while (tries < 16) {
            final Optional<String> serverId = session.getParamPresent("server" + (server == -1 ? "" : server));
            if (serverId.isPresent()) {
                final String[] values = serverId.get().split("::");
                result.add(newServer(
                        parseFormatTwo(values, 0),
                        parseFormatTwo(values, 1),
                        parseFormatTwo(values, 2),
                        parseFormatTwo(values, 3),
                        parseFormatTwo(values, 4),
                        parseFormatTwo(values, 5),
                        parseFormatTwo(values, 6)
                ));
            } else {
                tries++;
            }
            server++;
        }
        return result;
    }

    private List<Server> parseFormatThree() {
        final List<Server> result = new ArrayList<>();
        int tries = 0;
        int server = -1;
        while (tries < 16) {
            final String prefix = "server" + (server == -1 ? "" : server) + ".";
            final Optional<String> serverId = session.getParamPresent(prefix + "id");
            if (serverId.isPresent()) {
                result.add(newServer(
                        serverId.get(),
                        parseFormatThree(prefix, "username"),
                        parseFormatThree(prefix, "password"),
                        parseFormatThree(prefix, "privateKey"),
                        parseFormatThree(prefix, "passphrase"),
                        parseFormatThree(prefix, "filepermissions"),
                        parseFormatThree(prefix, "directoryPermissions")
                ));
            } else {
                tries++;
            }
            server++;
        }
        return result;

    }

    private Server newServer(
            final String serverId,
            final String username,
            final String password,
            final String privateKey,
            final String passphrase,
            final String filePermissions,
            final String directoryPermissions) {
        final Server server = new Server();
        server.setId(serverId);
        server.setUsername(username);
        server.setPassword(password);
        server.setPrivateKey(privateKey);
        server.setPassphrase(passphrase);
        server.setFilePermissions(filePermissions);
        server.setDirectoryPermissions(directoryPermissions);
        return server;
    }

    private String parseFormatOne(final String server, final String search) {
        return stream(server.split("--"))
                .filter(MojoUtil::isPresent)
                .filter(s -> s.toLowerCase().trim().startsWith((search + "=").toLowerCase().trim()))
                .map(s -> s.substring((search + "=").length()).trim())
                .findFirst().orElse(null);
    }

    private String parseFormatTwo(final String[] values, final int i) {
        return values.length > i ? toNull(values[i]) : null;
    }

    private String parseFormatThree(final String prefix, final String username) {
        return toNull(session.getParamPresent(prefix + username).orElse(null));
    }

    private String toNull(final String value) {
        return isEmpty(value) || value.equalsIgnoreCase("null") ? null : value;
    }
}
