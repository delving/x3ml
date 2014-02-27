package eu.delving.x3ml;

import org.junit.Test;

import static eu.delving.x3ml.TestHelper.context;
import static eu.delving.x3ml.TestHelper.engine;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestCoin {

    @Test
    public void testSimpleCoinExample() throws X3MLException {
        X3MLContext context = context("/coin/coin-input.xml", "/coin/coin1-uri-policy.xml");
        engine("/coin/coin1.x3ml").execute(context);
        context.write(System.out);
    }
}
