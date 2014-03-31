package eu.delving.x3ml;

import com.hp.hpl.jena.ontology.ConversionException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.*;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static eu.delving.x3ml.X3MLContext.ValueContext;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public interface X3ML {

    public enum SourceType {
        XPATH
    }

    @XStreamAlias("x3ml")
    public static class Root extends Visible {

        @XStreamAsAttribute
        public String version;

        @XStreamAsAttribute
        @XStreamAlias("source_type")
        public SourceType sourceType;

        @XStreamOmitField
        public String info;

        public List<MappingNamespace> namespaces;

        public List<Mapping> mappings;

        public void apply(X3MLContext context) {
            for (Mapping mapping : mappings) {
                mapping.apply(context);
            }
        }

        @XStreamOmitField
        public String comments;
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

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    public static class Source extends Visible {
        public String expression;
    }

    @XStreamAlias("domain")
    public static class Domain extends Visible {

        public Source source_node;

        public TargetNode target_node;

        public Comments comments;
    }

    @XStreamAlias("target_relation")
    @XStreamConverter(TargetRelationConverter.class)
    public static class TargetRelation extends Visible {

        public Condition condition;

        public List<Relationship> properties;

        public List<EntityElement> entities;
    }

    public static class TargetRelationConverter implements Converter {
        // make sure the output is property-entity-property-entity-property

        @Override
        public boolean canConvert(Class type) {
            return TargetRelation.class.equals(type);
        }

        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            TargetRelation relation = (TargetRelation) source;
            if (relation.condition != null) {
                writer.startNode("if");
                context.convertAnother(relation.condition);
                writer.endNode();
            }
            Iterator<Relationship> walkProperties = relation.properties.iterator();
            Relationship relationship = walkProperties.next();
            writer.startNode("relationship");
            context.convertAnother(relationship);
            writer.endNode();
            for (EntityElement entityElement : relation.entities) {
                relationship = walkProperties.next();
                writer.startNode("entity");
                context.convertAnother(entityElement);
                writer.endNode();
                writer.startNode("relationship");
                context.convertAnother(relationship);
                writer.endNode();
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            TargetRelation relation = new TargetRelation();
            relation.properties = new ArrayList<Relationship>();
            relation.entities = new ArrayList<EntityElement>();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if ("if".equals(reader.getNodeName())) {
                    relation.condition = (Condition) context.convertAnother(relation, Condition.class);
                }
                else if ("relationship".equals(reader.getNodeName())) {
                    relation.properties.add((Relationship) context.convertAnother(relation, Relationship.class));
                }
                else if ("entity".equals(reader.getNodeName())) {
                    relation.entities.add((EntityElement) context.convertAnother(relation, EntityElement.class));
                }
                else {
                    throw new ConversionException("Unrecognized: " + reader.getNodeName());
                }
                reader.moveUp();
            }
            return relation;
        }
    }

    @XStreamAlias("target_node")
    public static class TargetNode extends Visible {

        @XStreamAlias("if")
        public Condition condition;

        @XStreamAlias("entity")
        public EntityElement entityElement;
    }

    @XStreamAlias("path")
    public static class Path extends Visible {

        public Source source_relation;

        public TargetRelation target_relation;

        public Comments comments;
    }

    @XStreamAlias("range")
    public static class Range extends Visible {

        public Source source_node;

        public TargetNode target_node;

        public Comments comments;
    }

    @XStreamAlias("additional")
    public static class Additional extends Visible {

        @XStreamAlias("property")
        public Relationship relationship;

        @XStreamAlias("entity")
        public EntityElement entityElement;
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

    @XStreamAlias("relationship")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"tag"})
    public static class Relationship extends Visible {

        public String tag;

        @XStreamOmitField
        public String namespaceUri;

        public Relationship() {
        }

        public Relationship(String tag, String namespaceUri) {
            this.tag = tag;
            this.namespaceUri = namespaceUri;
        }

        public String getPrefix() {
            int colon = tag.indexOf(':');
            if (colon < 0) {
                throw new X3MLException("Unqualified tag " + tag);
            }
            return tag.substring(0, colon);
        }

        public String getLocalName() {
            int colon = tag.indexOf(':');
            if (colon < 0) {
                throw new X3MLException("Unqualified tag " + tag);
            }
            return tag.substring(colon + 1);
        }
    }

    @XStreamAlias("entity")
    public static class EntityElement extends Visible {

        @XStreamAsAttribute
        public String variable;

        @XStreamImplicit
        public List<TypeElement> typeElements;

        public String constant;

        @XStreamAlias("value_generator")
        public GeneratorElement valueGenerator;

        @XStreamImplicit
        public List<GeneratorElement> labelGenerators;

        @XStreamImplicit
        public List<Additional> additionals;

        public List<ValueEntry> getValues(ValueContext context) {
            List<ValueEntry> values = new ArrayList<ValueEntry>();
            if (typeElements != null) {
                for (TypeElement typeElement : typeElements) {
                   values.add(new ValueEntry(
                           typeElement,
                           context.generateValue(valueGenerator, typeElement)
                   ));
                }
            }
            return values;
        }
    }

    public static class ValueEntry {
        public final TypeElement typeElement;
        public final Value value;

        public ValueEntry(TypeElement typeElement, Value value) {
            this.typeElement = typeElement;
            this.value = value;
        }
    }

    @XStreamAlias("type")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"tag"})
    public static class TypeElement extends Visible {

        public String tag;

        @XStreamOmitField
        public String namespaceUri;

        public TypeElement() {
        }

        public TypeElement(String tag, String namespaceUri) {
            this.tag = tag;
            this.namespaceUri = namespaceUri;
        }

        public String getPrefix() {
            int colon = tag.indexOf(':');
            if (colon < 0) {
                throw new X3MLException("Unqualified tag " + tag);
            }
            return tag.substring(0, colon);
        }

        public String getLocalName() {
            int colon = tag.indexOf(':');
            if (colon < 0) {
                throw new X3MLException("Unqualified tag " + tag);
            }
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

    @XStreamAlias("label_generator")
    public static class GeneratorElement extends Visible {
        @XStreamAsAttribute
        public String name;

        @XStreamImplicit
        public List<GeneratorArg> args;
    }

    @XStreamAlias("arg")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"value"})
    public static class GeneratorArg extends Visible {
        @XStreamAsAttribute
        public String name;

        public String value;
    }


    @XStreamAlias("generator_policy")
    public static class GeneratorPolicy extends Visible {
        List<MappingNamespace> namespaces;

        @XStreamImplicit
        List<GeneratorSpec> generators;
    }

    @XStreamAlias("generator")
    public static class GeneratorSpec extends Visible {
        @XStreamAsAttribute
        public String name;

        @XStreamAsAttribute
        public String prefix;

        public String pattern;

        public String toString() {
            return pattern;
        }
    }

    public enum ArgType {
        XPATH,
        CONSTANT,
        QNAME
    }

    public static class ArgValue {
        public final TypeElement typeElement;
        public final String string;

        public ArgValue(TypeElement typeElement, String string) {
            this.typeElement = typeElement;
            this.string = string;
        }

        public String toString() {
            if (string != null) {
                return "ArgValue(" + string + ")";
            }
            else if (typeElement != null) {
                return "ArgValue(" + typeElement + ")";
            }
            else {
                return "ArgValue?";
            }
        }
    }

    public interface ArgValues {
        ArgValue getArgValue(String name, ArgType argType);
    }

    public enum ValueType {
        URI,
        LITERAL
    }

    public static class Value {
        public final ValueType valueType;
        public final String text;

        public Value(ValueType valueType, String text) {
            this.valueType = valueType;
            this.text = text;
        }

        public String toString() {
            return valueType + ":" + text;
        }
    }

    public interface ValuePolicy {
        Value generateValue(String name, ArgValues arguments);
    }

    static class Visible {
        public String toString() {
            return Helper.toString(this);
        }
    }

    static class Helper {
        static String toString(Object thing) {
            return "\n" + x3mlStream().toXML(thing);
        }

        static XStream generatorStream() {
            XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
            xstream.setMode(XStream.NO_REFERENCES);
            xstream.processAnnotations(GeneratorPolicy.class);
            return xstream;
        }

        static XStream x3mlStream() {
            XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
            xstream.setMode(XStream.NO_REFERENCES);
            xstream.processAnnotations(Root.class);
            return xstream;
        }

        public static ArgValue argQName(TypeElement typeElement, String name) {
            String value = null;
            if ("localName".equals(name)) {
                value = typeElement.getLocalName();
            }
            else if ("prefix".equals(name)) {
                value = typeElement.getPrefix();
            }
            else if ("namespaceUri".equals(name)) {
                value = typeElement.namespaceUri;
            }
            if (value == null) {
                throw new X3MLException("Expected 'localName', 'prefix', or 'namespaceUri', got " + name);
            }
            return new ArgValue(typeElement, value);
        }

        public static ArgValue argVal(String string) {
            return new ArgValue(null, string);
        }

        public static Value uriValue(String uri) {
            return new Value(ValueType.URI, uri);
        }

        public static Value literalValue(String uri) {
            return new Value(ValueType.LITERAL, uri);
        }
    }
}
