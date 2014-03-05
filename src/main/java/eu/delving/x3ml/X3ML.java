package eu.delving.x3ml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.*;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;

import javax.xml.namespace.NamespaceContext;
import java.util.List;

import static eu.delving.x3ml.X3MLContext.ValueContext;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public interface X3ML {

    public enum SourceType {
        XPATH,
        QNAME,
        LITERAL
    }

    @XStreamAlias("x3ml")
    public static class Root extends Visible {

        @XStreamAsAttribute
        public String version;

        @XStreamAsAttribute
        @XStreamAlias("source_type")
        public SourceType sourceType;

        public List<MappingNamespace> namespaces;

        @XStreamImplicit
        public List<Mapping> mappings;

        public void apply(X3MLContext context) {
            for (Mapping mapping : mappings) {
                mapping.apply(context);
            }
        }
    }

    @XStreamAlias("mapping")
    public static class Mapping extends Visible {

        public Domain domain;

        @XStreamImplicit
        public List<Link> links;

        public void apply(X3MLContext context) {
            for (X3MLContext.DomainContext domainContext : context.createDomainContexts(domain)) {
                for (Link link : links) {
                    link.apply(domainContext);
                }
            }
        }
    }

    @XStreamAlias("link")
    public static class Link extends Visible {

        public Path path;

        public Range range;

        public void apply(X3MLContext.DomainContext context) {
            for (X3MLContext.PathContext pathContext : context.createPathContexts(path)) {
                for (X3MLContext.RangeContext rangeContext : pathContext.createRangeContexts(range)) {
                    rangeContext.link();
                }
            }
        }
    }

    @XStreamAlias("namespace")
    public static class MappingNamespace extends Visible {
        @XStreamAsAttribute
        public String prefix;
        @XStreamAsAttribute
        public String uri;
    }

    @XStreamAlias("domain")
    public static class Domain extends Visible {
        public Source source;
        public Target target;
        public Comments comments;
    }

    @XStreamAlias("source")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    public static class Source extends Visible {
        public String expression;
    }

    @XStreamAlias("target")
    public static class Target extends Visible {

        @XStreamAlias("if")
        public Condition condition;

        @XStreamAlias("entity")
        public EntityElement entityElement;

        @XStreamAlias("property")
        public PropertyElement propertyElement;

        public Intermediate intermediate;

        @XStreamImplicit
        public List<Additional> additionals;
    }

    @XStreamAlias("path")
    public static class Path extends Visible {

        public Source source;

        public Target target;

        public Comments comments;
    }

    @XStreamAlias("range")
    public static class Range extends Visible {

        public Source source;

        public Target target;

        public Comments comments;
    }

    @XStreamAlias("additional")
    public static class Additional extends Visible {

        @XStreamAlias("property")
        public PropertyElement propertyElement;

        @XStreamAlias("entity")
        public EntityElement entityElement;
    }

    @XStreamAlias("intermediate")
    public static class Intermediate extends Visible {

        @XStreamAlias("entity")
        public EntityElement entityElement;

        @XStreamAlias("property")
        public PropertyElement propertyElement;
    }

    @XStreamAlias("if")
    public static class Condition extends Visible {
        public Narrower narrower;
        public Exists exists;
        public Equals equals;
        public AndCondition and;
        public OrCondition or;
        public NotCondition not;

        private static class Outcome {
            final ValueContext context;
            boolean failure;

            private Outcome(ValueContext context) {
                this.context = context;
            }

            Outcome evaluate(YesOrNo yesOrNo) {
                if (yesOrNo != null && !failure && !yesOrNo.yes(context)) {
                    failure = true;
                }
                return this;
            }
        }

        public boolean failure(ValueContext context) {
            return new Outcome(context)
                            .evaluate(narrower)
                            .evaluate(exists)
                            .evaluate(equals)
                            .evaluate(and)
                            .evaluate(or)
                            .evaluate(not).failure;
        }

    }

    interface YesOrNo {
        boolean yes(ValueContext context);
    }

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    @XStreamAlias("exists")
    public static class Exists extends Visible implements YesOrNo {

        public String expression;

        @Override
        public boolean yes(ValueContext context) {
            return context.evaluate(expression).length() > 0;
        }
    }

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    @XStreamAlias("equals")
    public static class Equals extends Visible implements YesOrNo {

        @XStreamAsAttribute
        public String value;

        public String expression;

        @Override
        public boolean yes(ValueContext context) {
            return value.equals(context.evaluate(expression));
        }
    }

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    @XStreamAlias("narrower")
    public static class Narrower extends Visible implements YesOrNo {

        @XStreamAsAttribute
        public String value;

        public String expression;

        @Override
        public boolean yes(ValueContext context) {
            return true;
        }
    }

    @XStreamAlias("and")
    public static class AndCondition extends Visible implements YesOrNo {

        @XStreamImplicit
        List<Condition> list;

        @Override
        public boolean yes(ValueContext context) {
            boolean result = true;
            for (Condition condition : list) {
                if (condition.failure(context)) result = false;
            }
            return result;
        }
    }

    @XStreamAlias("or")
    public static class OrCondition extends Visible implements YesOrNo {

        @XStreamImplicit
        List<Condition> list;

        @Override
        public boolean yes(ValueContext context) {
            boolean result = false;
            for (Condition condition : list) {
                if (!condition.failure(context)) result = true;
            }
            return result;
        }
    }

    @XStreamAlias("not")
    public static class NotCondition extends Visible implements YesOrNo {

        @XStreamAlias("if")
        Condition condition;

        @Override
        public boolean yes(ValueContext context) {
            return condition.failure(context);
        }
    }

    @XStreamAlias("property")
    public static class PropertyElement extends Visible {

        @XStreamAlias("qname")
        public QualifiedName qualifiedName;

        public QualifiedName getPropertyClass() {
            if (qualifiedName == null) throw new X3MLException("Missing class element");
            return qualifiedName;
        }
    }

    @XStreamAlias("entity")
    public static class EntityElement extends Visible {

        @XStreamAlias("qname")
        public QualifiedName qualifiedName;

        @XStreamAlias("value_generator")
        public ValueGenerator valueGenerator;

        public Value getValue(ValueContext context) {
            return context.generateValue(valueGenerator, this);
        }
    }

    @XStreamAlias("qname")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"tag"})
    public static class QualifiedName extends Visible {

        public String tag;

        @XStreamOmitField
        public String namespaceUri;

        public String getPrefix() {
            int colon = tag.indexOf(':');
            if (colon < 0) throw new X3MLException("Unqualified tag " + tag);
            return tag.substring(0, colon);
        }

        public String getLocalName() {
            int colon = tag.indexOf(':');
            if (colon < 0) throw new X3MLException("Unqualified tag " + tag);
            return tag.substring(colon + 1);
        }
    }

    @XStreamAlias("comments")
    public static class Comments extends Visible {

        @XStreamImplicit
        public List<Comment> comments;

    }

    @XStreamAlias("comment")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"content"})
    public static class Comment extends Visible {
        @XStreamAsAttribute
        public String type;

        public String content;
    }

    @XStreamAlias("value_generator")
    public static class ValueGenerator extends Visible {
        @XStreamAsAttribute
        public String name;

        @XStreamImplicit
        public List<ValueFunctionArg> args;
    }

    @XStreamAlias("arg")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"value"})
    public static class ValueFunctionArg extends Visible {
        @XStreamAsAttribute
        public String name;

        public String value;
    }

    public static class ArgValue {
        public String string;
        public QualifiedName qualifiedName;

        public QualifiedName setQName(String qname, NamespaceContext namespaceContext) {
            qualifiedName = new QualifiedName();
            qualifiedName.tag = qname;
            qualifiedName.namespaceUri = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
            return qualifiedName;
        }

        public String toString() {
            if (string != null) {
                return "ArgValue(" + string + ")";
            }
            else if (qualifiedName != null) {
                return "ArgValue(" + qualifiedName + ")";
            }
            else {
                return "ArgValue?";
            }
        }
    }

    public interface ValueFunctionArgs {
        ArgValue getArgValue(String name, SourceType sourceType);
    }

    public static class Value {
        public String uri;
        public QualifiedName labelQName;
        public String labelValue;

        public String toString() {
            if (uri != null && labelQName != null) {
                return "Value(" + uri + ", " + labelQName + " := " + labelValue + ")";
            }
            else if (uri != null) {
                return "Value(" + uri + ")";
            }
            else if (labelQName != null) {
                return "Value(" + labelQName + " := " + labelValue + ")";
            }
            else {
                return "Value?";
            }
        }
    }

    public interface ValuePolicy {
        Value generateValue(String name, ValueFunctionArgs arguments);
    }

    static class Visible {
        public String toString() {
            return Helper.toString(this);
        }
    }

    static class Helper {
        static String toString(Object thing) {
            return "\n" + stream().toXML(thing);
        }

        static XStream stream() {
            XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
            xstream.setMode(XStream.NO_REFERENCES);
            xstream.processAnnotations(Root.class);
            return xstream;
        }
    }
}
