package eu.delving.x3ml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import eu.delving.x3ml.engine.MappingConstant;
import eu.delving.x3ml.engine.Mappings;
import org.w3c.dom.Element;

import javax.xml.namespace.NamespaceContext;
import java.io.InputStream;
import java.util.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLEngine {

    private Mappings mappings;
    private NamespaceContext namespaceContext = new XPathContext();
    private Map<String,String> constants = new TreeMap<String, String>();

    public static X3MLEngine create(InputStream inputStream) throws X3MLException {
        return new X3MLEngine((Mappings) stream().fromXML(inputStream));
    }

    private X3MLEngine(Mappings mappings) {
        this.mappings = mappings;
        if (this.mappings.mappingConstants != null) {
            for (MappingConstant constant : this.mappings.mappingConstants) {
                constants.put(constant.name, constant.content);
            }
        }
    }

    public void addNamespace(String prefix, String uri) {
        ((XPathContext) namespaceContext).addNamespace(prefix, uri);
    }

    public String extractTriples(Element documentRoot) {
        MappingContext context = new MappingContext(namespaceContext, documentRoot, constants);
        mappings.apply(context);
        return context.toString();
    }

    public String toString() {
        return stream().toXML(mappings);
    }

    // ====================

    private static XStream stream() {
        XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.processAnnotations(Mappings.class);
        return xstream;
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

