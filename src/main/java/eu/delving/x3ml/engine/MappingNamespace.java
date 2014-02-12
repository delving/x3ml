package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("namespace")
public class MappingNamespace {
    @XStreamAsAttribute
    public String prefix;

    @XStreamAsAttribute
    public String uri;

    public String toString() {
        return prefix + ":" + uri;
    }
}
