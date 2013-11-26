package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("comments")
public class Comments {

    public String rationale;

    public String alternatives;

    @XStreamAlias("typical_mistakes")
    public String typicalMistakes;

    @XStreamAlias("local_habits")
    public String localHabits;

    @XStreamAlias("link_to_cook_book")
    public String linkToCookbook;

    public String example;

    @XStreamAlias("comments_last_update")
    public String lastUpdate;
}
