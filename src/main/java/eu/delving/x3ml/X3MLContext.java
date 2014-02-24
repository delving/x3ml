package eu.delving.x3ml;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
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
import java.util.Map;

/**
 * The context in which the engine acts
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLContext implements X3ML {
    private final Element documentRoot;
    private final URIPolicy uriPolicy;
    private final Logger log = Logger.getLogger(getClass());
    private NamespaceContext namespaceContext;
    private String tagPrefix;
    private Map<String, String> constants;
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

    public void setNamespaceContext(NamespaceContext namespaceContext, List<String> prefixes, String tagPrefix) {
        this.namespaceContext = namespaceContext;
        for (String prefix : prefixes) {
            this.model.setNsPrefix(prefix, namespaceContext.getNamespaceURI(prefix));
        }
        this.tagPrefix = tagPrefix;
    }

    public void setConstants(Map<String, String> constants) {
        this.constants = constants;
    }

    public void finished() {
        this.finished = true;
    }

    public void checkNotFinished() throws X3MLException {
        if (finished) throw new X3MLException("Job was already finished");
    }

    public void write(PrintStream out) {
        model.write(out, "TURTLE");
    }

    public String toString() {
        return "X3MLContext";
    }

    // ===== calls made from within X3ML.* classes ====

    public String getConstant(String name) {
        return constants.get(name);
    }

    public List<DomainContext> createDomainContexts(Domain domain) {
        List<Node> domainNodes = nodeList(documentRoot, domain.source);
        List<DomainContext> domainContexts = new ArrayList<DomainContext>();
        for (Node domainNode : domainNodes) {
            DomainContext domainContext = new DomainContext(domain, domainNode);
            if (domainContext.generateUri()) {
                domainContexts.add(domainContext);
            }
        }
        return domainContexts;
    }

    public class DomainContext {
        public final Domain domain;
        public final Node node;
        public String uri;
        public Resource resource;

        public DomainContext(Domain domain, Node node) {
            this.domain = domain;
            this.node = node;
        }

        public boolean generateUri() {
            this.uri = domain.entityElement.generateDomainURI(this);
            if (this.uri == null) return false;
            log.info("Domain.createResource: [" + this.uri + "]");
            resource = model.createResource(tagPrefix + ":" + this.uri);
            return true;
        }

        public String generateUri(final URIFunction uriFunction) {
            return uriPolicy.generateUri(uriFunction.name, new URIArguments() {
                @Override
                public String getArgument(String name) {
                    if (CLASS_NAME.equals(name)) {
                        return domain.entityElement.tag;
                    }
                    if (uriFunction.args != null) {
                        for (URIFunctionArg arg : uriFunction.args) {
                            if (arg.name.equals(name)) {
                                return valueAt(node, arg);
                            }
                        }
                    }
                    String constantValue = constants.get(name);
                    if (constantValue != null) return constantValue;
                    return valueAt(node, "text()");
                }
            });
        }

        public List<PathContext> createPathContexts(Path path) {
            List<Node> pathNodes = nodeList(node, path.source);
            List<PathContext> pathContexts = new ArrayList<PathContext>();
            for (Node pathNode : pathNodes) {
                PathContext pathContext = new PathContext(this, pathNode, path);
                if (pathContext.generateUri()) {
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
        public String uri;
        public Property property;

        public PathContext(DomainContext domainContext, Node node, Path path) {
            this.domainContext = domainContext;
            this.node = node;
            this.path = path;
        }

        public boolean generateUri() {
            this.uri = path.propertyElement.getPropertyURI(this);
            if (this.uri == null) return false;
            log.info("Path.createProperty: [" + this.uri + "]");
            this.property = model.createProperty(namespaceContext.getNamespaceURI(tagPrefix), this.uri);
            return true;
        }

        public List<RangeContext> createRangeContexts(Range range) {
            List<Node> rangeNodes = nodeList(node, range.source);
            List<RangeContext> rangeContexts = new ArrayList<RangeContext>();
            for (Node rangeNode : rangeNodes) {
                RangeContext rangeContext = new RangeContext(this, rangeNode, range);
                if (rangeContext.generateUri()) {
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
        public String uri;
        public Resource resource;

        public RangeContext(PathContext pathContext, Node node, Range range) {
            this.pathContext = pathContext;
            this.node = node;
            this.range = range;
        }

        public boolean generateUri() {
            this.uri = range.entityElement.generateRangeUri(this);
            if (this.uri == null) return false;
            log.info("Range.createResource: [" + this.uri + "]");
            resource = model.createResource(tagPrefix + ":" + this.uri);
            return true;
        }

        public String generateUri(final URIFunction uriFunction) {
            return uriPolicy.generateUri(uriFunction.name, new URIArguments() {
                @Override
                public String getArgument(String name) {
                    if (CLASS_NAME.equals(name)) {
                        return range.entityElement.tag;
                    }
                    if (uriFunction.args != null) {
                        for (URIFunctionArg arg : uriFunction.args) {
                            if (arg.name.equals(name)) {
                                return valueAt(node, arg);
                            }
                        }
                    }
                    String constantValue = constants.get(name);
                    if (constantValue != null) return constantValue;
                    return valueAt(node, "text()");
                }
            });
        }

        public void generateTriple() {
            pathContext.domainContext.resource.addProperty(pathContext.property, resource);
        }
    }

    // =============================================

    private String valueAt(Node node, URIFunctionArg arg) {
        if (arg.expression == null || arg.expression.isEmpty()) {
            return constants.get(arg.name);
        }
        else {
            return valueAt(node, arg.expression);
        }
    }

    private String valueAt(Node node, String expression) {
        List<Node> nodes = nodeList(node, expression);
        if (nodes.isEmpty()) return "";
        String value = nodes.get(0).getNodeValue();
        if (value == null) return "";
        return value.trim();
    }

    private List<Node> nodeList(Node context, String expressionString) {
        if (expressionString == null) {
            List<Node> list = new ArrayList<Node>(1);
            list.add(context);
            return list;
        }
        try {
            XPathExpression expression = path().compile(expressionString);
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
