package berlin.yuna.mavendeploy.config;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.MojoExecutor;
import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.apache.maven.model.Plugin;

import java.util.Objects;

import static berlin.yuna.mavendeploy.plugin.MojoExecutor.plugin;
import static java.lang.String.format;

public abstract class MojoBase {

    private final String groupId;
    private final String artifactId;
    private final String version;

    final PluginSession session;
    final Logger log;

    MojoBase(final String groupId, final String artifactId, final String version, final PluginSession session) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.session = session;
        this.log = session.getLog();
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    public Logger log() {
        return log;
    }

    public Plugin toPlugin() {
        final Plugin plugin = new Plugin();
        plugin.setGroupId(groupId);
        plugin.setArtifactId(artifactId);
        plugin.setVersion(version);
        return plugin;
    }

    Plugin getPlugin() {
        return plugin(
                MojoExecutor.groupId(groupId),
                MojoExecutor.artifactId(artifactId),
                MojoExecutor.version(version)
        );
    }

    void logGoal(final String goal, final boolean start) {
        log.info(format("--------------------------<=[ %s %s:%s:%s:%s ]=>--------------------------", start ? "Start" : "End", groupId, artifactId, goal, version));
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MojoBase mojoBase = (MojoBase) o;
        return Objects.equals(groupId, mojoBase.groupId) &&
                Objects.equals(artifactId, mojoBase.artifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId);
    }

    @Override
    public String toString() {
        return "MojoBase{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                '}';
    }
}