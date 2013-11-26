package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("metadata")
public class Metadata {
    @XStreamAsAttribute
    public String version;

    public String title;

    public String description;
}
