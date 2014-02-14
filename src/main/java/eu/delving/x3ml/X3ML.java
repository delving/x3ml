package eu.delving.x3ml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;
import org.w3c.dom.Node;

import java.util.List;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public interface X3ML {

    public interface Context {

        void setCurrentNode(Node node);

        String getConstant(String name);

        String valueAt(String expression);

        String generateURI(URIFunction function, Domain domain);

        String generateURI(URIFunction function, Entity domainEntity, Path path);

        boolean setDomain(Domain domain);

        boolean setProperty(String propertyURI);

        boolean setRange(Entity entity, Path path);

        boolean createTriple();
    }

    @XStreamAlias("mappings")
    public static class Mappings {

        @XStreamAsAttribute
        public String version;

        public Metadata metadata;

        public List<MappingConstant> mappingConstants;

        public List<MappingNamespace> mappingNamespaces;

        @XStreamImplicit
        public List<Mapping> mappings;

        public void apply(Context context) {
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

        public void applyMapping(Context context) {
            if (domain.applyDomain(context)) {
                for (Link link : links) {
                    link.applyLink(context, domain);
                }
            }
        }
    }

    @XStreamAlias("domain")
    public static class Domain {
        public String source;

        public Entity entity;

        public Comments comments;

        public boolean applyDomain(Context context) {
            return context.setDomain(this);
        }
    }

    @XStreamAlias("link")
    public static class Link {
        public Path path;

        public Range range;

        public void applyLink(Context context, Domain domain) {
            path.applyPath(context, domain);
            range.applyRange(context, domain, path);
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

        public void applyPath(Context context, Domain domain) {
            String propertyUri = property.getPropertyURI(context, domain);
            if (internalNode != null) {
                for (InternalNode node : internalNode) {
                    node.applyInternalNode(context, domain, property);
                }
            }
        }
    }

    @XStreamAlias("range")
    public static class Range {
        public String source;

        public Entity entity;

        @XStreamAlias("additional_node")
        public AdditionalNode additionalNode;

        public Comments comments;

        public void applyRange(Context context, Domain domain, Path path) {
            if (additionalNode != null) {
                additionalNode.apply(context, domain, path, entity);
            }
        }
    }

    @XStreamAlias("additional_node")
    public static class AdditionalNode {

        public Property property;

        public Entity entity;

        public boolean apply(Context context, Domain domain, Path path, Entity contextEntity) {
            String propertyURI = property.getPropertyURI(context, domain);
            if (context.setProperty(propertyURI)) {
                if (context.setRange(entity, path)) {
                    context.createTriple();
                    return true;
                }
            }
            return false;
        }
    }

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"xpath"})
    @XStreamAlias("exists")
    public static class Exists {
        @XStreamAsAttribute
        public String value;

        public String xpath;

        public boolean evaluate(Context context) {
            return true; // todo
        }
    }

    @XStreamAlias("property")
    public static class Property {
        @XStreamAsAttribute
        public String tag;

        @XStreamAlias("exists")
        public Exists exists;

        public String getPropertyURI(Context context, Domain domain) {
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

        public String generateDomainURI(Context context, Domain domain) {
            if (exists != null && !exists.evaluate(context)) return null;
            return context.generateURI(uriFunction, domain);
        }

        public String generateRangeUri(Context context, Entity domainEntity, Path path) {
            if (exists != null && !exists.evaluate(context)) return null;
            return context.generateURI(uriFunction, domainEntity, path);
        }
    }

    @XStreamAlias("internal_node")
    public static class InternalNode {
        public Entity entity;
        public Property property;

        public void applyInternalNode(Context context, Domain domain, Property contextProperty) {
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
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"content"})
    public static class URIFunctionArg {
        @XStreamAsAttribute
        public String name;

        public String content;

        public String toString() {
            return content;
        }

        public String evaluate(Context context, Entity domainEntity, Path path) {

            return "arg";
        }
    }

}
