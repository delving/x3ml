package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("link")
public class Link {
    public Path path;

    public Range range;

    public void applyLink(Context context, Domain domain) {
        path.applyPath(context, domain);
        range.applyRange(context, domain, path);
    }
}
