package eu.delving.x3ml;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestX3MLEngine {

    @Test
    public void testReadWrite() throws IOException, X3MLException {
        URL mappingFile = getClass().getResource("/lido/lido-to-crm.xml");
        X3MLEngine engine = X3MLEngine.create(mappingFile.openStream());
        engine.addNamespace("lido", "http://www.lido-schema.org");
        String xml = engine.toString();
        String[] fresh = xml.split("\n");
        List<String> original = IOUtils.readLines(mappingFile.openStream());
        int index = 0;
        for (String originalLine : original) {
            originalLine = originalLine.trim();
            String freshLine = fresh[index].trim();
            Assert.assertEquals("Line " + index, originalLine, freshLine);
            index++;
        }
    }

    @Test
    public void testSimpleCase() throws IOException, X3MLException, ParserConfigurationException, SAXException {
        URL mappingFile = getClass().getResource("/simple/simple-x3.xml");
        X3MLEngine engine = X3MLEngine.create(mappingFile.openStream());
        URL inputFile = getClass().getResource("/simple/simple-input.xml");
        Document inputDocument = XmlSerializer.documentBuilderFactory().newDocumentBuilder().parse(inputFile.openStream());
        String inputXml = new XmlSerializer().toXml(inputDocument.getDocumentElement(), false);
        System.out.println(inputXml);
        String graph = engine.extractTriples(inputDocument.getDocumentElement());
        System.out.println(graph);
    }
}
