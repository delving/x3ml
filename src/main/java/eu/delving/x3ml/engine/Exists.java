package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamConverter(value = ToAttributedValueConverter.class, strings = {"xpath"})
@XStreamAlias("exists")
public class Exists {
    @XStreamAsAttribute
    public String value;

    public String xpath;

    public boolean evaluate(Context context) {
        return true; // todo
    }
}
