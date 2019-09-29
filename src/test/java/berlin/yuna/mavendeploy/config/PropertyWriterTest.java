package berlin.yuna.mavendeploy.config;


import berlin.yuna.mavendeploy.helper.PluginUnitBase;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PropertyWriterTest extends PluginUnitBase {

    @Test
    public void entryToStringString_shouldRemoveLineSeparatorAndTabs() {
        final PropertyWriter propertyWriter = new PropertyWriter(session);
        final Properties properties = session.getProperties();
        final String allProps = properties.values().stream().map(String::valueOf).collect(Collectors.joining("; "));

        assertThat(allProps.contains(lineSeparator()), is(true));
        final String result = properties.entrySet().stream().map(propertyWriter::entryToString).collect(Collectors.joining("; "));
        assertThat(result.contains(lineSeparator()), is(false));
    }
}
