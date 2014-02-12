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

    public void applyPath(Context context, Domain domain) {
        String propertyUri = property.getPropertyURI(context, domain);
        if (internalNode != null) {
            for (InternalNode node : internalNode) {
                node.applyInternalNode(context, domain, property);
            }
        }
    }
}
