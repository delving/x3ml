package eu.delving.x3ml;

import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import eu.delving.x3ml.engine.Context;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The context in which the engine acts
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class MappingContext implements Context {
    private final NamespaceContext namespaceContext;
    private final Map<String, String> constants;
    private final Element documentRoot;
    private Node node;
    private XPathFactory pathFactory = new XPathFactoryImpl();
    private Map<String, String> pathUri = new HashMap<String, String>();
    private List<GraphTriple> triples = new ArrayList<GraphTriple>();

    public MappingContext(NamespaceContext namespaceContext, Element documentRoot, Map<String,String> constants) {
        this.namespaceContext = namespaceContext;
        this.node = this.documentRoot = documentRoot;
        this.constants = constants;
    }

    @Override
    public void setNode(Node node) {
        this.node = (node == null) ? documentRoot : node;
    }

    @Override
    public String getConstant(String name) {
        return constants.get(name);
    }

    @Override
    public String valueAt(String expression) {
        List<Node> nodes = nodeList(node, expression);
        if (nodes.isEmpty()) return "";
        String value = nodes.get(0).getNodeValue();
        if (value == null) return "";
        return value.trim();
    }

    @Override
    public GraphEntity entity(String entityClass, String path, String generatedUri) {
        String uri = pathUri.get(path);
        if (uri == null) {
            pathUri.put(path, uri = generatedUri);
        }
        return new GraphEntityImpl(entityClass, uri);
    }

    @Override
    public GraphTriple triple(GraphEntity subject, String predicate, GraphEntity object) {
        GraphTripleImpl triple = new GraphTripleImpl(subject, predicate, object);
        triples.add(triple);
        return triple;
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

    private static class GraphTripleImpl implements Context.GraphTriple {
        public final GraphEntityImpl subject;
        public final String predicate;
        public final GraphEntityImpl object;

        public GraphTripleImpl(Context.GraphEntity subject, String predicate, Context.GraphEntity object) {
            this.subject = (GraphEntityImpl) subject;
            this.predicate = predicate;
            this.object = (GraphEntityImpl) object;
        }

        public String toString() {
            return String.format("%s - %s - %s", subject, predicate, object);
        }
    }

    private class GraphEntityImpl implements Context.GraphEntity {
        public final String entityClass;
        public final String uri;

        public GraphEntityImpl(String entityClass, String uri) {
            this.entityClass = entityClass;
            this.uri = uri;
        }

        public String toString() {
            return String.format("Entity(%s,%s)", entityClass, uri);
        }
    }


}
