package eu.delving.x3ml;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Here we handle turning a DOM node into an XML Document
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class XmlSerializer {

    private XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    private XMLEventFactory eventFactory = XMLEventFactory.newInstance();
    private List<String> indentStrings = new ArrayList<String>();

    public String toXml(Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE)
            throw new IllegalArgumentException("toXml should only be called on an element");
        try {
            Map<String, String> namespaces = new TreeMap<String, String>();
            gatherNamespaces(node, namespaces);
            List<Namespace> nslist = new ArrayList<Namespace>();
            for (Map.Entry<String, String> entry : namespaces.entrySet()) {
                if (entry.getValue().trim().isEmpty()) continue;
                nslist.add(eventFactory.createNamespace(entry.getKey(), entry.getValue()));
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            XMLEventWriter out = outputFactory.createXMLEventWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            out.add(eventFactory.createStartDocument());
            out.add(eventFactory.createCharacters("\n"));
            List<Attribute> attributes = getAttributes(node);
            String prefix = node.getPrefix();
            if (prefix == null) prefix = "";
            String uri = node.getNamespaceURI();
            if (uri == null) uri = "";
            String localName = node.getLocalName();
            if (localName == null) {
                localName = node.getNodeName();
            }
            out.add(eventFactory.createStartElement(prefix, uri, localName, attributes.iterator(), nslist.iterator()));
            out.add(eventFactory.createCharacters("\n"));
            NodeList kids = node.getChildNodes();
            for (int walk = 0; walk < kids.getLength(); walk++) {
                Node kid = kids.item(walk);
                switch (kid.getNodeType()) {
                    case Node.TEXT_NODE:
                    case Node.CDATA_SECTION_NODE:
                    case Node.COMMENT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        nodeToXml(out, kid, 1);
                        break;
                    default:
                        throw new RuntimeException("Node type not implemented: " + kid.getNodeType());
                }
            }
//                for (int walk = 0; walk < kids.getLength(); walk++) {
//                    Node kid = kids.item(walk);
//                    switch (kid.getNodeType()) {
//                        case Node.TEXT_NODE:
//                            out.add(eventFactory.createCharacters(kid.getTextContent()));
//                            break;
//                        case Node.CDATA_SECTION_NODE:
//                            out.add(eventFactory.createCData(kid.getTextContent()));
//                            break;
//                        case Node.ATTRIBUTE_NODE:
//                            break;
//                        case Node.ELEMENT_NODE:
//                            nodeToXml(out, kid, 0);
//                            break;
//                        default:
//                            throw new RuntimeException("Node type not implemented: " + kid.getNodeType());
//                    }
//                }
            out.add(eventFactory.createCharacters("\n"));
            out.add(eventFactory.createEndElement(prefix, uri, localName));
            out.add(eventFactory.createEndDocument());
            out.flush();
            return new String(outputStream.toByteArray(), "UTF-8");
        }
        catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void nodeToXml(XMLEventWriter out, Node node, int level) throws XMLStreamException {
        if (node.getLocalName() == null) return;
        List<Attribute> attributes = getAttributes(node);
        String indentString = level > 0 ? indentString(level) : null;
        if (indentString != null) out.add(eventFactory.createCharacters(indentString));
        out.add(eventFactory.createStartElement(
                node.getPrefix() == null ? "" : node.getPrefix(),
                node.getNamespaceURI() == null ? "" : node.getNamespaceURI(),
                node.getLocalName(),
                level > 0 ? attributes.iterator() : null,
                null
        ));
        NodeList kids = node.getChildNodes();
        boolean nodeHasSubelement = false;
        for (int walk = 0; walk < kids.getLength(); walk++) {
            if (kids.item(walk).getNodeType() == Node.ELEMENT_NODE) {
                nodeHasSubelement = true;
                break;
            }
        }
//        if (nodeHasSubelement) out.add(eventFactory.createCharacters("\n"));
        for (int walk = 0; walk < kids.getLength(); walk++) {
            Node kid = kids.item(walk);
            switch (kid.getNodeType()) {
                case Node.TEXT_NODE:
                    out.add(eventFactory.createCharacters(kid.getTextContent()));
                    break;
                case Node.CDATA_SECTION_NODE:
                    out.add(eventFactory.createCData(kid.getTextContent()));
                    break;
                case Node.ATTRIBUTE_NODE:
                    break;
                case Node.ELEMENT_NODE:
                    nodeToXml(out, kid, level + 1);
                    break;
            }
        }
        if (nodeHasSubelement && indentString != null) out.add(eventFactory.createCharacters(indentString));
        out.add(eventFactory.createEndElement(node.getPrefix(), node.getNamespaceURI(), node.getLocalName()));
    }

    private List<Attribute> getAttributes(Node node) {
        NamedNodeMap nodeAttributes = node.getAttributes();
        List<Attribute> attributes = new ArrayList<Attribute>();
        for (int walk = 0; walk < nodeAttributes.getLength(); walk++) {
            Node attrItem = nodeAttributes.item(walk);
            if (attrItem.getPrefix() == null || attrItem.getPrefix().isEmpty()) {
                attributes.add(eventFactory.createAttribute(attrItem.getNodeName(), attrItem.getNodeValue()));
            }
            else {
                attributes.add(eventFactory.createAttribute(
                        attrItem.getPrefix(), attrItem.getNamespaceURI(), attrItem.getLocalName(),
                        attrItem.getNodeValue()
                ));
            }
        }
        return attributes;
    }

    private String indentString(int level) {
        if (level >= indentStrings.size()) {
            StringBuilder indentBuilder = new StringBuilder(level * 4);
            for (int walk = 0; walk < level + 1; walk++) {
                if (walk == indentStrings.size()) indentStrings.add(indentBuilder.toString());
                indentBuilder.append("    ");
            }
        }
        return indentStrings.get(level);
    }

    private void gatherNamespaces(Node node, Map<String, String> namespaces) {
        if (node.getPrefix() != null && node.getNamespaceURI() != null) {
            namespaces.put(node.getPrefix(), node.getNamespaceURI());
        }
        if (node instanceof Element) {
            Element element = (Element) node;
            NamedNodeMap attrs = element.getAttributes();
            for (int walk = 0; walk < attrs.getLength(); walk++) {
                if (attrs.item(walk).getPrefix() == null) continue;
                namespaces.put(attrs.item(walk).getPrefix(), attrs.item(walk).getNamespaceURI());
            }
        }
        NodeList list = node.getChildNodes();
        for (int walk = 0; walk < list.getLength(); walk++) {
            Node sub = list.item(walk);
            gatherNamespaces(sub, namespaces);
        }
    }

    public static DocumentBuilderFactory documentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory;
    }


}
