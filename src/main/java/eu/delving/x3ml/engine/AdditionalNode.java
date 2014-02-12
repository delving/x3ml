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
        String propertyUri = property.getPropertyURI(context, domain);
        String rangeUri = entity.generateRangeUri(context, domain, path);
        if (propertyUri != null && rangeUri != null) {
            // todo: this could be prettier
            Context.GraphEntity domainEntity = context.entity(domain.entity.tag.toString(), domain.source, context.getDomainURI());
            Context.GraphEntity rangeEntity = context.entity(entity.tag.toString(), "?", rangeUri);
            Context.GraphTriple triple = context.triple(domainEntity, propertyUri, rangeEntity);
        }
    }
}
