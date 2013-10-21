package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("uri_function")
public class URIFunction {
    @XStreamAsAttribute
    public String name;

    @XStreamImplicit
    public List<URIFunctionArg> args;

    @XStreamOmitField
    private List<String> argList = new ArrayList<String>();

    public String generateURI(Context context, Domain domain, Path path) {
        return "URI"; // todo
    }
}
