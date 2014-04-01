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

import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static eu.delving.x3ml.AllTests.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestLido07 {
    private final Logger log = Logger.getLogger(getClass());
    private Queue<String> uuidQueue = new LinkedBlockingQueue<String>();

    private void log(String title, String[] list) {
        log.info(title);
        for (String line : list) {
            log.info(line);
        }
    }

    @Test
    public void testLIDOExample() {
        uuidQueue.add("uuid:A");
        uuidQueue.add("uuid:B");
        X3MLEngine engine = engine("/lido07/lido07.x3ml");
        X3MLEngine.Output output = engine.execute(
                document("/lido07/lido07.xml"),
                policy("/lido07/lido07-gen-policy.xml")
        );
        output.writeXML(System.out);
//        String [] mappingResult = context.toStringArray();
//        String [] expectedResult = AllTests.xmlToNTriples("/coin/coin1-rdf.xml");
//        log("result", mappingResult);
//        log("expected", expectedResult);
//        assertArrayEquals("Does not match expected", expectedResult, mappingResult);
    }


}
