package eu.delving.x3ml;

import com.hp.hpl.jena.rdf.model.*;
import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import com.sun.tools.hat.internal.server.QueryListener;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

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
    private XPathFactory pathFactory = new XPathFactoryImpl();
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
        String evaluate(String expression);

        Value generateValue(ValueGenerator valueGenerator, EntityElement entityElement);
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

    public class DomainContext implements ValueContext {
        public final Domain domain;
        public final Node node;
        public Resource domainResource;
        public Value value;
        public List<AdditionalNode> additionalNodes;

        public DomainContext(Domain domain, Node node) {
            this.domain = domain;
            this.node = node;
        }

        public boolean resolve() {
            if (conditionFails(domain.target.condition, this)) return false;
            value = domain.target.entityElement.getValue(this);
            if (value == null) return false;
            domainResource = createTypedResource(value.uri, domain.target.entityElement.qualifiedName);
            additionalNodes = createAdditionalNodes(domain.target.additionals, this);
            return domainResource != null;
        }

        @Override
        public String evaluate(String expression) {
            return valueAt(node, expression);
        }

        @Override
        public Value generateValue(final ValueGenerator valueGenerator, final EntityElement entityElement) {
            return valuePolicy.generateValue(valueGenerator.name, new ValueFunctionArgs() {
                @Override
                public ArgValue getArgValue(String name, SourceType type) {
                    return evaluateArgument(node, valueGenerator, name, type, domain.target.entityElement.qualifiedName);
                }

                @Override
                public String toString() {
                    return entityElement.toString();
                }
            });
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
            for (AdditionalNode additionalNode : additionalNodes) {
                additionalNode.linkFrom(domainResource);
            }
        }
    }

    public class PathContext implements ValueContext {
        public final DomainContext domainContext;
        public final Node node;
        public final Path path;
        public QualifiedName qualifiedName;
        public Property property, intermediateProperty;
        public Resource intermediateResource;

        public PathContext(DomainContext domainContext, Node node, Path path) {
            this.domainContext = domainContext;
            this.node = node;
            this.path = path;
        }

        public boolean resolve() {
            if (conditionFails(path.target.condition, this)) return false;
            qualifiedName = path.target.propertyElement.getPropertyClass();
            if (qualifiedName == null) return false;
            String namespaceUri = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
            property = model.createProperty(namespaceUri, qualifiedName.getLocalName());
            if (path.target.intermediate != null) {
                Intermediate inter = path.target.intermediate;
                Value interValue = inter.entityElement.getValue(this);
                intermediateResource = createTypedResource(interValue.uri, inter.entityElement.qualifiedName);
                intermediateProperty = createProperty(inter.propertyElement.qualifiedName);
                if (interValue.labelQName != null) {
                    intermediateResource.addLiteral(
                            createProperty(interValue.labelQName),
                            createLiteral(interValue.labelValue)
                    );
                }
            }
            return true;
        }

        public void linkTo(Resource rangeResource) {
            if (intermediateProperty == null) {
                domainContext.domainResource.addProperty(property, rangeResource);
            }
            else {
                domainContext.domainResource.addProperty(property, intermediateResource);
                intermediateResource.addProperty(intermediateProperty, rangeResource);
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

        @Override
        public String evaluate(String expression) {
            return valueAt(node, expression);
        }

        @Override
        public Value generateValue(final ValueGenerator valueGenerator, final EntityElement entityElement) {
            return valuePolicy.generateValue(valueGenerator.name, new ValueFunctionArgs() {
                @Override
                public ArgValue getArgValue(String name, SourceType type) {
                    return evaluateArgument(node, valueGenerator, name, type, entityElement.qualifiedName);
                }

                @Override
                public String toString() {
                    return entityElement.toString();
                }
            });
        }
    }

    public class RangeContext implements ValueContext {
        public final PathContext pathContext;
        public final Node node;
        public final Range range;
        public Value value;
        public Resource rangeResource;
        public List<AdditionalNode> additionalNodes;

        public RangeContext(PathContext pathContext, Node node, Range range) {
            this.pathContext = pathContext;
            this.node = node;
            this.range = range;
        }

        public boolean resolve() {
            if (conditionFails(range.target.condition, this)) return false;
            value = range.target.entityElement.getValue(this);
            if (value == null) return false;
            rangeResource = createTypedResource(value.uri, range.target.entityElement.qualifiedName);
            if (rangeResource == null) return false;
            if (value.labelQName != null) {
                rangeResource.addLiteral(
                        createProperty(value.labelQName),
                        createLiteral(value.labelValue)
                );
            }
            additionalNodes = createAdditionalNodes(range.target.additionals, this);
            return true;
        }

        @Override
        public String evaluate(String expression) {
            return valueAt(node, expression);
        }

        @Override
        public Value generateValue(final ValueGenerator valueGenerator, final EntityElement entityElement) {
            return valuePolicy.generateValue(valueGenerator.name, new ValueFunctionArgs() {
                @Override
                public ArgValue getArgValue(String name, SourceType type) {
                    return evaluateArgument(node, valueGenerator, name, type, entityElement.qualifiedName);
                }

                @Override
                public String toString() {
                    return entityElement.toString();
                }
            });
        }

        public void link() {
            pathContext.linkTo(rangeResource);
            for (AdditionalNode additionalNode : additionalNodes) {
                additionalNode.linkFrom(rangeResource);
            }
        }
    }


// =============================================

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

        private AdditionalNode(Additional additional, ValueContext valueContext) {
            this.additional = additional;
            this.valueContext = valueContext;
        }

        public boolean resolve() {
            property = createProperty(additional.propertyElement.qualifiedName);
            if (property == null) return false;
            Value value = additional.entityElement.getValue(valueContext);
            if (value == null) return false;
            resource = createTypedResource(value.uri, additional.entityElement.qualifiedName);
            return resource != null;
        }

        public void linkFrom(Resource fromResource) {
            fromResource.addProperty(property, resource);
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
        if (qualifiedName == null) throw new X3MLException("Missing qualified name");
        String typeUri = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
        return model.createResource(uriString, model.createResource(typeUri + qualifiedName.getLocalName()));
    }

    private Property createProperty(QualifiedName qualifiedName) {
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
