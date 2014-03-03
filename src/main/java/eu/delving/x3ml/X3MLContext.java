package eu.delving.x3ml;

import com.hp.hpl.jena.rdf.model.*;
import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
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
    private final SourceType sourceType;
    private final ValuePolicy valuePolicy;
    private NamespaceContext namespaceContext;
    private XPathFactory pathFactory = new XPathFactoryImpl();
    private Model model = ModelFactory.createDefaultModel();

    X3MLContext(Element documentRoot, SourceType sourceType, ValuePolicy valuePolicy) {
        this.documentRoot = documentRoot;
        this.sourceType = sourceType;
        this.valuePolicy = valuePolicy;
    }

    public void setNamespaceContext(NamespaceContext namespaceContext, List<String> prefixes) {
        this.namespaceContext = namespaceContext;
        for (String prefix : prefixes) {
            this.model.setNsPrefix(prefix, namespaceContext.getNamespaceURI(prefix));
        }
    }

    public void write(PrintStream out) {
        model.write(out, "RDF/XML-ABBREV");
    }

    public String[] toStringArray() {
        return toString().split("\n");
    }

    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        model.write(baos, "N-TRIPLE");
        return new String(baos.toByteArray());
    }

    // ===== calls made from within X3ML.* classes ====

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

    private ArgValue evaluate(Node contextNode, QualifiedName qualifiedName, ValueGenerator function, String argName, SourceType type) {
        ValueFunctionArg foundArg = null;
        if (function.args != null) {
            for (ValueFunctionArg arg : function.args) {
                if (arg.name.equals(argName)) foundArg = arg;
            }
        }
        if (foundArg == null) {
            switch (type) {
                case XPATH:
                    foundArg = new ValueFunctionArg();
                    foundArg.name = argName;
                    foundArg.value = "text()";
                    break;
                case QNAME:
                    if (qualifiedName == null) {
                        throw new X3MLException("Qualified name expected");
                    }
                    foundArg = new ValueFunctionArg();
                    foundArg.name = argName;
                    foundArg.value = qualifiedName.tag;
                    break;
                default:
                    throw new X3MLException("Not implemented");
            }
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

    public class DomainContext {
        public final Domain domain;
        public final Node node;
        public Value value;

        public DomainContext(Domain domain, Node node) {
            this.domain = domain;
            this.node = node;
        }

        public boolean resolve() {
            this.value = domain.target.entityElement.getValue(this);
            return this.value != null;
        }

        public Value generateValue(final ValueGenerator valueGenerator) {
            return valuePolicy.generateValue(valueGenerator.name, new ValueFunctionArgs() {
                @Override
                public ArgValue getArgValue(String name, SourceType type) {
                    QualifiedName qualifiedName = domain.target.entityElement.qualifiedName;
                    return evaluate(node, qualifiedName, valueGenerator, name, type);
                }
            });
        }

        public List<PathContext> createPathContexts(Path path) {
            List<Node> pathNodes = nodeList(node, path.source);
            List<PathContext> pathContexts = new ArrayList<PathContext>();
            for (Node pathNode : pathNodes) {
                PathContext pathContext = new PathContext(this, pathNode, path);
                if (pathContext.generateProperty()) {
                    pathContexts.add(pathContext);
                }
            }
            return pathContexts;
        }
    }

    public class PathContext {
        public final DomainContext domainContext;
        public final Node node;
        public final Path path;
        public QualifiedName qualifiedName;
        public Property property;

        public PathContext(DomainContext domainContext, Node node, Path path) {
            this.domainContext = domainContext;
            this.node = node;
            this.path = path;
        }

        public boolean generateProperty() {
            qualifiedName = path.target.propertyElement.getPropertyClass(this);
            if (qualifiedName == null) return false;
            this.property = model.createProperty(namespaceContext.getNamespaceURI(qualifiedName.getPrefix()), qualifiedName.getLocalName());
            return true;
        }

        public List<RangeContext> createRangeContexts(Range range) {
            List<Node> rangeNodes = nodeList(node, range.source);
            List<RangeContext> rangeContexts = new ArrayList<RangeContext>();
            for (Node rangeNode : rangeNodes) {
                RangeContext rangeContext = new RangeContext(this, rangeNode, range);
                if (rangeContext.resolve()) {
                    rangeContexts.add(rangeContext);
                }
            }
            return rangeContexts;
        }
    }

    public class RangeContext {
        public final PathContext pathContext;
        public final Node node;
        public final Range range;
        public Value value;

        public RangeContext(PathContext pathContext, Node node, Range range) {
            this.pathContext = pathContext;
            this.node = node;
            this.range = range;
        }

        public boolean resolve() {
            this.value = range.target.entityElement.getValue(this);
            return this.value != null;
        }

        public Value generateValue(final ValueGenerator valueGenerator, final EntityElement entityElement) {
            return valuePolicy.generateValue(valueGenerator.name, new ValueFunctionArgs() {
                @Override
                public ArgValue getArgValue(String name, SourceType type) {
                    return evaluate(node, entityElement.qualifiedName, valueGenerator, name, type);
                }
            });
        }

        public void generate() {
            Resource domainResource = createTypedResource(
                    pathContext.domainContext.value.uri,
                    pathContext.domainContext.domain.target.entityElement.qualifiedName
            );
            Property property = createProperty(pathContext.qualifiedName);
            Resource rangeResource = createTypedResource(
                    value.uri,
                    range.target.entityElement.qualifiedName
            );
            domainResource.addProperty(property, rangeResource);
            if (value.labelQName != null && value.labelValue != null) {
                Property labelProperty = createProperty(value.labelQName);
                rangeResource.addProperty(labelProperty, value.labelValue);
            }
            if (range.target.additional != null) {
                Property additionalProperty = createProperty(
                        range.target.additional.propertyElement.qualifiedName
                );
                Value additionalValue = range.target.additional.entityElement.getValue(this);
                Resource additionalResource = createTypedResource(
                        additionalValue.uri,
                        range.target.additional.entityElement.qualifiedName
                );
                rangeResource.addProperty(additionalProperty, additionalResource);
            }
        }
    }

    private Resource createTypedResource(String uriString, QualifiedName qualifiedName) {
        if (qualifiedName == null) throw new X3MLException("no class element");
        String typeUri = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
        return model.createResource(uriString, model.createResource(typeUri + qualifiedName.getLocalName()));
    }

    private Property createProperty(QualifiedName qualifiedName) {
        String propertyNamespace = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
        return model.createProperty(propertyNamespace, qualifiedName.getLocalName());
    }

// =============================================

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
        if (expression == null) {
            List<Node> list = new ArrayList<Node>(1);
            list.add(context);
            return list;
        }
        try {
            XPathExpression xe = path().compile(expression);
            NodeList nodeList = (NodeList) xe.evaluate(context, XPathConstants.NODESET);
            List<Node> list = new ArrayList<Node>(nodeList.getLength());
            for (int index = 0; index < nodeList.getLength(); index++) list.add(nodeList.item(index));
            return list;
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException("XPath Problem", e);
        }
    }

    private XPath path() {
        if (sourceType != SourceType.XPATH) throw new X3MLException("Only sourceType=\"XPATH\" is implemented");
        XPath path = pathFactory.newXPath();
        path.setNamespaceContext(namespaceContext);
        return path;
    }
}
