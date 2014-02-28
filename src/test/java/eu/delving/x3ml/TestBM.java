package eu.delving.x3ml;

import org.junit.Test;

import static eu.delving.x3ml.AllTests.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestBM {

    @Test
    public void testFirstSteps() throws X3MLException {
        X3MLContext context = context("/bm/BM20.xml", "/bm/BM20-uri-policy.xml");
        engine("/bm/BM20.x3ml").execute(context);
        context.write(System.out);
    }
}
