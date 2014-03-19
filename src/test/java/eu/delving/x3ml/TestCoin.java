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

    @Test
    public void testSimpleCoinExample() throws X3MLException {
        X3MLEngine engine = engine("/coin/coin1.x3ml");
        X3MLContext context = engine.execute(document("/coin/coin-input.xml"), new SimplePolicy());
        String[] mappingResult = context.toStringArray();
        String[] expectedResult = xmlToNTriples("/coin/coin1-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void testJoinExample() throws X3MLException {
        X3MLEngine engine = engine("/join/join1.x3ml");
        X3MLContext context = engine.execute(document("/coin/coin-input.xml"), new SimplePolicy());
        String[] mappingResult = context.toStringArray();
        String[] expectedResult = xmlToNTriples("/join/join1-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
    }

    @Test
    public void testJoinVariableExample() throws X3MLException {
        X3MLEngine engine = engine("/join/join2.x3ml");
        X3MLContext context = engine.execute(document("/coin/coin-input.xml"), new SimplePolicy());
        String[] mappingResult = context.toStringArray();
        String[] expectedResult = xmlToNTriples("/join/join2-rdf.xml");
        List<String> diff = compareNTriples(expectedResult, mappingResult);
        assertTrue("\n" + StringUtils.join(diff, "\n") + "\n", errorFree(diff));
        System.out.println(StringUtils.join(diff, "\n"));
    }

    private boolean errorFree(List<String> diff) {
        for (String line : diff) {
            if (line.startsWith("!")) {
                return false;
            }
        }
        return true;
    }


    private class SimplePolicy implements X3ML.ValuePolicy {
        private char uuidLetter = 'A';

        private String createUUID() {
            return "uuid:" + (uuidLetter++);
        }

        @Override
        public X3ML.Value generateValue(String name, X3ML.ValueFunctionArgs arguments) {
            X3ML.Value value = new X3ML.Value();
            if ("UUID".equals(name)) {
                value.uri = createUUID();
//                System.out.println(arguments + ":\n"+ value.uri);
            }
            else if ("Literal".equals(name)) {
                X3ML.ArgValue literalXPath = arguments.getArgValue(null, X3ML.SourceType.XPATH);
                if (literalXPath == null) {
                    throw new X3MLException("Argument failure: need one argument");
                }
                if (literalXPath.string == null || literalXPath.string.isEmpty()) {
                    throw new X3MLException("Argument failure: empty argument");
                }
                value.literal = literalXPath.string;
            }
            else if ("Constant".equals(name)) {
                X3ML.ArgValue constant = arguments.getArgValue(null, X3ML.SourceType.LITERAL);
                if (constant == null) {
                    throw new X3MLException("Argument failure: need one argument");
                }
                value.literal = constant.string;
            }
            else {
                throw new X3MLException("Unknown function: " + name);
            }
            return value;
        }
    }
}
