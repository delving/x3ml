package eu.delving.x3ml;

import com.hp.hpl.jena.rdf.model.*;
import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The context in which the engine acts
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLContext implements X3ML {
    public static final XPathElement TEXT_XPATH = new XPathElement("text()");
    private final Element documentRoot;
    private final URIPolicy uriPolicy;
    private final Logger log = Logger.getLogger(getClass());
    private NamespaceContext namespaceContext;
    private XPathFactory pathFactory = new XPathFactoryImpl();
    private Model model = ModelFactory.createDefaultModel();
    private boolean finished = false;

    public static X3MLContext create(Element documentRoot, URIPolicy uriPolicy) {
        return new X3MLContext(documentRoot, uriPolicy);
    }

    private X3MLContext(Element documentRoot, URIPolicy uriPolicy) {
        this.documentRoot = documentRoot;
        this.uriPolicy = uriPolicy;
    }

    public void setNamespaceContext(NamespaceContext namespaceContext, List<String> prefixes) {
        this.namespaceContext = namespaceContext;
        for (String prefix : prefixes) {
            this.model.setNsPrefix(prefix, namespaceContext.getNamespaceURI(prefix));
        }
    }

    public void finished() {
        this.finished = true;
    }

    public void checkNotFinished() throws X3MLException {
        if (finished) throw new X3MLException("Job was already finished");
    }

    public void write(PrintStream out) {
        model.write(out, "RDF/XML-ABBREV");
    }

    public String toString() {
        return "X3MLContext";
    }

    // ===== calls made from within X3ML.* classes ====

    public List<DomainContext> createDomainContexts(Domain domain) {
        List<Node> domainNodes = nodeList(documentRoot, domain.source.xpath);
        List<DomainContext> domainContexts = new ArrayList<DomainContext>();
        for (Node domainNode : domainNodes) {
            DomainContext domainContext = new DomainContext(domain, domainNode);
            if (domainContext.resolve()) {
                domainContexts.add(domainContext);
            }
        }
        return domainContexts;
    }

    private Resource createTypedResource(String uriString, QualifiedName qualifiedName) {
        if (qualifiedName == null) throw new X3MLException("no class element");
        String typeUri = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
        return model.createResource(uriString, model.createResource(typeUri+ qualifiedName.getLocalName()));
    }

    private Property createLiteralProperty(QualifiedName qualifiedName) {
        String propertyNamespace = namespaceContext.getNamespaceURI(qualifiedName.getPrefix());
        return model.createProperty(propertyNamespace, qualifiedName.getLocalName());
    }

    public class EntityResolution {
        public QualifiedName qualifiedName;
        public String literalString;
        public String resourceString;
        public Resource resource;
        public Property literalProperty;
        public Literal literal;

        public boolean resolve() {
            if (literalString != null) {
                literalProperty = createLiteralProperty(qualifiedName);
                literal = model.createLiteral(literalString);
                return true;
            }
            else if (resourceString != null) {
                resource = createTypedResource(resourceString, qualifiedName);
                return true;
            }
            else {
                return false;
            }
        }
    }

    public class DomainContext {
        public final Domain domain;
        public final Node node;
        public EntityResolution resolution;

        public DomainContext(Domain domain, Node node) {
            this.domain = domain;
            this.node = node;
        }

        public boolean resolve() {
            this.resolution = domain.target.entityElement.getResolution(this);
            return this.resolution.resolve();
        }

        public String dereference(XPathElement xpath) {
            return valueAt(node, xpath);
        }

        public String generateUri(final URIFunction uriFunction) {
            return uriPolicy.generateUri(uriFunction.name, new URIArguments() {
                @Override
                public String getArgument(String name) {
                    if (CLASS_NAME.equals(name)) {
                        if (domain.target.entityElement == null || domain.target.entityElement.qualifiedName == null) {
                            throw new X3MLException("No class element: " + domain);
                        }
                        return domain.target.entityElement.qualifiedName.getLocalName();
                    }
                    if (UUID_NAME.equals(name)) {
                        return UUID.randomUUID().toString();
                    }
                    if (uriFunction.args != null) {
                        for (URIFunctionArg arg : uriFunction.args) {
                            if (arg.name.equals(name)) {
                                return valueAt(node, arg);
                            }
                        }
                    }
                    return valueAt(node);
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

        public EntityResolution createResolution() {
            return new EntityResolution();
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
        public EntityResolution resolution;

        public RangeContext(PathContext pathContext, Node node, Range range) {
            this.pathContext = pathContext;
            this.node = node;
            this.range = range;
        }

        public boolean resolve() {
            this.resolution = range.target.entityElement.getResolution(this);
            // todo: what about range.additionalNode?
            return this.resolution.resolve();
        }

        public String dereference(XPathElement xpath) {
            return valueAt(node, xpath);
        }

        public String generateUri(final URIFunction uriFunction) {
            return uriPolicy.generateUri(uriFunction.name, new URIArguments() {
                @Override
                public String getArgument(String name) {
                    if (CLASS_NAME.equals(name)) {
                        if (range.target.entityElement == null || range.target.entityElement.qualifiedName == null) {
                            throw new X3MLException("No class element: " + range);
                        }
                        return range.target.entityElement.qualifiedName.getLocalName();
                    }
                    if (uriFunction.args != null) {
                        for (URIFunctionArg arg : uriFunction.args) {
                            if (arg.name.equals(name)) {
                                return valueAt(node, arg);
                            }
                        }
                    }
                    return valueAt(node);
                }
            });
        }

        public void generateTriple() {
            Resource domainResource = pathContext.domainContext.resolution.resource;
            if (resolution.literal != null) {
                // todo: make use of resolution.property
                domainResource.addProperty(resolution.literalProperty, resolution.literal);
            }
            else if (resolution.resource != null) {
                domainResource.addProperty(pathContext.property, resolution.resource);
            }
            else {
                throw new X3MLException("Unable to generate triple");
            }
        }

        public EntityResolution createResolution() {
            return new EntityResolution();
        }
    }

    // =============================================

    private String valueAt(Node node) {
        return valueAt(node, TEXT_XPATH);
    }

    private String valueAt(Node node, URIFunctionArg arg) {
        return valueAt(node, arg.xpath);
    }

    private String valueAt(Node node, XPathElement xpath) {
        List<Node> nodes = nodeList(node, xpath);
        if (nodes.isEmpty()) return "";
        String value = nodes.get(0).getNodeValue();
        if (value == null) return "";
        return value.trim();
    }

    private List<Node> nodeList(Node node, Source source) {
        if (source != null) {
            return nodeList(node, source.xpath);
        }
        else {
            List<Node> list = new ArrayList<Node>(1);
            list.add(node);
            return list;
        }
    }

    private List<Node> nodeList(Node context, XPathElement xpath) {
        if (xpath == null || xpath.expression == null) {
            List<Node> list = new ArrayList<Node>(1);
            list.add(context);
            return list;
        }
        try {
            XPathExpression expression = path().compile(xpath.expression);
            NodeList nodeList = (NodeList) expression.evaluate(context, XPathConstants.NODESET);
            List<Node> list = new ArrayList<Node>(nodeList.getLength());
            for (int index = 0; index < nodeList.getLength(); index++) list.add(nodeList.item(index));
            return list;
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException("XPath Problem", e);
        }
    }

    private XPath path() {
        XPath path = pathFactory.newXPath();
        path.setNamespaceContext(namespaceContext);
        return path;
    }
}
