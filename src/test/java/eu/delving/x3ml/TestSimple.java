package eu.delving.x3ml;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static eu.delving.x3ml.AllTests.*;
import static org.junit.Assert.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestSimple {
    private final Logger log = Logger.getLogger(getClass());

    private void log(String title, String[] list) {
        log.info(title);
        int count = 0;
        for (String line : list) {
            log.info((count++) + " ) " + line);
        }
    }

    @Test
    public void testReadWrite() throws IOException, X3MLException {
        String xml = engine("/simple/simple.x3ml").toString();
        String[] fresh = xml.split("\n");
        List<String> original = IOUtils.readLines(resource("/simple/simple.x3ml"));
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
        X3MLEngine engine = engine("/simple/simple.x3ml");
        X3MLContext context = engine.execute(document("/simple/simple.xml"), policy("/simple/simple-value-policy.xml"));
        String[] mappingResult = context.toStringArray();
        String[] expectedResult = AllTests.xmlToNTriples("/simple/simple-rdf.xml");
        log("result", mappingResult);
        log("expected", expectedResult);
        assertArrayEquals("Does not match expected", expectedResult, mappingResult);
    }
}

// to ignore stuff:
//XStream xStream = new XStream() {
//    @Override
//    protected MapperWrapper wrapMapper(MapperWrapper next) {
//        return new MapperWrapper(next) {
//            @Override
//            public boolean shouldSerializeMember(Class definedIn, String fieldName) {
//                if (definedIn == Object.class) {
//                    try {
//                        return this.realClass(fieldName) != null;
//                    } catch(Exception e) {
//                        return false;
//                    }
//                } else {
//                    return super.shouldSerializeMember(definedIn, fieldName);
//                }
//            }
//        };
//    }
//};