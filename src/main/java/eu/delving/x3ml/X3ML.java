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

    @XStreamAlias("mappings")
    public static class Mappings {

        @XStreamAsAttribute
        public String version;

        public Metadata metadata;

        public List<MappingConstant> mappingConstants;

        public List<MappingNamespace> mappingNamespaces;

        @XStreamImplicit
        public List<Mapping> mappings;

        public void apply(X3MLContext context) {
            for (Mapping mapping : mappings) {
                mapping.applyMapping(context);
            }
        }
    }

    @XStreamAlias("metadata")
    public static class Metadata {
        @XStreamAsAttribute
        public String version;

        public String title;

        public String description;
    }

    @XStreamAlias("constant")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"content"})
    public static class MappingConstant {
        @XStreamAsAttribute
        public String name;

        public String content;

        public String toString() {
            return content;
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

        public String source;

        public Entity entity;

        public Comments comments;
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
        public String source;

        public Property property;

        @XStreamAlias("internal_node")
        @XStreamImplicit
        public List<InternalNode> internalNode;

        public Comments comments;
    }

    @XStreamAlias("range")
    public static class Range {
        public String source;

        public Entity entity;

        @XStreamAlias("additional_node")
        public AdditionalNode additionalNode;

        public Comments comments;
    }

    @XStreamAlias("additional_node")
    public static class AdditionalNode {

        public Property property;

        public Entity entity;
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
    public static class Property {
        @XStreamAsAttribute
        public String tag;

        @XStreamAlias("exists")
        public Exists exists;

        public String getPropertyURI(X3MLContext.PathContext context) {
            if (exists != null && !exists.evaluate(context)) return null;
            return tag; // todo: should be a CRM URI i suppose
        }
    }

    @XStreamAlias("entity")
    public static class Entity {
        @XStreamAsAttribute
        public String tag;

        @XStreamAsAttribute
        public String binding;

        @XStreamAlias("exists")
        public Exists exists;

        @XStreamAlias("uri_function")
        public URIFunction uriFunction;

        public String generateDomainURI(X3MLContext.DomainContext context) {
            if (exists != null && !exists.evaluate(context)) return null;
            return context.generateUri(uriFunction);
        }

        public String generateRangeUri(X3MLContext.RangeContext context) {
            if (exists != null && !exists.evaluate(context)) return null;
            return context.generateUri(uriFunction);
        }
    }

    @XStreamAlias("internal_node")
    public static class InternalNode {
        public Entity entity;
        public Property property;

        public void applyInternalNode(X3MLContext context, Domain domain, Property contextProperty) {
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
    }

    @XStreamAlias("arg")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    public static class URIFunctionArg {
        @XStreamAsAttribute
        public String name;

        public String expression;

        public String toString() {
            return name + ":=" + expression;
        }
    }

    public interface URIArguments {
        String getArgument(String name);
    }

    public interface URIPolicy {
        String generateUri(String name, URIArguments arguments);
    }
}
