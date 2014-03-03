package eu.delving.x3ml;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import static eu.delving.x3ml.AllTests.document;
import static eu.delving.x3ml.AllTests.engine;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestCoin {
    private final Logger log = Logger.getLogger(getClass());
    private Queue<String> uuidQueue = new LinkedBlockingQueue<String>();

    private void log(String title, String[] list) {
        log.info(title);
        for (String line : list) {
            log.info(line);
        }
    }

    @Test
    public void testSimpleCoinExample() throws X3MLException {
        uuidQueue.add("uuid:A");
        uuidQueue.add("uuid:B");
        X3MLEngine engine = engine("/coin/coin1.x3ml");
        X3MLContext context = engine.execute(document("/coin/coin-input.xml"), new X3ML.ValuePolicy() {
            @Override
            public X3ML.Value generateValue(String name, X3ML.ValueFunctionArgs arguments) {
                X3ML.Value value = new X3ML.Value();
                if ("UUID".equals(name)) {
                    value.uri = uuidQueue.remove();
                }
                else if ("UUID_Label".equals(name)) {
                    X3ML.ArgValue labelQName = arguments.getArgValue("labelQName", X3ML.SourceType.QNAME);
                    if (labelQName == null || labelQName.qualifiedName == null) {
                        throw new X3MLException("Argument failure: labelQName");
                    }
                    value.uri = uuidQueue.remove();
                    value.labelQName = labelQName.qualifiedName;
                    X3ML.ArgValue labelXPath = arguments.getArgValue("labelXPath", X3ML.SourceType.XPATH);
                    if (labelXPath == null || labelXPath.string == null) {
                        throw new X3MLException("Argument failure: labelXPath");
                    }
                    value.labelValue = labelXPath.string;
                }
                return value;
            }
        });
        String [] mappingResult = context.toStringArray();
        String [] expectedResult = AllTests.xmlToNTriples("/coin/coin1-rdf.xml");
        log("result", mappingResult);
        log("expected", expectedResult);
        Assert.assertArrayEquals("Does not match expected", expectedResult, mappingResult);
    }


}
