package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("property")
public class Property {
    @XStreamAsAttribute
    public CRMProperty tag;

    @XStreamAlias("exists")
    public Exists exists;

    public void apply(Context context, Domain domain) {
        // todo: implement
    }
}
