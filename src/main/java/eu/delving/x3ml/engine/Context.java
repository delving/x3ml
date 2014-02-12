package eu.delving.x3ml.engine;

import org.w3c.dom.Node;

public interface Context {

    void setCurrentNode(Node node);

    String getConstant(String name);

    String valueAt(String expression);

    boolean setDomain(Domain domain);

    boolean setProperty(String propertyURI);

    boolean setRange(Entity entity, Path path);

    boolean createTriple();
}
