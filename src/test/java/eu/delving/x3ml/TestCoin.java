package eu.delving.x3ml;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.List;

import static eu.delving.x3ml.AllTests.*;
import static org.junit.Assert.assertTrue;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestCoin {
    private final Logger log = Logger.getLogger(getClass());
    private final X3ML.ValuePolicy VALUE_POLICY = X3MLValuePolicy.load(null);

    @Test
    public void testSimpleCoinExample() throws X3MLException {
        X3MLEngine engine = engine("/coin/coin1.x3ml");
        X3MLContext context = engine.execute(document("/coin/coin-input.xml"), VALUE_POLICY);
        String[] mappingResult = context.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin/coin1-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void testJoinExample() throws X3MLException {
        X3MLEngine engine = engine("/join/join1.x3ml");
        X3MLContext context = engine.execute(document("/coin/coin-input.xml"), VALUE_POLICY);
        String[] mappingResult = context.toStringArray();
        String[] expectedResult = xmlToNTriples("/join/join1-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void testJoinVariableExample() throws X3MLException {
        X3MLEngine engine = engine("/join/join2.x3ml");
        X3MLContext context = engine.execute(document("/coin/coin-input.xml"), VALUE_POLICY);
        String[] mappingResult = context.toStringArray();
        String[] expectedResult = xmlToNTriples("/join/join2-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
        System.out.println(StringUtils.join(diff, "\n"));
    }
}
