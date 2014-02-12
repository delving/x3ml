package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("internal_node")
public class InternalNode {
    public Entity entity;
    public Property property;

    public void applyInternalNode(Context context, Domain domain, Property contextProperty) {
        // todo: implement
    }
}
