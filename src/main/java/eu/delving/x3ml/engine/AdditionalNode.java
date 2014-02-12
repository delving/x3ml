package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("additional_node")
public class AdditionalNode {

    public Property property;

    public Entity entity;

    public boolean apply(Context context, Domain domain, Path path, Entity contextEntity) {
        String propertyURI = property.getPropertyURI(context, domain);
        if (context.setProperty(propertyURI)) {
            if (context.setRange(entity, path)) {
                context.createTriple();
                return true;
            }
        }
        return false;
    }
}
