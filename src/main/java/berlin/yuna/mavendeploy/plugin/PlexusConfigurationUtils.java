package berlin.yuna.mavendeploy.plugin;

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

class PlexusConfigurationUtils {
    private PlexusConfigurationUtils() {
        throw new AssertionError("non-instantiable");
    }

    /**
     * Converts PlexusConfiguration to a Xpp3Dom.
     *
     * @param config the PlexusConfiguration. Must not be {@code null}.
     * @return the Xpp3Dom representation of the PlexusConfiguration
     */
    public static Xpp3Dom toXpp3Dom(final PlexusConfiguration config) {
        final Xpp3Dom result = new Xpp3Dom(config.getName());
        result.setValue(config.getValue(null));
        for (String name : config.getAttributeNames()) {
            result.setAttribute(name, config.getAttribute(name));
        }
        for (PlexusConfiguration child : config.getChildren()) {
            result.addChild(toXpp3Dom(child));
        }
        return result;
    }
}