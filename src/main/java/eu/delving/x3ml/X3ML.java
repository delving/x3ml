package eu.delving.x3ml;

import com.thoughtworks.xstream.annotations.*;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

import javax.xml.namespace.NamespaceContext;
import java.util.List;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public interface X3ML {

    public enum SourceType {
        XPATH,
        QNAME,
        LITERAL
    }

    @XStreamAlias("mappings")
    public static class Mappings {

        @XStreamAsAttribute
        public String version;

        @XStreamAsAttribute
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
    public static class Mapping {
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
    public static class Link {

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
    public static class MappingNamespace {
        @XStreamAsAttribute
        public String prefix;

        @XStreamAsAttribute
        public String uri;

        public String toString() {
            return prefix + ":" + uri;
        }
    }

    @XStreamAlias("domain")
    public static class Domain {

        public Source source;

        public Target target;

        public Comments comments;

        public String toString() {
            return "Domain(" + source + ", " + target + ")";
        }
    }

    @XStreamAlias("source")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    public static class Source {

        public String expression;

        public String toString() {
            return "Source(" + expression + ")";
        }
    }

    @XStreamAlias("target")
    public static class Target {

        @XStreamAlias("if")
        public Condition condition;

        @XStreamAlias("entity")
        public EntityElement entityElement;

        @XStreamAlias("property")
        public PropertyElement propertyElement;

        public Intermediate intermediate;

        @XStreamImplicit
        public List<Additional> additionals;

        public String toString() {
            return "Target(" + entityElement + ", " + propertyElement + ")";
        }
    }

    @XStreamAlias("path")
    public static class Path {

        public Source source;

        public Target target;

        public Comments comments;
    }

    @XStreamAlias("range")
    public static class Range {

        public Source source;

        public Target target;

        public Comments comments;
    }

    @XStreamAlias("additional")
    public static class Additional {

        @XStreamAlias("property")
        public PropertyElement propertyElement;

        @XStreamAlias("entity")
        public EntityElement entityElement;
    }

    @XStreamAlias("intermediate")
    public static class Intermediate {

        @XStreamAlias("entity")
        public EntityElement entityElement;

        @XStreamAlias("property")
        public PropertyElement propertyElement;
    }

    @XStreamAlias("if")
    public static class Condition {
        public Narrower narrower;
        public Exists exists;
        public Equals equals;
        public AndCondition and;
        public OrCondition or;
        public NotCondition not;
    }

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    @XStreamAlias("exists")
    public static class Exists {
        public String expression;
    }

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    @XStreamAlias("equals")
    public static class Equals {
        @XStreamAsAttribute
        public String value;

        public String expression;
    }

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    @XStreamAlias("narrower")
    public static class Narrower {
        @XStreamAsAttribute
        public String value;

        public String expression;
    }

    @XStreamAlias("and")
    public static class AndCondition {
        @XStreamImplicit
        List<Condition> list;
    }

    @XStreamAlias("or")
    public static class OrCondition {
        @XStreamImplicit
        List<Condition> list;
    }

    @XStreamAlias("not")
    public static class NotCondition {
        @XStreamAlias("if")
        Condition condition;
    }

    @XStreamAlias("property")
    public static class PropertyElement {

        @XStreamAlias("qname")
        public QualifiedName qualifiedName;

        public QualifiedName getPropertyClass(X3MLContext.PathContext context) {
            if (qualifiedName == null) throw new X3MLException("Missing class element");
            return qualifiedName;
        }
    }

    @XStreamAlias("entity")
    public static class EntityElement {

        @XStreamAlias("qname")
        public QualifiedName qualifiedName;

        @XStreamAlias("value_generator")
        public ValueGenerator valueGenerator;

        public Value getValue(X3MLContext.ValueContext context) {
            return context.generateValue(valueGenerator, this);
        }

        public String toString() {
            return "Entity(" + valueGenerator + ")";
        }
    }

    @XStreamAlias("qname")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"tag"})
    public static class QualifiedName {
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

        public String toString() {
            return "Class(" + tag + ")";
        }
    }

    @XStreamAlias("comments")
    public static class Comments {

        @XStreamImplicit
        public List<Comment> comments;

    }

    @XStreamAlias("comment")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"content"})
    public static class Comment {
        @XStreamAsAttribute
        public String type;

        public String content;
    }

    @XStreamAlias("value_generator")
    public static class ValueGenerator {
        @XStreamAsAttribute
        public String name;

        @XStreamImplicit
        public List<ValueFunctionArg> args;

        public String toString() {
            return "URIFunction(" + name + ")";
        }
    }

    @XStreamAlias("arg")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"value"})
    public static class ValueFunctionArg {
        @XStreamAsAttribute
        public String name;

        public String value;

        public String toString() {
            return name + ":=" + value;
        }
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
}
