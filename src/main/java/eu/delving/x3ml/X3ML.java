package eu.delving.x3ml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

import java.util.List;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public interface X3ML {
    String CLASS_NAME = "className";
    String UUID_NAME = "UUID";

    @XStreamAlias("mappings")
    public static class Mappings {

        @XStreamAsAttribute
        public String version;

        public List<MappingNamespace> namespaces;

        @XStreamImplicit
        public List<Mapping> mappings;

        public void apply(X3MLContext context) {
            for (Mapping mapping : mappings) {
                mapping.applyMapping(context);
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

    @XStreamAlias("mapping")
    public static class Mapping {
        public Domain domain;

        @XStreamImplicit
        public List<Link> links;

        public void applyMapping(X3MLContext context) {
            for (X3MLContext.DomainContext domainContext : context.createDomainContexts(domain)) {
                for (Link link : links) {
                    link.applyLink(domainContext);
                }
            }
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
    public static class Source {

        @XStreamAlias("xpath")
        public XPathElement xpath;

        public String toString() {
            return "Source(" + xpath + ")";
        }
    }

    @XStreamAlias("target")
    public static class Target {

        @XStreamAlias("entity")
        public EntityElement entityElement;

        @XStreamAlias("property")
        public PropertyElement propertyElement;

        public String toString() {
            return "Target(" + entityElement + ", " + propertyElement + ")";
        }
    }

    @XStreamAlias("link")
    public static class Link {

        public Path path;

        public Range range;

        public void applyLink(X3MLContext.DomainContext context) {
            for (X3MLContext.PathContext pathContext : context.createPathContexts(path)) {
                for (X3MLContext.RangeContext rangeContext : pathContext.createRangeContexts(range)) {
                    rangeContext.generateTriple();
                }
            }
        }
    }

    @XStreamAlias("path")
    public static class Path {

        public Source source;

        public Target target;

//        @XStreamAlias("internal_node")
//        @XStreamImplicit
//        public List<InternalNode> internalNode;
//
        public Comments comments;
    }

    @XStreamAlias("range")
    public static class Range {

        public Source source;

        public Target target;

//        @XStreamAlias("additional_node")
//        public AdditionalNode additionalNode;

        public Comments comments;
    }

    @XStreamAlias("additional_node")
    public static class AdditionalNode {

        @XStreamAlias("property")
        public PropertyElement propertyElement;

        @XStreamAlias("entity")
        public EntityElement entityElement;
    }

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"xpath"})
    @XStreamAlias("exists")
    public static class Exists {
        @XStreamAsAttribute
        public String value;

        public String xpath;

        public boolean evaluate(X3MLContext.DomainContext context) {
            return true; // todo
        }

        public boolean evaluate(X3MLContext.PathContext context) {
            return true; // todo
        }

        public boolean evaluate(X3MLContext.RangeContext context) {
            return true; // todo
        }
    }

    @XStreamAlias("property")
    public static class PropertyElement {

        @XStreamAlias("qname")
        public QualifiedName qualifiedName;

        @XStreamAlias("exists")
        public Exists exists;

        public QualifiedName getPropertyClass(X3MLContext.PathContext context) {
            if (exists != null && !exists.evaluate(context)) return null;
            if (qualifiedName == null) throw new X3MLException("Missing class element");
            return qualifiedName;
        }
    }

    @XStreamAlias("entity")
    public static class EntityElement {

        @XStreamAlias("qname")
        public QualifiedName qualifiedName;

        @XStreamAlias("xpath")
        public XPathElement xpath;

        @XStreamAlias("uri_function")
        public URIFunction uriFunction;

        public X3MLContext.EntityResolution getResolution(X3MLContext.DomainContext context) {
            X3MLContext.EntityResolution resolution = context.createResolution();
            resolution.qualifiedName = qualifiedName;
            if (xpath != null) {
                throw new X3MLException("No xpath allowed in domain");
            }
            if (uriFunction != null) {
                resolution.resourceString = context.generateUri(uriFunction);
            }
            return resolution;
        }

        public X3MLContext.EntityResolution getResolution(X3MLContext.RangeContext context) {
            X3MLContext.EntityResolution resolution = context.createResolution();
            resolution.qualifiedName = qualifiedName;
            if (xpath != null && qualifiedName != null) {
                resolution.literalString = context.dereference(xpath);
            }
            else if (uriFunction != null) {
                resolution.resourceString = context.generateUri(uriFunction);
            }
            else {
                throw new X3MLException("Entity must have class/literal or uri_function");
            }
            return resolution;
        }

        public String toString() {
            return "Entity(" + qualifiedName + ", " + xpath + ", " + uriFunction + ")";
        }
    }

    @XStreamAlias("qname")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"tag"})
    public static class QualifiedName {
        public String tag;

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

    @XStreamAlias("internal_node")
    public static class InternalNode {

        @XStreamAlias("entity")
        public EntityElement entityElement;

        @XStreamAlias("property")
        public PropertyElement propertyElement;

        public void applyInternalNode(X3MLContext context, Domain domain, PropertyElement contextPropertyElement) {
            // todo: implement
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

    @XStreamAlias("uri_function")
    public static class URIFunction {
        @XStreamAsAttribute
        public String name;

        @XStreamImplicit
        public List<URIFunctionArg> args;

        public String toString() {
            return "URIFunction(" + name + ")";
        }
    }

    @XStreamAlias("arg")
    public static class URIFunctionArg {
        @XStreamAsAttribute
        public String name;

        @XStreamAlias("xpath")
        public XPathElement xpath;

        public String toString() {
            return name + ":=" + xpath;
        }
    }

    @XStreamAlias("xpath")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    public static class XPathElement {
        public String expression;

        public XPathElement() {
        }

        public XPathElement(String expression) {
            this.expression = expression;
        }

        public String toString() {
            return expression;
        }
    }

    public interface URIArguments {
        String getArgument(String name);
    }

    public interface URIPolicy {
        String generateUri(String name, URIArguments arguments);
    }
}
