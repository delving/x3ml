package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("domain")
public class Domain {
    public String source;

    public Entity entity;

    public Comments comments;

    public void apply(Context context) {
        entity.apply(context, this, null);
        // todo: implement
    }
}
