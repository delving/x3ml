package eu.delving.x3ml;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestX3MLEngine {

    @Test
    public void testReadWrite() throws IOException, X3MLException {
        String xml = engine("/simple/simple-x3ml.xml").toString();
        String[] fresh = xml.split("\n");
        List<String> original = IOUtils.readLines(resource("/simple/simple-x3ml.xml"));
        int index = 0;
        for (String originalLine : original) {
            originalLine = originalLine.trim();
            String freshLine = fresh[index].trim();
            Assert.assertEquals("Line " + index, originalLine, freshLine);
            index++;
        }
    }

    @Test
    public void testSimple() throws X3MLException {
        X3MLContext job = job("/simple/simple-input.xml");
        engine("/simple/simple-x3ml.xml").execute(job);
    }

    // ==== helpers ====

    private static X3MLEngine engine(String path) throws X3MLException {
        return X3MLEngine.load(resource(path));
    }

    private static X3MLContext job(String path) throws X3MLException {
        return X3MLContext.create(document(path), new URIPolicy());
    }

    private static Element document(String path) throws X3MLException {
        try {
            return XmlSerializer.documentBuilderFactory().newDocumentBuilder().parse(resource(path)).getDocumentElement();
        }
        catch (Exception e) {
            throw new X3MLException("Unable to parse "+path);
        }
    }

    private static InputStream resource(String path) {
        return TestX3MLEngine.class.getResourceAsStream(path);
    }
}
