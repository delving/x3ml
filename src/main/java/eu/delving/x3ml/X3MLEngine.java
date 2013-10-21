package eu.delving.x3ml;

import com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import eu.delving.x3ml.engine.Context;
import eu.delving.x3ml.engine.Mappings;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.io.InputStream;
import java.util.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLEngine {

    private Mappings mappings;
    private NamespaceContext namespaceContext = new XPathContext();
    private XPathFactory pathFactory = new XPathFactoryImpl();

    public static X3MLEngine create(InputStream inputStream) throws X3MLException {
        return new X3MLEngine((Mappings) stream().fromXML(inputStream));
    }

    private X3MLEngine(Mappings mappings) {
        this.mappings = mappings;
    }

    public void addNamespace(String prefix, String uri) {
        ((XPathContext) namespaceContext).addNamespace(prefix, uri);
    }

    public String extractTriples(Element document) {
        ContextImpl context = new ContextImpl(document);
        mappings.apply(context);
        return context.toString();
    }

    public String toString() {
        return stream().toXML(mappings);
    }

    // ====================

    private class ContextImpl implements Context {
        private Element document;
        private Map<String, String> pathUri = new HashMap<String, String>();
        private List<Context.GraphTriple> triples = new ArrayList<Context.GraphTriple>();

        private ContextImpl(Element document) {
            this.document = document;
        }

        @Override
        public String valueAt(Node node, String expression) {
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
            for (Context.GraphTriple triple : triples) {
                out.append(triple).append("\n");
            }
            return out.toString();
        }
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

    private static XStream stream() {
        XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.processAnnotations(Mappings.class);
        return xstream;
    }

    private XPath path() {
        XPath path = pathFactory.newXPath();
        path.setNamespaceContext(namespaceContext);
        return path;
    }

    private class XPathContext implements NamespaceContext {
        private Map<String, String> prefixUri = new TreeMap<String, String>();
        private Map<String, String> uriPrefix = new TreeMap<String, String>();

        void addNamespace(String prefix, String uri) {
            prefixUri.put(prefix, uri);
            uriPrefix.put(uri, prefix);
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefixUri.size() == 1) {
                return prefixUri.values().iterator().next();
            }
            return prefixUri.get(prefix);
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return uriPrefix.get(namespaceURI);
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            String prefix = getPrefix(namespaceURI);
            if (prefix == null) return null;
            List<String> list = new ArrayList<String>();
            list.add(prefix);
            return list.iterator();
        }
    }

}

// =======================================================================
// Legacy work-around efforts to study:
//    public String apply(Node node, String className) {
//        try {
//            if ("Appellation".equals(name)) {
//                // appellationURI(String className, String subjUri, String appellation)
//                argList.add(className);
////                    argList.add(domainMapResult.uri);
//                fetchArgs(node);
//            }
//            else if ("createLiteral".equals(name)) {
//                // createLiteral(String className, String type, String note)
//                argList.add(UNUSED_CLASS_NAME);
//                argList.add(node.getNodeName());
//                fetchArgs(node);
//            }
//            else if ("dimensionURI".equals(name)) {
//                // dimensionURI(String className, String subjUri, String dimensions)
//                argList.add(UNUSED_CLASS_NAME);
////                    argList.add(domainMapResult.uri);
//                fetchArgs(node);
//            }
//            else if ("uriConceptual".equals(name)) {
//                // uriConceptual(String className, String thing)
//                argList.add(className);
//                fetchArgs(node);
//            }
//            else if ("uriEvents".equals(name)) {
//                // uriEvents(String className, String authority, String eventID, String subjUri)
//                argList.add(className);
////                    argList.add(domainMapResult.uri);
//                fetchArgs(node);
//            }
//            else if ("uriForActors".equals(name)) {
//                // uriForActors(String className, String authority, String id, String name, String birthDate)
//                argList.add(className);
//                fetchArgs(node);
//            }
//            else if ("PhysicalObject".equals(name)) {
//                // uriForPhysicalObjects(String className, String nameOfMuseum, String entry)
//                argList.add(UNUSED_CLASS_NAME);
//                fetchArgs(node);
//            }
//            else if ("Place".equals(name)) {
//                // uriForPlaces(String className, String placeName, String authority, String placeID,
//                //              Stribng coordinates, String spaces)
//                argList.add(UNUSED_CLASS_NAME);
//                fetchArg(node, 0);
//                fetchArg(node, 1);
//                fetchArg(node, 2);
//                fetchArg(node, 3); // coordinates never really used
//                argList.add(getPartOfPlaceHack(node));
//            }
//            else if ("PhysicalThing".equals(name)) {
//                // uriPhysThing(String className, String thing)
//                argList.add(className);
//                fetchArgs(node);
//            }
//            else if ("uriTimeSpan".equals(name)) {
//                // uriTimeSpan(String className, String timespan)
//                argList.add(UNUSED_CLASS_NAME);
//                fetchArgs(node);
//            }
//            else if ("Type".equals(name)) {
//                // uriType(String className, String type)
//                argList.add(className);
//                fetchArgs(node);
//            }
//            else {
//                throw new RuntimeException("Unknown function name: " + name);
//            }
//            Class<?>[] types = new Class<?>[argList.size()];
//            Arrays.fill(types, String.class);
//            try {
//                Method method = POLICIES.getClass().getMethod(name, types);
//                return (String) method.invoke(POLICIES, argList.toArray());
//            }
//            catch (NoSuchMethodException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void fetchArgs(Node node) {
//        for (URIFunctionArg a : args) {
//            argList.add(valueAt(node, a.content));
//        }
//    }
//
//    private void fetchArg(Node node, int index) {
//        argList.add(valueAt(node, args.get(index).content));
//    }
//
//    private String getPartOfPlaceHack(Node node) {
//        try { // iterate into partOfPlace fetching names and then join them with dash
//            List<String> places = new ArrayList<String>();
//            while (node != null) {
//                XPathExpression expr = path().compile("lido:namePlaceSet/lido:appellationValue/text()");
//                String placeName = (String) expr.evaluate(node, XPathConstants.STRING);
//                places.add(placeName);
//                expr = path().compile("lido:partOfPlace");
//                node = (Node) expr.evaluate(node, XPathConstants.NODE);
//            }
//            return StringUtils.join(places, '-');
//        }
//        catch (XPathExpressionException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
