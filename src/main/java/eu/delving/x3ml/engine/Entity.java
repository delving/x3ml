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

    public void apply(Context context, Domain domain, Path path) {
        if (exists == null || exists.evaluate(context)) {
            String uri = uriFunction.generateURI(context, domain, path);
//            context.triple()
        }
    }
}
