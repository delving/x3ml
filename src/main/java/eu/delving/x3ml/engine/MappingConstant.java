package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("constant")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = {"content"})
public class MappingConstant {
    @XStreamAsAttribute
    public String name;

    public String content;

    public String toString() {
        return content;
    }
}
