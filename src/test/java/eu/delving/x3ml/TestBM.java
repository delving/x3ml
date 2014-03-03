package eu.delving.x3ml;

import org.junit.Test;

import static eu.delving.x3ml.AllTests.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestBM {

    @Test
    public void testFirstSteps() throws X3MLException {
        X3MLEngine engine = engine("/bm/BM20.x3ml");
        X3MLContext context = engine.execute(document("/bm/BM20.xml"), policy("/bm/BM20-value-policy.xml"));
        context.write(System.out);
    }
}
