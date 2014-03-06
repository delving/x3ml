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

import static eu.delving.x3ml.X3ML.SourceType.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLValuePolicy implements X3ML.ValuePolicy {
    private static final Pattern BRACES = Pattern.compile("\\{[?;+#]?([^}]+)\\}");
    private Map<String, Generator> templateMap = new TreeMap<String, Generator>();

    public static X3MLValuePolicy load(InputStream inputStream) {
        return new X3MLValuePolicy(inputStream);
    }

    private X3MLValuePolicy(InputStream inputStream) {
        ValuePolicy policy = (ValuePolicy) stream().fromXML(inputStream);
        for (Generator generator : policy.generators) {
            templateMap.put(generator.name, generator);
        }
    }

    @Override
    public X3ML.Value generateValue(String name, X3ML.ValueFunctionArgs args) {
        if (name == null) throw new X3MLException("URI function name missing");
        X3ML.Value value = new X3ML.Value();
        Generator generator = templateMap.get(name);
        if (generator == null) throw new X3MLException("No template for " + name);
        X3ML.ArgValue qname = args.getArgValue("qname", QNAME);
        String localName = qname.qualifiedName.getLocalName();
        String namespaceUri = qname.qualifiedName.namespaceUri;
        try {
            UriTemplate uriTemplate = UriTemplate.fromTemplate(generator.pattern);
            uriTemplate.set("localName", localName);
            Set<String> literals = new TreeSet<String>();
            if (generator.literals != null) literals.addAll(Arrays.asList(generator.literals.split(",")));
            for (String variableName : variablesFromPattern(generator.pattern)) {
                if ("localName".equals(variableName)) continue;
                boolean isLiteral = literals.contains(variableName);
                X3ML.ArgValue argValue = args.getArgValue(variableName, isLiteral ? LITERAL :XPATH);
                if (argValue == null || argValue.string == null) {
                    throw new X3MLException("Argument failure " + variableName);
                }
                uriTemplate.set(variableName, argValue.string);
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
        xstream.processAnnotations(ValuePolicy.class);
        return xstream;
    }

    @XStreamAlias("value-policy")
    public static class ValuePolicy {
        @XStreamImplicit
        List<Generator> generators;
    }

    @XStreamAlias("generator")
    public static class Generator {
        @XStreamAsAttribute
        public String name;

        public String pattern;

        public String literals;

        public String toString() {
            return pattern;
        }
    }


}
