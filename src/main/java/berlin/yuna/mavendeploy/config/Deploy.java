package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.logic.SettingsXmlReader;
import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.settings.IdentifiableBase;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static berlin.yuna.mavendeploy.model.Prop.prop;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.executeMojo;
import static berlin.yuna.mavendeploy.plugin.PluginExecutor.goal;
import static berlin.yuna.mavendeploy.plugin.PluginSession.addSecret;
import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static berlin.yuna.mavendeploy.util.MojoUtil.isEmpty;

public class Deploy extends MojoBase {

    public Deploy(final PluginSession session) {
        super("org.apache.maven.plugins", "maven-deploy-plugin", "2.8.2", session);
    }

    public static Deploy build(final PluginSession session) {
        return new Deploy(session);
    }

    public Deploy deploy() throws MojoExecutionException {
        final String goal = "deploy";
        prepareSettingsServer();
        configureDeployment();
        if (isNexusDeployment()) {
            NexusStaging.build(session).deploy();
        } else {
            logGoal(goal, true);
            executeMojo(
                    getPlugin(),
                    goal(goal),
                    session.prepareXpp3Dom(
                            prop("altDeploymentRepository"),
                            prop("altReleaseDeploymentRepository"),
                            prop("altSnapshotDeploymentRepository"),
                            prop("deployAtEnd"),
                            prop("retryFailedDeploymentCount"),
                            prop("maven.deploy.skip")
                    ), session.getEnvironment()
            );
            logGoal(goal, false);
        }
        return this;
    }

    public void prepareSettingsServer() {
        final List<Server> serverList = SettingsXmlReader.read(session);
        serverList.forEach(server -> addSecret("pass", server.getPassword()));
        serverList.forEach(server -> log.info("%s [%s] added %s", unicode(0x271A), Settings.class.getSimpleName(), session.toString(server)));
        serverList.forEach(server -> {
            if (session.getMavenSession().getSettings().getServer(server.getId()) == null) {
                session.getMavenSession().getSettings().addServer(server);
            }
        });
    }

    public void configureDeployment() {
        final String deployUrl = session.getParamPresent("deploy.url").orElse("http://deploy.url-not.found");
        final String deployId = session.getParamPresent("deploy.id").orElse(findServerByDeployUrl(deployUrl).orElse(new Server()).getId());
        session.setParameter("deployId", deployId, true);
        session.setParameter("deploy.url", deployUrl, true);
        session.setParameter("altDeploymentRepository", deployId + "::default::" + deployUrl, true);

        if (session.getProject().getDistributionManagement() == null) {
            session.getProject().setDistributionManagement(new DistributionManagement());
        }
        if (session.getParamPresent("deploy").isPresent()) {
            if (session.getProject().getDistributionManagement().getRepository() == null || !session.getProject().getDistributionManagement().getRepository().getId().equals(deployId)) {
                log.info("%s Created repository id [%s] url [%s]", unicode(0x1f4d1), deployId, deployUrl);
                session.getProject().getDistributionManagement().setRepository(prepareRepository(deployId, deployUrl));
            }
        } else if (session.getParamPresent("deploy.snapshot").isPresent()) {
            if (session.getProject().getDistributionManagement().getSnapshotRepository() == null || !session.getProject().getDistributionManagement().getSnapshotRepository().getId().equals(deployId)) {
                log.info("%s Created snapshot-repository id [%s] url [%s]", unicode(0x1f4d1), deployId, deployUrl);
                session.getProject().getDistributionManagement().setSnapshotRepository(prepareRepository(deployId, deployUrl));
            }
        }
    }

    public boolean isNexusDeployment() {
        final Optional<String> nexusDeployment = session.getParamPresent("deploy.nexus");
        final String deployUrl = session.getParamPresent("deploy.url").orElse("http://deploy.url-not.found");
        if (nexusDeployment.isEmpty() && Stream.of("nexus", "sonatype", "oss").anyMatch(nexus -> deployUrl.toLowerCase().contains(nexus))) {
            session.setParameter("deploy.nexus", "true", true);
            return true;
        }
        return false;
    }

    private DeploymentRepository prepareRepository(final String deployId, final String deployUrl) {
        final DeploymentRepository repository = new DeploymentRepository();
        repository.setUrl(deployUrl);
        repository.setId(deployId);
        repository.setName(repository.getId());
        return repository;
    }

    private Optional<Server> findServerByDeployUrl(final String deployUrl) {
        log.warn("[deploy.id] not set");
        final String[] artifactRepositories = Arrays
                .stream(new String[]{"nexus", "artifact", "archiva", "repository", "snapshot"})
                .sorted(Comparator.comparingInt(o -> (deployUrl.toLowerCase().contains(o.toLowerCase()) ? -1 : 1)))
                .toArray(String[]::new);
        final Optional<Server> server = getServerContains(artifactRepositories);
        if (server.isPresent()) {
            log.info("%s Fallback to [deploy.id] [%s]", unicode(0x1F511), server.get().getId());
        } else {
            log.warn("Cant find [deploy.id] by [deploy.url] [%s]", deployUrl);
        }
        return server;
    }

    private Optional<Server> getServerContains(final String... names) {
        final List<Server> servers = session.getMavenSession().getSettings().getServers();
        if (servers != null && !servers.isEmpty()) {
            log.debug("serverList [%s]", servers.stream().map(IdentifiableBase::getId).collect(Collectors.joining(", ")));
            for (String name : names) {
                final Optional<Server> server = servers.stream()
                        .filter(s -> !isEmpty(s.getId()))
                        .filter(s -> s.getId().toLowerCase().contains(name.toLowerCase()) || (!isEmpty(s.getUsername()) && s.getUsername().toLowerCase().contains(name)))
                        .findFirst();
                log.debug("server [%s] name [%s]", server.orElse(new Server()).getId(), name);
                if (server.isPresent()) {
                    return server;
                }
            }
        }
        return ((servers != null) ? servers.stream().findFirst() : Optional.empty());
    }
}
