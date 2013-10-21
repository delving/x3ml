package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("path")
public class Path {
    public String source;

    public Property property;

    @XStreamAlias("internal_node")
    public InternalNode internalNode;

    public void apply(Context context, Domain domain) {
        property.apply(context, domain);
        if (internalNode != null) {
            internalNode.apply(context, domain, property);
        }
    }
}
