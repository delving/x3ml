//===========================================================================
//    Copyright 2014 Delving B.V.
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//===========================================================================
package eu.delving.x3ml.engine;

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

import static eu.delving.x3ml.X3MLEngine.exception;

/**
 * This interface defines the XML interpretation of the engine using the XStream library.
 *
 * There is also a helper class for encapsulating related functions.
 *
 * The XSD definition is to be found in /src/main/resources.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public interface X3ML {

    public enum SourceType {
        xpath,
        constant
    }

    @XStreamAlias("x3ml")
    public static class RootElement extends Visible {

        @XStreamAsAttribute
        public String version;

        @XStreamAsAttribute
        @XStreamAlias("source_type")
        public SourceType sourceType;

        @XStreamAsAttribute
        public String language;

        @XStreamOmitField
        public String info;

        public List<MappingNamespace> namespaces;

        public List<Mapping> mappings;

        public void apply(Root context) {
            for (Mapping mapping : mappings) {
                mapping.apply(context);
            }
        }

        @XStreamOmitField
        public String comments;
    }

    @XStreamAlias("mapping")
    public static class Mapping extends Visible {

        public DomainElement domain;

        @XStreamImplicit
        public List<Link> links;

        public void apply(Root context) {
            for (Domain domain : context.createDomainContexts(this.domain)) {
                if (links == null) continue;
                for (Link link : links) {
                    link.apply(domain);
                }
            }
        }
    }

    @XStreamAlias("link")
    public static class Link extends Visible {

        public PathElement path;

        public RangeElement range;

        public void apply(Domain context) {
            for (Path path : context.createPathContexts(this.path)) {
                for (Range range : path.createRangeContexts(this.range)) {
                    range.link();
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
    public static class DomainElement extends Visible {

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
    public static class PathElement extends Visible {

        public Source source_relation;

        public TargetRelation target_relation;

        public Comments comments;
    }

    @XStreamAlias("range")
    public static class RangeElement extends Visible {

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
            final GeneratorContext context;
            boolean failure;

            private Outcome(GeneratorContext context) {
                this.context = context;
            }

            Outcome evaluate(YesOrNo yesOrNo) {
                if (yesOrNo != null && !failure && !yesOrNo.yes(context)) {
                    failure = true;
                }
                return this;
            }
        }

        public boolean failure(GeneratorContext context) {
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
        boolean yes(GeneratorContext context);
    }

    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"expression"})
    @XStreamAlias("exists")
    public static class Exists extends Visible implements YesOrNo {

        public String expression;

        @Override
        public boolean yes(GeneratorContext context) {
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
        public boolean yes(GeneratorContext context) {
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
        public boolean yes(GeneratorContext context) {
            return true;
        }
    }

    @XStreamAlias("and")
    public static class AndCondition extends Visible implements YesOrNo {

        @XStreamImplicit
        List<Condition> list;

        @Override
        public boolean yes(GeneratorContext context) {
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
        public boolean yes(GeneratorContext context) {
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
        public boolean yes(GeneratorContext context) {
            return condition.failure(context);
        }
    }

    @XStreamAlias("relationship")
    @XStreamConverter(value = ToAttributedValueConverter.class, strings = {"tag"})
    public static class Relationship extends Visible {

        public String tag;

        public String getPrefix() {
            int colon = tag.indexOf(':');
            if (colon < 0) {
                throw exception("Unqualified tag " + tag);
            }
            return tag.substring(0, colon);
        }

        public String getLocalName() {
            int colon = tag.indexOf(':');
            if (colon < 0) {
                throw exception("Unqualified tag " + tag);
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

        public String constant; // documentation purposes only

        @XStreamAlias("instance_generator")
        public GeneratorElement instanceGenerator;

        @XStreamImplicit
        public List<GeneratorElement> labelGenerators;

        @XStreamImplicit
        public List<Additional> additionals;

        public Instance getInstance(GeneratorContext context) {
            return context.getInstance(instanceGenerator);
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
                throw exception("Unqualified tag " + tag);
            }
            return tag.substring(0, colon);
        }

        public String getLocalName() {
            int colon = tag.indexOf(':');
            if (colon < 0) {
                throw exception("Unqualified tag " + tag);
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

        @XStreamAsAttribute
        public String type;

        @XStreamAsAttribute
        public String language;

        public String value;
    }


    @XStreamAlias("generator_policy")
    public static class GeneratorPolicy extends Visible {
        public List<MappingNamespace> namespaces;

        @XStreamImplicit
        public
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

    public static class ArgValue {
        public final String string;
        public final String language;

        public ArgValue(String string, String language) {
            this.string = string;
            this.language = language;
        }

        public String toString() {
            if (string != null) {
                return "ArgValue(" + string + ")";
            }
            else {
                return "ArgValue?";
            }
        }
    }

    public interface ArgValues {
        ArgValue getArgValue(String name, SourceType sourceType);
    }

    public enum InstanceType {
        URI,
        LITERAL
    }

    public static class Instance {
        public final InstanceType type;
        public final String text;
        public final String language;

        public Instance(InstanceType type, String text, String language) {
            this.type = type;
            this.text = text;
            this.language = language;
        }

        public String toString() {
            return type + ":" + text;
        }
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

        public static XStream generatorStream() {
            XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
            xstream.setMode(XStream.NO_REFERENCES);
            xstream.processAnnotations(GeneratorPolicy.class);
            return xstream;
        }

        public static XStream x3mlStream() {
            XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
            xstream.setMode(XStream.NO_REFERENCES);
            xstream.processAnnotations(RootElement.class);
            return xstream;
        }

        public static ArgValue argVal(String string, String language) {
            return new ArgValue(string, language);
        }

        public static Instance uriValue(String uri) {
            return new Instance(InstanceType.URI, uri, null);
        }

        public static Instance literalValue(String literal, String language) {
            return new Instance(InstanceType.LITERAL, literal, language);
        }
    }
}
