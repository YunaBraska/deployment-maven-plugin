package berlin.yuna.mavendeploy.logic;

import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readDeveloperProperties;
import static berlin.yuna.mavendeploy.logic.AdditionalPropertyReader.readLicenseProperties;
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

        final Properties result = readDeveloperProperties(inputs);
        assertThat(result, is(notNullValue()));
        assertThat(result.get("project.developers").toString(), is(equalTo("1")));
        assertThat(result.get("project.developers[0].id").toString(), is(equalTo("22.09.19")));
        assertThat(result.get("project.developers[0].url").toString(), is(equalTo("https://envelop.er")));
        assertThat(result.get("project.developers[0].name").toString(), is(equalTo("David Envelop")));
        assertThat(result.get("project.developers[0].email").toString(), is(equalTo("d.envelop@er")));
        assertThat(result.get("project.developers[0].organization").toString(), is(equalTo("Enveloper Solutions")));
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

        final Properties result = readLicenseProperties(inputs);
        assertThat(result, is(notNullValue()));
        assertThat(result.get("project.licenses").toString(), is(equalTo("1")));
        assertThat(result.get("project.licenses[0].url").toString(), is(equalTo("https://envelop.er")));
        assertThat(result.get("project.licenses[0].name").toString(), is(equalTo("David Envelop")));
        assertThat(result.get("project.licenses[0].comments").toString(), is(equalTo("d.envelop@er")));
        assertThat(result.get("project.licenses[0].distribution").toString(), is(equalTo("Enveloper Solutions")));
    }
}