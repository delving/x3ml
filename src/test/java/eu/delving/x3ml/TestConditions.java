package eu.delving.x3ml;

import com.sun.deploy.util.StringUtils;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static eu.delving.x3ml.AllTests.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TestConditions implements X3ML {
    private final Logger log = Logger.getLogger(getClass());

    private void log(String title, String[] list) {
        log.info(title);
        int count = 0;
        for (String line : list) {
            log.info((count++) + " ) " + line);
        }
    }

    @Test
    public void testSerializations() {
        testSerialization(new String[] {
                "<if>",
                "  <exists>gumby</exists>",
                "</if>"
        });
        testSerialization(new String[] {
                "<if>",
                "  <equals value=\"pokey\">gumby</equals>",
                "</if>"
        });
        testSerialization(new String[] {
                "<if>",
                "  <narrower value=\"pokey\">gumby</narrower>",
                "</if>"
        });
        testSerialization(new String[] {
                "<if>",
                "  <and>",
                "    <if>",
                "      <exists>gumby</exists>",
                "    </if>",
                "    <if>",
                "      <equals value=\"pokey\">gumby</equals>",
                "    </if>",
                "    <if>",
                "      <not>",
                "        <if>",
                "          <exists>gumby</exists>",
                "        </if>",
                "      </not>",
                "    </if>",
                "  </and>",
                "</if>"
        });
    }

    private void testSerialization(String[] x3ml) {
        String x3mlString = toString(x3ml);
        Condition condition = (Condition) stream().fromXML(x3mlString);
        String check = stream().toXML(condition);
        assertEquals("serialization problem", x3mlString, check);
    }

    private static String toString(String [] array) {
        return StringUtils.join(Arrays.asList(array), "\n");
    }

    private static XStream stream() {
        XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.processAnnotations(X3ML.Condition.class);
        return xstream;
    }


}