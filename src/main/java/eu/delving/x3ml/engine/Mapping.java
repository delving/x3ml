package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.util.List;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("mapping")
public class Mapping {
    public Domain domain;

    @XStreamImplicit
    public List<Link> links;

    public void applyMapping(Context context) {
        if (domain.applyDomain(context)) {
            for (Link link: links) {
                link.applyLink(context, domain);
            }
        }
    }
}
