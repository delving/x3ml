package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("entity")
public class Entity {
    @XStreamAsAttribute
    public CRMEntity tag;

    @XStreamAsAttribute
    public String binding;

    @XStreamAlias("exists")
    public Exists exists;

    @XStreamAlias("uri_function")
    public URIFunction uriFunction;

    public String generateDomainURI(Context context, Domain domain) {
        if (exists != null && !exists.evaluate(context)) return null;
        return uriFunction.generateURI(context, domain);
    }

    public String generateRangeUri(Context context, Entity domainEntity, Path path) {
        if (exists != null && !exists.evaluate(context)) return null;
        return uriFunction.generateURI(context, domainEntity, path);
    }
}
