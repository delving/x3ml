package eu.delving.x3ml;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;

import javax.xml.namespace.NamespaceContext;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLEngine {

    private X3ML.Mappings mappings;
    private NamespaceContext namespaceContext = new XPathContext();
    private Map<String,String> constants = new TreeMap<String, String>();

    public static X3MLEngine load(InputStream inputStream) throws X3MLException {
        return new X3MLEngine((X3ML.Mappings) stream().fromXML(inputStream));
    }

    public static void save(X3MLEngine engine, OutputStream outputStream) throws X3MLException {
        stream().toXML(engine.mappings, outputStream);
    }

    private X3MLEngine(X3ML.Mappings mappings) {
        this.mappings = mappings;
        if (this.mappings.mappingConstants != null) {
            for (X3ML.MappingConstant constant : this.mappings.mappingConstants) {
                constants.put(constant.name, constant.content);
            }
        }
        if (this.mappings.mappingNamespaces != null) {
            for (X3ML.MappingNamespace namespace : this.mappings.mappingNamespaces) {
                ((XPathContext) namespaceContext).addNamespace(namespace.prefix, namespace.uri);
            }
        }
    }

    public void execute(X3MLContext context) throws X3MLException {
        context.checkNotFinished();
        context.setNamespaceContext(namespaceContext);
        context.setConstants(constants);
        mappings.apply(context);
        context.finished();
    }

    public String toString() {
        return stream().toXML(mappings);
    }

    // ====================

    private static XStream stream() {
        XStream xstream = new XStream(new PureJavaReflectionProvider(), new XppDriver(new NoNameCoder()));
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.processAnnotations(X3ML.Mappings.class);
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

