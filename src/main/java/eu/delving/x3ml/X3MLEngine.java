package eu.delving.x3ml;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import static eu.delving.x3ml.X3ML.Helper.x3mlStream;
import static eu.delving.x3ml.X3ML.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLEngine {

    private Root root;
    private NamespaceContext namespaceContext = new XPathContext();
    private List<String> prefixes = new ArrayList<String>();

    public static List<String> validate(InputStream inputStream) {
        try {
            return (new X3MLValidator().validate(inputStream));
        }
        catch (SAXException e) {
            throw new X3MLException("Unable to validate: SAX", e);
        }
        catch (IOException e) {
            throw new X3MLException("Unable to validate: IO", e);
        }
    }

    public static X3MLEngine load(InputStream inputStream) throws X3MLException {
        return new X3MLEngine((Root) x3mlStream().fromXML(inputStream));
    }

    public static void save(X3MLEngine engine, OutputStream outputStream) throws X3MLException {
        x3mlStream().toXML(engine.root, outputStream);
    }

    private X3MLEngine(Root root) {
        this.root = root;
        if (this.root.namespaces != null) {
            for (MappingNamespace namespace : this.root.namespaces) {
                ((XPathContext) namespaceContext).addNamespace(namespace.prefix, namespace.uri);
                prefixes.add(namespace.prefix);
            }
        }
    }

    public X3MLContext execute(Element sourceRoot, ValuePolicy valuePolicy) throws X3MLException {
        X3MLContext context = new X3MLContext(sourceRoot, this.root, valuePolicy, namespaceContext, prefixes);
        root.apply(context);
        return context;
    }

    public String toString() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + x3mlStream().toXML(root);
    }

    // ====================

    private class XPathContext implements NamespaceContext {
        private Map<String, String> prefixUri = new TreeMap<String, String>();
        private Map<String, String> uriPrefix = new TreeMap<String, String>();

        void addNamespace(String prefix, String uri) {
            prefixUri.put(prefix, uri);
            uriPrefix.put(uri, prefix);
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new X3MLException("Null prefix!");
            }
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

