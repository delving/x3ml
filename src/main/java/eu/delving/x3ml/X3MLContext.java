package eu.delving.x3ml;

import com.hp.hpl.jena.rdf.model.*;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static eu.delving.x3ml.X3ML.Helper.argQName;
import static eu.delving.x3ml.X3ML.Helper.argVal;

/**
 * The context in which the engine acts
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLContext implements X3ML {
    private final Element documentRoot;
    private final ValuePolicy valuePolicy;
    private final Root root;
    private NamespaceContext namespaceContext;
    private XPathFactory pathFactory = net.sf.saxon.xpath.XPathFactoryImpl.newInstance();
    private Model model = ModelFactory.createDefaultModel();

    X3MLContext(Element documentRoot, Root root, ValuePolicy valuePolicy, NamespaceContext namespaceContext, List<String> prefixes) {
        this.documentRoot = documentRoot;
        this.root = root;
        this.valuePolicy = valuePolicy;
        this.namespaceContext = namespaceContext;
        for (String prefix : prefixes) {
            this.model.setNsPrefix(prefix, namespaceContext.getNamespaceURI(prefix));
        }
    }

    public void writeXML(PrintStream out) {
        model.write(out, "RDF/XML-ABBREV");
    }

    public void writeNTRIPLE(PrintStream out) {
        model.write(out, "N-TRIPLE");
    }

    public void writeTURTLE(PrintStream out) {
        model.write(out, "TURTLE");
    }

    public void write(PrintStream out, String format) {
        if ("N-TRIPLE".equalsIgnoreCase(format)) {
            writeNTRIPLE(out);
        }
        else if ("TURTLE".equalsIgnoreCase(format)) {
            writeTURTLE(out);
        }
        else {
            writeXML(out);
        }
    }

    public String[] toStringArray() {
        return toString().split("\n");
    }

    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeNTRIPLE(new PrintStream(baos));
        return new String(baos.toByteArray());
    }

    // ===== calls made from within X3ML.* classes ====

    public interface ValueContext {

        Resource get(String variable);

        void put(String variable, Resource resource);

        String evaluate(String expression);

        Value generateValue(GeneratorElement generator, QualifiedName qualifiedName);

        String getLanguage();
    }

    public abstract class LocalContext implements ValueContext {
        public final ValueContext parent;
        public final Node node;

        protected LocalContext(ValueContext parent, Node node) {
            this.parent = parent;
            this.node = node;
        }

        @Override
        public Resource get(String variable) {
            if (parent == null) throw new X3MLException("Parent context missing");
            return parent.get(variable);
        }

        @Override
        public void put(String variable, Resource resource) {
            if (parent == null) throw new X3MLException("Parent context missing");
            parent.put(variable, resource);
        }

        @Override
        public String evaluate(String expression) {
            return valueAt(node, expression);
        }

        @Override
        public Value generateValue(final GeneratorElement generator, final QualifiedName qualifiedName) {
            if (generator == null) {
                throw new X3MLException("Value generator missing");
            }
            Value value = valuePolicy.generateValue(generator.name, new ArgValues() {
                @Override
                public ArgValue getArgValue(String name, ArgType type) {
                    return evaluateArgument(node, generator, name, type, qualifiedName);
                }
            });
            if (value == null) {
                throw new X3MLException("Empty value produced");
            }
            return value;
        }

        @Override
        public String getLanguage() {
            Node walkNode = node;
            while (walkNode != null) {
                NamedNodeMap attributes = walkNode.getAttributes();
                if (attributes != null) {
                    Node lang = attributes.getNamedItemNS("http://www.w3.org/XML/1998/namespace", "lang");
                    if (lang != null) {
                        return lang.getNodeValue();
                    }
                }
                walkNode = walkNode.getParentNode();
            }
            throw new X3MLException("Missing language");
        }

    }

    public List<DomainContext> createDomainContexts(Domain domain) {
        List<Node> domainNodes = nodeList(documentRoot, domain.source_node);
        List<DomainContext> domainContexts = new ArrayList<DomainContext>();
        for (Node domainNode : domainNodes) {
            DomainContext domainContext = new DomainContext(domain, domainNode);
            if (domainContext.resolve()) {
                domainContexts.add(domainContext);
            }
        }
        return domainContexts;
    }

    public class DomainContext extends LocalContext {
        public final Domain domain;
        public EntityResolver entityResolver;
        private Map<String, Resource> variables = new TreeMap<String, Resource>();

        public DomainContext(Domain domain, Node node) {
            super(null, node);
            this.domain = domain;
        }

        @Override
        public Resource get(String variable) {
//            System.out.println("GET " + variable);
            return variables.get(variable);
        }

        @Override
        public void put(String variable, Resource resource) {
//            System.out.println("PUT " + variable + " = " + resource);
            variables.put(variable, resource);
        }

        public boolean resolve() {
            if (conditionFails(domain.target_node.condition, this)) return false;
            entityResolver = new EntityResolver(domain.target_node.entityElement, this);
            return entityResolver.resolve();
        }

        public List<PathContext> createPathContexts(Path path) {
            if (path.source_relation == null) throw new X3MLException("Path source absent");
            List<PathContext> pathContexts = new ArrayList<PathContext>();
            for (Node pathNode : nodeList(node, path.source_relation)) {
                PathContext pathContext = new PathContext(this, pathNode, path);
                if (pathContext.resolve()) {
                    pathContexts.add(pathContext);
                }
            }
            return pathContexts;
        }

        public void link() {
            entityResolver.link();
        }
    }

    public class PathContext extends LocalContext {
        public final DomainContext domainContext;
        public final Path path;
        public QualifiedName qualifiedName;
        public Property property;
        public List<IntermediateNode> intermediateNodes;
        public Resource lastResource;
        public Property lastProperty;

        public PathContext(DomainContext domainContext, Node node, Path path) {
            super(domainContext, node);
            this.domainContext = domainContext;
            this.path = path;
        }

        public boolean resolve() {
            TargetRelation relation = path.target_relation;
            if (conditionFails(relation.condition, this)) return false;
            if (relation.properties == null) {
                throw new X3MLException("Target relation must have at least one property");
            }
            if (relation.entities != null) {
                if (relation.entities.size() + 1 != relation.properties.size()) {
                    throw new X3MLException("Target relation must have one more property than entity");
                }
            }
            else if (relation.properties.size() != 1) {
                throw new X3MLException("Target relation must just one property if it has no entiti3s");
            }

            qualifiedName = relation.properties.get(0).qualifiedName;
            if (qualifiedName == null) return false;
            String namespaceUri = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
            property = model.createProperty(namespaceUri, qualifiedName.getLocalName());
            intermediateNodes = createIntermediateNodes(relation.entities, relation.properties, this);
            return true;
        }

        public void link() {
            domainContext.link();
            if (!domainContext.entityResolver.hasResource()) {
                throw new X3MLException("Domain node has no resource");
            }
            lastResource = domainContext.entityResolver.resource;
            lastProperty = property;
            for (IntermediateNode intermediateNode : intermediateNodes) {
                intermediateNode.entityResolver.link();
                if (!intermediateNode.entityResolver.hasResource()) {
                    throw new X3MLException("Intermediate node has no resource");
                }
                lastResource.addProperty(lastProperty, intermediateNode.entityResolver.resource);
                lastResource = intermediateNode.entityResolver.resource;
                lastProperty = intermediateNode.property;
            }
        }

        public List<RangeContext> createRangeContexts(Range range) {
            if (range.source_node == null) throw new X3MLException("Range source absent: " + range);
            String pathExtension = getPathExtension(range);
            List<Node> rangeNodes = nodeList(node, pathExtension);
            List<RangeContext> rangeContexts = new ArrayList<RangeContext>();
            for (Node rangeNode : rangeNodes) {
                RangeContext rangeContext = new RangeContext(this, rangeNode, range);
                if (rangeContext.resolve()) {
                    rangeContexts.add(rangeContext);
                }
            }
            return rangeContexts;
        }

        private String getPathExtension(Range range) {
            String rangeSource = range.source_node.expression;
            String pathSource = path.source_relation.expression;
            if (pathSource.length() > rangeSource.length()) {
                throw new X3MLException(String.format(
                        "Path source [%s] longer than range source [%s]",
                        pathSource, rangeSource
                ));
            }
            String base = rangeSource.substring(0, pathSource.length());
            String pathExtension = rangeSource.substring(base.length());
            if (pathExtension.length() > 0) {
                if (pathExtension.charAt(0) == '/') {
                    pathExtension = pathExtension.substring(1);
                }
            }
            if (!base.equals(pathSource)) {
                throw new X3MLException(String.format(
                        "Path and Range source expressions are not compatible: %s %s",
                        pathSource, rangeSource
                ));
            }
            return pathExtension;
        }
    }

    public class RangeContext extends LocalContext {
        public final PathContext pathContext;
        public final Range range;
        public EntityResolver rangeResolver;

        public RangeContext(PathContext pathContext, Node node, Range range) {
            super(pathContext, node);
            this.pathContext = pathContext;
            this.range = range;
        }

        public boolean resolve() {
            if (conditionFails(range.target_node.condition, this)) return false;
            rangeResolver = new EntityResolver(range.target_node.entityElement, this);
            return rangeResolver.resolve();
        }

        public void link() {
            pathContext.link();
            if (rangeResolver.hasResource()) {
                rangeResolver.link();
                pathContext.lastResource.addProperty(pathContext.lastProperty, rangeResolver.resource);
            }
            else if (rangeResolver.hasLiteral()) {
                pathContext.lastResource.addLiteral(pathContext.lastProperty, rangeResolver.literal);
            }
        }
    }

// =============================================

    private class EntityResolver {
        public final EntityElement entityElement;
        public final ValueContext valueContext;
        public List<LabelNode> labelNodes;
        public List<AdditionalNode> additionalNodes;
        public Resource resource;
        public Literal literal;

        EntityResolver(EntityElement entityElement, ValueContext valueContext) {
            this.entityElement = entityElement;
            this.valueContext = valueContext;
        }

        boolean resolve() {
            if (entityElement == null) {
                throw new X3MLException("Missing entity");
            }
            if (entityElement.variable == null) {
                return resolveResource();
            }
            resource = valueContext.get(entityElement.variable);
            if (resource != null) return true;
            if (!resolveResource()) return false;
            valueContext.put(entityElement.variable, resource);
            return true;
        }

        private boolean resolveResource() {
            List<ValueEntry> values = entityElement.getValues(valueContext);
            if (values.isEmpty()) return false;
            for (ValueEntry valueEntry : values) { // todo: this will fail for multiple value entries
                switch (valueEntry.value.valueType) {
                    case URI:
                        resource = createTypedResource(valueEntry.value.text, valueEntry.qualifiedName);
                        labelNodes = createLabelNodes(entityElement.labelGenerators, valueContext);
                        additionalNodes = createAdditionalNodes(entityElement.additionals, valueContext);
                        break;
                    case LITERAL:
                        literal = createLiteral(valueEntry.value.text, valueContext.getLanguage());
                        break;
                }
            }
            return hasResource() || hasLiteral();
        }

        boolean hasResource() {
            return resource != null;
        }

        boolean hasLiteral() {
            return literal != null;
        }

        void link() {
            if (resource != null && labelNodes != null) {
                for (LabelNode labelNode : labelNodes) {
                    labelNode.linkFrom(resource);
                }
                for (AdditionalNode additionalNode : additionalNodes) {
                    additionalNode.linkFrom(resource);
                }
            }
        }
    }

    private boolean conditionFails(Condition condition, ValueContext context) {
        return condition != null && condition.failure(context);
    }

    private List<AdditionalNode> createAdditionalNodes(List<Additional> additionalList, ValueContext valueContext) {
        List<AdditionalNode> additionalNodes = new ArrayList<AdditionalNode>();
        if (additionalList != null) {
            for (Additional additional : additionalList) {
                AdditionalNode additionalNode = new AdditionalNode(additional, valueContext);
                if (additionalNode.resolve()) {
                    additionalNodes.add(additionalNode);
                }
            }
        }
        return additionalNodes;
    }

    private class AdditionalNode {
        public final Additional additional;
        public final ValueContext valueContext;
        public Property property;
        public Resource resource;
        public Literal literal;

        private AdditionalNode(Additional additional, ValueContext valueContext) {
            this.additional = additional;
            this.valueContext = valueContext;
        }

        public boolean resolve() {
            property = createProperty(additional.propertyElement.qualifiedName);
            if (property == null) return false;
            List<ValueEntry> values = additional.entityElement.getValues(valueContext);
            if (values.isEmpty()) return false;
            for (ValueEntry valueEntry : values) { // todo: this will fail for multiple value entries
                switch (valueEntry.value.valueType) {
                    case URI:
                        resource = createTypedResource(valueEntry.value.text, valueEntry.qualifiedName);
                        break;
                    case LITERAL:
                        literal = createLiteral(valueEntry.value.text, valueContext.getLanguage());
                        break;
                }
            }
            return true;
        }

        public void linkFrom(Resource fromResource) {
            if (resource != null) {
                fromResource.addProperty(property, resource);
            }
            else if (literal != null) {
                fromResource.addLiteral(property, literal);
            }
            else {
                throw new X3MLException("Cannot link without property or literal");
            }
        }
    }

    private List<LabelNode> createLabelNodes(List<GeneratorElement> generatorList, ValueContext valueContext) {
        List<LabelNode> labelNodes = new ArrayList<LabelNode>();
        if (generatorList != null) {
            for (GeneratorElement generator : generatorList) {
                LabelNode labelNode = new LabelNode(generator, valueContext);
                if (labelNode.resolve()) {
                    labelNodes.add(labelNode);
                }
            }
        }
        return labelNodes;
    }

    private class LabelNode {
        public final GeneratorElement generator;
        public final ValueContext valueContext;
        public Property property;
        public Literal literal;

        private LabelNode(GeneratorElement generator, ValueContext valueContext) {
            this.generator = generator;
            this.valueContext = valueContext;
        }

        public boolean resolve() {
            property = createProperty(new QualifiedName("rdfs:label", "http://www.w3.org/2000/01/rdf-schema#"));
            Value value = valueContext.generateValue(generator, null);
            if (value == null) return false;
            switch (value.valueType) {
                case URI:
                    throw new X3MLException("Label node must produce a literal");
                case LITERAL:
                    literal = createLiteral(value.text, valueContext.getLanguage());
                    return true;
            }
            return false;
        }

        public void linkFrom(Resource fromResource) {
            fromResource.addLiteral(property, literal);
        }
    }

    private List<IntermediateNode> createIntermediateNodes(List<EntityElement> entityList, List<PropertyElement> propertyList, ValueContext valueContext) {
        List<IntermediateNode> intermediateNodes = new ArrayList<IntermediateNode>();
        if (entityList != null) {
            Iterator<PropertyElement> walkProperty = propertyList.iterator();
            walkProperty.next(); // ignore
            for (EntityElement entityElement : entityList) {
                IntermediateNode intermediateNode = new IntermediateNode(entityElement, walkProperty.next(), valueContext);
                if (intermediateNode.resolve()) {
                    intermediateNodes.add(intermediateNode);
                }
            }
        }
        return intermediateNodes;
    }

    private class IntermediateNode {
        public final EntityElement entityElement;
        public final PropertyElement propertyElement;
        public final ValueContext valueContext;
        public EntityResolver entityResolver;
        public Property property;

        private IntermediateNode(EntityElement entityElement, PropertyElement propertyElement, ValueContext valueContext) {
            this.entityElement = entityElement;
            this.propertyElement = propertyElement;
            this.valueContext = valueContext;
        }

        public boolean resolve() {
            entityResolver = new EntityResolver(entityElement, valueContext);
            if (!entityResolver.resolve()) return false;
            property = createProperty(propertyElement.qualifiedName);
            return true;
        }
    }

    private ArgValue evaluateArgument(Node contextNode, GeneratorElement function, String argName, ArgType type, QualifiedName qualifiedName) {
        GeneratorArg foundArg = null;
        if (function.args != null) {
            if (function.args.size() == 1 && function.args.get(0).name == null) {
                foundArg = function.args.get(0);
                foundArg.name = argName;
            }
            else {
                for (GeneratorArg arg : function.args) {
                    if (arg.name.equals(argName)) {
                        foundArg = arg;
                    }
                }
            }
        }
        ArgValue value;
        switch (type) {
            case XPATH:
                if (foundArg == null) {
                    return null;
                }
                value = argVal(valueAt(contextNode, foundArg.value));
                if (value.string.isEmpty()) {
                    throw new X3MLException("Empty result");
                }
                break;
            case QNAME:
                value = argQName(qualifiedName, argName);
                break;
            case CONSTANT:
                if (foundArg == null) {
                    return null;
                }
                value = argVal(foundArg.value);
                break;
            default:
                throw new RuntimeException("Not implemented");
        }
        return value;
    }

    private Resource createTypedResource(String uriString, QualifiedName qualifiedName) {
        if (qualifiedName == null) {
            throw new X3MLException("Missing qualified name");
        }
        String typeUri = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
        return model.createResource(uriString, model.createResource(typeUri + qualifiedName.getLocalName()));
    }

    private Property createProperty(QualifiedName qualifiedName) {
        if (qualifiedName == null) {
            throw new X3MLException("Missing qualified name");
        }
        String propertyNamespace = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
        return model.createProperty(propertyNamespace, qualifiedName.getLocalName());
    }

    private Literal createLiteral(String value, String language) {
        return model.createLiteral(value, language);
    }

    private String valueAt(Node node, String expression) {
        List<Node> nodes = nodeList(node, expression);
        if (nodes.isEmpty()) return "";
        String value = nodes.get(0).getNodeValue();
        if (value == null) return "";
        return value.trim();
    }

    private List<Node> nodeList(Node node, Source source) {
        if (source != null) {
            return nodeList(node, source.expression);
        }
        else {
            List<Node> list = new ArrayList<Node>(1);
            list.add(node);
            return list;
        }
    }

    private List<Node> nodeList(Node context, String expression) {
        if (expression == null || expression.length() == 0) {
            List<Node> list = new ArrayList<Node>(1);
            list.add(context);
            return list;
        }
        try {
            XPathExpression xe = path().compile(expression);
            NodeList nodeList = (NodeList) xe.evaluate(context, XPathConstants.NODESET);
            int nodesReturned = nodeList.getLength();
            List<Node> list = new ArrayList<Node>(nodesReturned);
//            System.out.println(String.format(
//                    "nodes return from %s in context %s: %d",
//                    expression, context, nodesReturned
//            ));
            for (int index = 0; index < nodesReturned; index++) {
                list.add(nodeList.item(index));
            }
            return list;
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException("XPath Problem: " + expression, e);
        }
    }

    private XPath path() {
        if (root.sourceType != SourceType.XPATH)
            throw new X3MLException("Only sourceType=\"XPATH\" is implemented, not " + root.sourceType);
        XPath path = pathFactory.newXPath();
        path.setNamespaceContext(namespaceContext);
        return path;
    }
}
