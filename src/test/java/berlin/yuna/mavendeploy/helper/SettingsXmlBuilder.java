package berlin.yuna.mavendeploy.helper;


import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SettingsXmlBuilder {

    private final Element domServers;

    public SettingsXmlBuilder() {
        domServers = new Element("servers");
    }

    public File create() {
        try {
            final Document doc = new Document();
            final XMLOutputter outPutter = new XMLOutputter();
            final File settingsFile = File.createTempFile("settings_", ".xml");
            final Element domSettings = new Element("settings");

            domSettings.addContent(domServers);
            doc.setRootElement(domSettings);
            outPutter.setFormat(Format.getPrettyFormat());
            outPutter.output(doc, new FileWriter(settingsFile));
            return settingsFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SettingsXmlBuilder addServer(final String id, final String username, final String password) {
        final Element domServer = new Element("server");
        addElement(domServer, "id", id);
        addElement(domServer, "username", username);
        addElement(domServer, "password", password);
        domServers.addContent(domServer);
        return this;
    }

    private void addElement(final Element domServer, final String elementName, final String elementContent) {
        if (elementContent != null && !elementContent.isEmpty()) {
            domServer.addContent(new Element(elementName).addContent(elementContent));
        }
    }
}
