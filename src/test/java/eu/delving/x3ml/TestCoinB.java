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

import eu.delving.x3ml.engine.Generator;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.List;

import static eu.delving.x3ml.AllTests.*;
import static org.junit.Assert.assertTrue;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestCoinB {
    private final Logger log = Logger.getLogger(getClass());
    private final Generator VALUE_POLICY = X3MLGeneratorPolicy.load(null, X3MLGeneratorPolicy.createUUIDSource(1));

    @Test
    public void test10Join() {
        X3MLEngine engine = engine("/coin_b/10-join.x3ml");
        Generator policy = X3MLGeneratorPolicy.load(resource("/coin_a/00-generator-policy.xml"), X3MLGeneratorPolicy.createUUIDSource(2));
        X3MLEngine.Output output = engine.execute(document("/coin_b/01-coin-input-simplified.xml"), policy);
        String[] mappingResult = output.toStringArray();
//        output.writeXML(System.out);
        String[] expectedResult = xmlToNTriples("/coin_b/10-join-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\nLINES:"+ diff.size() + "\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }


}
