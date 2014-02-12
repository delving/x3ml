package eu.delving.x3ml.engine;

import org.w3c.dom.Node;

public interface Context {

    void setCurrentNode(Node node);

    String getConstant(String name);

    String valueAt(String expression);

    GraphEntity entity(String entityClass, String path, String generatedUri);

    GraphTriple triple(GraphEntity subject, String predicate, GraphEntity object);

    public interface GraphEntity {
    }

    public interface GraphTriple {
    }
}
