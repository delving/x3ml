package eu.delving.x3ml;

import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static eu.delving.x3ml.TestHelper.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestBMData {

    @Test
    public void testFirstSteps() throws X3MLException {
        X3MLContext context = context("/bm/BM20.xml", "/bm/uri-policy.xml");
        engine("/bm/BM20.x3ml").execute(context);
        context.write(System.out);
    }
}
