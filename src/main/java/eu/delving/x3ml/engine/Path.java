package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("path")
public class Path {
    public String source;

    public Property property;

    @XStreamAlias("internal_node")
    @XStreamImplicit
    public List<InternalNode> internalNode;

    public Comments comments;

    public void apply(Context context, Domain domain) {
        property.apply(context, domain);
        if (internalNode != null) {
            for (InternalNode node : internalNode) {
                node.apply(context, domain, property);
            }
        }
    }
}
