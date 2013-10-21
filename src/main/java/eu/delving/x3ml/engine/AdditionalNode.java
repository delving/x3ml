package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("additional_node")
public class AdditionalNode {

    public Property property;

    public Entity entity;

    public void apply(Context context, Domain domain, Path path, Entity contextEntity) {
        property.apply(context, domain);
        entity.apply(context, domain, path);
    }
}
