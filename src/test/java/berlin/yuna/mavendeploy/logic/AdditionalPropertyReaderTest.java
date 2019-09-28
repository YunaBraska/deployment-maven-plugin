package berlin.yuna.mavendeploy.logic;

import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readDeveloperProperties;
import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readLicenseProperties;
import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readModuleProperties;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdditionalPropertyReaderTest {

    @Test
    public void readDeveloperProperties_shouldBeSuccessful() {
        final List<Developer> inputs = new ArrayList<>();
        final Developer input = new Developer();
        input.setId("22.09.19");
        input.setEmail("d.envelop@er");
        input.setName("David Envelop");
        input.setUrl("https://envelop.er");
        input.setOrganization("Enveloper Solutions");
        inputs.add(input);

        final Map<String, String> result = readDeveloperProperties(inputs);
        assertThat(result, is(notNullValue()));
        assertThat(result.get("project.developers"), is(equalTo("1")));
        assertThat(result.get("project.developers[0].id"), is(equalTo("22.09.19")));
        assertThat(result.get("project.developers[0].url"), is(equalTo("https://envelop.er")));
        assertThat(result.get("project.developers[0].name"), is(equalTo("David Envelop")));
        assertThat(result.get("project.developers[0].email"), is(equalTo("d.envelop@er")));
        assertThat(result.get("project.developers[0].organization"), is(equalTo("Enveloper Solutions")));
    }

    @Test
    public void readLicenseProperties_shouldBeSuccessful() {
        final List<License> inputs = new ArrayList<>();
        final License input = new License();
        input.setUrl("https://envelop.er");
        input.setName("David Envelop");
        input.setComments("d.envelop@er");
        input.setDistribution("Enveloper Solutions");
        inputs.add(input);

        final Map<String, String> result = readLicenseProperties(inputs);
        assertThat(result, is(notNullValue()));
        assertThat(result.get("project.licenses"), is(equalTo("1")));
        assertThat(result.get("project.licenses[0].url"), is(equalTo("https://envelop.er")));
        assertThat(result.get("project.licenses[0].name"), is(equalTo("David Envelop")));
        assertThat(result.get("project.licenses[0].comments"), is(equalTo("d.envelop@er")));
        assertThat(result.get("project.licenses[0].distribution"), is(equalTo("Enveloper Solutions")));
    }

    @Test
    public void readModuleProperties_shouldBeSuccessful() {
        final List<String> inputs = asList("module1", "module2", "module3");

        final Map<String, String> result = readModuleProperties(inputs);
        assertThat(result, is(notNullValue()));
        assertThat(result.get("project.modules"), is(equalTo("3")));
        assertThat(result.get("project.module[0]"), is(equalTo("module1")));
        assertThat(result.get("project.module[1]"), is(equalTo("module2")));
        assertThat(result.get("project.module[2]"), is(equalTo("module3")));
    }
}