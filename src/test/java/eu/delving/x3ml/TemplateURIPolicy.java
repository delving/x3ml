package eu.delving.x3ml;

import com.damnhandy.uri.template.MalformedUriTemplateException;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.VariableExpansionException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class TemplateURIPolicy implements X3ML.URIPolicy {
    private Map<String, Template> templateMap = new TreeMap<String, Template>();

    public TemplateURIPolicy(InputStream inputStream) {
        Policy policy = (Policy) stream().fromXML(inputStream);
        for (Template template: policy.templates) {
            templateMap.put(template.name, template);
        }
    }

    @Override
    public String generateUri(String name, X3ML.URIArguments arguments) {
        if (name == null) name = arguments.getClassName();
        Template template = templateMap.get(name);
        if (template == null) throw new X3MLException("No template for "+name);
        try {
            UriTemplate uriTemplate = UriTemplate.fromTemplate(template.pattern);
            uriTemplate.set("className", arguments.getClassName());
            for (String variable : variables(template.variables)) {
                uriTemplate.set(variable, arguments.getArgument(variable));
            }
            return uriTemplate.expand();
        }
        catch (MalformedUriTemplateException e) {
            throw new X3MLException("Malformed", e);
        }
        catch (VariableExpansionException e) {
            throw new X3MLException("Variable", e);
        }
    }

    // == the rest is for the XML form

    private static String [] variables(String commaDelimited) {
        return commaDelimited.split(", *");
    }

    private static XStream stream() {
        XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.processAnnotations(Policy.class);
        return xstream;
    }

    @XStreamAlias("uri-policy")
    public static class Policy {
        @XStreamImplicit
        List<Template> templates;
    }

    @XStreamAlias("template")
    public static class Template {
        @XStreamAsAttribute
        public String name;

        public String pattern;

        public String variables;

        public String toString() {
            return pattern;
        }
    }


}
