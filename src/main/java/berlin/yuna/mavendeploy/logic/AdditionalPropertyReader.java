package berlin.yuna.mavendeploy.logic;

import org.apache.maven.model.Developer;
import org.apache.maven.model.License;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.nullToEmpty;

public class AdditionalPropertyReader {

    public static Map<String, String> readDeveloperProperties(final List<Developer> developer) {
        final HashMap<String, String> properties = new HashMap<>();
        properties.put("project.developers", String.valueOf(developer.size()));
        for (int i = 0; i < developer.size(); i++) {
            properties.put("project.developers[" + i + "].id", nullToEmpty(developer.get(i).getId()));
            properties.put("project.developers[" + i + "].name", nullToEmpty(developer.get(i).getName()));
            properties.put("project.developers[" + i + "].url", nullToEmpty(developer.get(i).getUrl()));
            properties.put("project.developers[" + i + "].email", nullToEmpty(developer.get(i).getEmail()));
            properties.put("project.developers[" + i + "].organization", nullToEmpty(developer.get(i).getOrganization()));
        }
        return properties;
    }

    public static Map<String, String> readLicenseProperties(final List<License> licenses) {
        final HashMap<String, String> properties = new HashMap<>();
        properties.put("project.licenses", String.valueOf(licenses.size()));
        for (int i = 0; i < licenses.size(); i++) {
            properties.put("project.licenses[" + i + "].url", nullToEmpty(licenses.get(i).getUrl()));
            properties.put("project.licenses[" + i + "].name", nullToEmpty(licenses.get(i).getName()));
            properties.put("project.licenses[" + i + "].comments", nullToEmpty(licenses.get(i).getComments()));
            properties.put("project.licenses[" + i + "].distribution", nullToEmpty(licenses.get(i).getDistribution()));
        }
        return properties;
    }
}
