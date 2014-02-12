package eu.delving.x3ml;

import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import eu.delving.x3ml.engine.Context;
import eu.delving.x3ml.engine.Domain;
import eu.delving.x3ml.engine.Entity;
import eu.delving.x3ml.engine.Path;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The context in which the engine acts
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLJob {
    private final Element documentRoot;
    private NamespaceContext namespaceContext;
    private Map<String, String> constants;
    private XPathFactory pathFactory = new XPathFactoryImpl();
    private List<GraphTriple> triples = new ArrayList<GraphTriple>();
    private MappingContext context;
    private boolean finished = false;

    public static X3MLJob create(Element documentRoot) {
        return new X3MLJob(documentRoot);
    }

    private X3MLJob(Element documentRoot) {
        this.context = new MappingContext(this.documentRoot = documentRoot);
    }

    public Context getContext() {
        return context;
    }

    public Element getDocumentRoot() {
        return documentRoot;
    }

    public void setNamespaceContext(NamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
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

    private List<Node> nodeList(Node context, String expressionString) {
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

    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("graph:\n");
        for (GraphTriple triple : triples) {
            out.append(triple).append("\n");
        }
        return out.toString();
    }

    private XPath path() {
        XPath path = pathFactory.newXPath();
        path.setNamespaceContext(namespaceContext);
        return path;
    }

    private class MappingContext implements Context {
        private Node currentNode;
        private GraphEntity domainEntity;
        private String propertyURI;
        private GraphEntity rangeEntity;

        private MappingContext(Node currentNode) {
            this.currentNode = currentNode;
        }

        @Override
        public void setCurrentNode(Node currentNode) {
            this.currentNode = currentNode;
        }

        @Override
        public String getConstant(String name) {
            return constants.get(name);
        }

        @Override
        public String valueAt(String expression) {
            List<Node> nodes = nodeList(currentNode, expression);
            if (nodes.isEmpty()) return "";
            String value = nodes.get(0).getNodeValue();
            if (value == null) return "";
            return value.trim();
        }

        @Override
        public boolean createTriple() {
            if (domainEntity == null || propertyURI == null || rangeEntity == null) return false;
            triples.add(new GraphTriple(domainEntity, propertyURI, rangeEntity));
            return true;
        }

        @Override
        public boolean setDomain(Domain domain) {
            String uri = domain.entity.generateDomainURI(this, domain);
            if (uri != null) {
                this.domainEntity = new GraphEntity(domain.entity, uri);
                // todo: drop a breadcrumb in the DOM to mark the spot
                return true;
            }
            else {
                return false;
            }
        }

        @Override
        public boolean setProperty(String propertyURI) {
            return (this.propertyURI = propertyURI) != null;
        }

        @Override
        public boolean setRange(Entity entity, Path path) {
            String rangeURI = entity.generateRangeUri(this, domainEntity.entity, path);
            if (rangeURI != null) {
                this.rangeEntity = new GraphEntity(entity, rangeURI);
                return true;
            }
            else {
                return false;
            }
        }
    }

    private class GraphEntity {
        public final Entity entity;
        public final String uri;

        public GraphEntity(Entity entity, String uri) {
            this.entity = entity;
            this.uri = uri;
        }

        public String toString() {
            return String.format("Entity(%s,%s)", entity.tag, uri);
        }
    }

    private static class GraphTriple {
        public final GraphEntity subject;
        public final String predicate;
        public final GraphEntity object;

        public GraphTriple(GraphEntity subject, String predicate, GraphEntity object) {
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
        }

        public String toString() {
            return String.format("%s - %s - %s", subject, predicate, object);
        }
    }
}
