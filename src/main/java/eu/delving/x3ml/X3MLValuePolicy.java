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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.delving.x3ml.X3ML.SourceType.QNAME;
import static eu.delving.x3ml.X3ML.SourceType.XPATH;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLValuePolicy implements X3ML.ValuePolicy {
    private static final Pattern BRACES = Pattern.compile("\\{[?;+#]?([^}]+)\\}");
    private Map<String, Template> templateMap = new TreeMap<String, Template>();

    public X3MLValuePolicy(InputStream inputStream) {
        Policy policy = (Policy) stream().fromXML(inputStream);
        for (Template template : policy.templates) {
            templateMap.put(template.name, template);
        }
    }

    @Override
    public X3ML.Value generateValue(String name, X3ML.ValueFunctionArgs args) {
        if (name == null) throw new X3MLException("URI function name missing");
        X3ML.Value value = new X3ML.Value();
        Template template = templateMap.get(name);
        if (template == null) throw new X3MLException("No template for " + name);
        X3ML.ArgValue qname = args.getArgValue("qname", QNAME);
        String localName = qname.qualifiedName.getLocalName();
        String namespaceUri = qname.qualifiedName.namespaceUri;
        try {
            UriTemplate uriTemplate = UriTemplate.fromTemplate(template.pattern);
            uriTemplate.set("localName", localName);
            for (String variableName : variablesFromPattern(template.pattern)) {
                if (!"localName".equals(variableName)) {
                    X3ML.ArgValue argValue = args.getArgValue(variableName, XPATH);
                    if (argValue == null || argValue.string == null) {
                        throw new X3MLException("Argument failure " + variableName);
                    }
                    uriTemplate.set(variableName, argValue.string);
                }
            }
            value.uri = namespaceUri + uriTemplate.expand();
            return value;
        }
        catch (MalformedUriTemplateException e) {
            throw new X3MLException("Malformed", e);
        }
        catch (VariableExpansionException e) {
            throw new X3MLException("Variable", e);
        }
    }

    // == the rest is for the XML form

    private static List<String> variablesFromPattern(String pattern) {
        Matcher braces = BRACES.matcher(pattern);
        List<String> variables = new ArrayList<String>();
        while (braces.find()) {
            Collections.addAll(variables, braces.group(1).split(","));
        }
        return variables;
    }

    private static XStream stream() {
        XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.processAnnotations(Policy.class);
        return xstream;
    }

    @XStreamAlias("uri-policy") // todo: change this
    public static class Policy {
        @XStreamImplicit
        List<Template> templates;
    }

    @XStreamAlias("template")
    public static class Template {
        @XStreamAsAttribute
        public String name;

        public String pattern;

        public String toString() {
            return pattern;
        }
    }


}
