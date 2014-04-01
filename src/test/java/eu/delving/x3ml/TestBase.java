//===========================================================================
//    Copyright 2014 Delving B.V.
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//===========================================================================
package eu.delving.x3ml;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static eu.delving.x3ml.AllTests.*;
import static org.junit.Assert.assertTrue;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestBase {
    private final Logger log = Logger.getLogger(getClass());

    private void log(String title, String[] list) {
        log.info(title);
        int count = 0;
        for (String line : list) {
            log.info((count++) + " ) " + line);
        }
    }

    @Test
    public void testReadWrite() throws IOException {
        String xml = engine("/base/base.x3ml").toString();
        String[] lines = xml.split("\n");
        List<String> serialized = new ArrayList<String>();
        List<String> originalLines = IOUtils.readLines(resource("/base/base.x3ml"));
        List<String> original = new ArrayList<String>();
        boolean ignore = false;
        int index = 0;
        for (String orig : originalLines) {
            orig = orig.trim();
            if (orig.startsWith("<!--")) continue;
            if (orig.startsWith("<comments") || orig.startsWith("<info")) ignore = true;
            if (!ignore) {
                serialized.add(lines[index].trim());
                original.add(orig);
                index++;
            }
            if (orig.startsWith("</comments") || orig.startsWith("</info")) ignore = false;
        }
        Assert.assertEquals("Mismatch", StringUtils.join(original, "\n"), StringUtils.join(serialized, "\n"));
    }

    @Test
    public void testSimple() {
        X3MLEngine engine = engine("/base/base.x3ml");
        X3MLEngine.Output output = engine.execute(document("/base/base.xml"), policy("/base/base-gen-policy.xml"));
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = AllTests.xmlToNTriples("/base/base-rdf.xml");
//        log("Expected", expectedResult);
//        log("Actual", mappingResult);
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
        System.out.println(StringUtils.join(diff, "\n"));
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