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

public class TestCoinA {
    private final Logger log = Logger.getLogger(getClass());
    private final Generator VALUE_POLICY = X3MLGeneratorPolicy.load(null, X3MLGeneratorPolicy.createUUIDSource(1));

    @Test
    public void testSimpleCoinExample() {
        X3MLEngine engine = engine("/coin_a/01-coin-simple.x3ml");
        X3MLEngine.Output output = engine.execute(document("/coin_a/00-coin-input.xml"), VALUE_POLICY);
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/01-coin-simple-rdf.xml");
//        System.out.println(StringUtils.join(expectedResult, "\n"));
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void test02Join() {
        X3MLEngine engine = engine("/coin_a/02-join.x3ml");
        X3MLEngine.Output output = engine.execute(document("/coin_a/00-coin-input.xml"), VALUE_POLICY);
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/02-join-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void test03Join() {
        X3MLEngine engine = engine("/coin_a/03-join.x3ml");
        X3MLEngine.Output output = engine.execute(document("/coin_a/00-coin-input.xml"), VALUE_POLICY);
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/03-join-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
//        System.out.println(StringUtils.join(diff, "\n"));
    }

    @Test
    public void testAppellation() {
        X3MLEngine engine = engine("/coin_a/04-appell.x3ml");
        X3MLEngine.Output output = engine.execute(document("/coin_a/00-coin-input.xml"), policy("/coin_a/00-generator-policy.xml"));
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/04-appell-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void testMulti() {
        X3MLEngine engine = engine("/coin_a/05-multi.x3ml");
        X3MLEngine.Output output = engine.execute(document("/coin_a/00-coin-input.xml"), policy("/coin_a/00-generator-policy.xml"));
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/05-multi-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void testIf() {
        X3MLEngine engine = engine("/coin_a/06-if.x3ml");
        Generator policy = X3MLGeneratorPolicy.load(resource("/coin_a/00-generator-policy.xml"), X3MLGeneratorPolicy.createUUIDSource(1));
        X3MLEngine.Output output = engine.execute(document("/coin_a/00-coin-input.xml"), policy);
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/06-if-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void test07Date() {
        X3MLEngine engine = engine("/coin_a/07-date.x3ml");
        X3MLEngine.Output output = engine.execute(document("/coin_a/00-coin-input.xml"), policy("/coin_a/00-generator-policy.xml"));
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/07-date-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void testCRMDig() {
        X3MLEngine engine = engine("/coin_a/08-crmdig.x3ml");
        Generator policy = X3MLGeneratorPolicy.load(resource("/coin_a/00-generator-policy.xml"), X3MLGeneratorPolicy.createUUIDSource(4));
        X3MLEngine.Output output = engine.execute(document("/coin_a/00-coin-input.xml"), policy);
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/08-crmdig-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void testOrderA() {
        X3MLEngine engine = engine("/coin_a/09-order-A.x3ml");
        Generator policy = X3MLGeneratorPolicy.load(resource("/coin_a/00-generator-policy.xml"), X3MLGeneratorPolicy.createUUIDSource(4));
        X3MLEngine.Output output = engine.execute(document("/coin_a/02-coin-input.xml"), policy);
//        output.writeXML(System.out);
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/09-order-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void testOrderB() {
        X3MLEngine engine = engine("/coin_a/09-order-B.x3ml");
        Generator policy = X3MLGeneratorPolicy.load(resource("/coin_a/00-generator-policy.xml"), X3MLGeneratorPolicy.createUUIDSource(4));
        X3MLEngine.Output output = engine.execute(document("/coin_a/02-coin-input.xml"), policy);
//        output.writeXML(System.out);
        String[] mappingResult = output.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin_a/09-order-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }
}
