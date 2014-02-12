package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("range")
public class Range {
    public String source;

    public Entity entity;

    @XStreamAlias("additional_node")
    public AdditionalNode additionalNode;

    public Comments comments;

    public void applyRange(Context context, Domain domain, Path path) {
        if (additionalNode != null) {
            additionalNode.apply(context, domain, path, entity);
        }
    }
}
