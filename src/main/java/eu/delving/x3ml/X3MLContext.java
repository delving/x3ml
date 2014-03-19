package eu.delving.x3ml;

import com.hp.hpl.jena.rdf.model.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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

        Value generateValue(ValueGenerator valueGenerator, EntityElement entityElement);
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
        public Value generateValue(final ValueGenerator valueGenerator, final EntityElement entityElement) {
            if (valueGenerator == null) {
                throw new X3MLException("Value generator missing");
            }
            Value value = valuePolicy.generateValue(valueGenerator.name, new ValueFunctionArgs() {
                @Override
                public ArgValue getArgValue(String name, SourceType type) {
                    return evaluateArgument(node, valueGenerator, name, type, entityElement.qualifiedName);
                }

                @Override
                public String toString() {
                    return entityElement.toString();
                }
            });
            if (value.uri == null && value.literal == null) {
                throw new X3MLException("Empty value produced");
            }
            return value;
        }

    }

    public List<DomainContext> createDomainContexts(Domain domain) {
        List<Node> domainNodes = nodeList(documentRoot, domain.source);
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
            if (path.source == null) throw new X3MLException("Path source absent");
            List<PathContext> pathContexts = new ArrayList<PathContext>();
            for (Node pathNode : nodeList(node, path.source)) {
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
            if (conditionFails(path.target_relation.condition, this)) return false;
            qualifiedName = path.target_relation.propertyElement.getPropertyClass();
            if (qualifiedName == null) return false;
            String namespaceUri = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
            property = model.createProperty(namespaceUri, qualifiedName.getLocalName());
            intermediateNodes = createIntermediateNodes(path.target_relation.intermediates, this);
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
            if (range.source == null) throw new X3MLException("Range source absent: " + range);
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
            String rangeSource = range.source.expression;
            String pathSource = path.source.expression;
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
        public Resource resource;
        public List<AdditionalNode> additionalNodes;
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
            Value value = entityElement.getValue(valueContext);
            if (value == null) return false;
            if (value.uri != null) {
                resource = createTypedResource(value.uri, entityElement.qualifiedName);
                additionalNodes = createAdditionalNodes(entityElement.additionals, valueContext);
            }
            if (value.literal != null) {
                literal = createLiteral(value.literal);
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
            if (resource != null) {
                for (AdditionalNode additionalNode : createAdditionalNodes(entityElement.additionals, valueContext)) {
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
            Value value = additional.entityElement.getValue(valueContext);
            if (value == null) return false;
            if (additional.entityElement.qualifiedName != null) {
                resource = createTypedResource(value.uri, additional.entityElement.qualifiedName);
                return true;
            }
            if (value.literal != null) {
                literal = createLiteral(value.literal);
                return true;
            }
            return false;
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

    private List<IntermediateNode> createIntermediateNodes(List<Intermediate> intermediateList, ValueContext valueContext) {
        List<IntermediateNode> intermediateNodes = new ArrayList<IntermediateNode>();
        if (intermediateList != null) {
            for (Intermediate intermediate : intermediateList) {
                IntermediateNode intermediateNode = new IntermediateNode(intermediate, valueContext);
                if (intermediateNode.resolve()) {
                    intermediateNodes.add(intermediateNode);
                }
            }
        }
        return intermediateNodes;
    }

    private class IntermediateNode {
        public final Intermediate intermediate;
        public final ValueContext valueContext;
        public EntityResolver entityResolver;
        public Property property;

        private IntermediateNode(Intermediate intermediate, ValueContext valueContext) {
            this.intermediate = intermediate;
            this.valueContext = valueContext;
        }

        public boolean resolve() {
            entityResolver = new EntityResolver(intermediate.entityElement, valueContext);
            if (!entityResolver.resolve()) return false;
            property = createProperty(intermediate.propertyElement.qualifiedName);
            return property != null && entityResolver.hasResource();
        }
    }

    private ArgValue evaluateArgument(Node contextNode, ValueGenerator function, String argName, SourceType type, QualifiedName qualifiedName) {
        if (argName == null && type == SourceType.QNAME && qualifiedName != null) {
            ArgValue v = new ArgValue();
            v.setQName(qualifiedName.tag, namespaceContext);
            return v;
        }
        else {
            return evaluateArgumentZ(contextNode, function, argName, type);
        }
    }

    private ArgValue evaluateArgumentZ(Node contextNode, ValueGenerator function, String argName, SourceType type) {
        ValueFunctionArg foundArg = null;
        if (function.args != null) {
            if (function.args.size() == 1 && function.args.get(0).name == null) {
                foundArg = function.args.get(0);
                foundArg.name = argName;
            }
            else {
                for (ValueFunctionArg arg : function.args) {
                    if (arg.name.equals(argName)) {
                        foundArg = arg;
                    }
                }
            }
        }
        if (foundArg == null) {
            return null;
        }
        ArgValue value = new ArgValue();
        switch (type) {
            case XPATH:
                value.string = valueAt(contextNode, foundArg.value);
                if (value.string.isEmpty()) {
                    throw new X3MLException("Empty result");
                }
                break;
            case QNAME:
                value.setQName(foundArg.value, namespaceContext);
                break;
            case LITERAL:
                value.string = foundArg.value;
                break;
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

    private Literal createLiteral(String value) {
        return model.createLiteral(value); // todo: language
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
