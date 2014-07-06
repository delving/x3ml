//===========================================================================
//    Copyright 2014 Delving B.V.
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//===========================================================================
package eu.delving.x3ml.engine;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.GeneratorElement;
import static eu.delving.x3ml.engine.X3ML.Helper.argVal;
import static eu.delving.x3ml.engine.X3ML.SourceType;

/**
 * The source data is accessed using xpath to fetch nodes from a DOM tree.
 * <p/>
 * Here we have tools for evaluating xpaths in various contexts.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class XPathInput {
    public static final Pattern DOMAIN_PLACEHOLDER = Pattern.compile("(^.*)(DomainContext[(])([^)]*)([)])(.*$)");
    private final XPathFactory pathFactory = net.sf.saxon.xpath.XPathFactoryImpl.newInstance();
    private final NamespaceContext namespaceContext;
    private final String languageFromMapping;

    public XPathInput(NamespaceContext namespaceContext, String languageFromMapping) {
        this.namespaceContext = namespaceContext;
        this.languageFromMapping = languageFromMapping;
    }

    public X3ML.ArgValue evaluateArgument(Node contextNode, Node domainContextNode, int index, GeneratorElement generatorElement, String argName, SourceType defaultType) {
        X3ML.GeneratorArg foundArg = null;
        SourceType type = defaultType;
        if (generatorElement.args != null) {
            for (X3ML.GeneratorArg arg : generatorElement.args) {
                if (arg.name == null) {
//                    throw exception("Argument needs a name in generator "+generatorElement.name);
                    arg.name = "text";
                }
                if (arg.name.equals(argName)) {
                    foundArg = arg;
                    type = sourceType(arg.type, defaultType);
                }
            }
        }
        X3ML.ArgValue value = null;
        switch (type) {
            case xpath:
                if (foundArg == null) return null;
                String lang = getLanguageFromSource(contextNode);
                if (lang == null) lang = languageFromMapping;
                if (!foundArg.value.isEmpty()) {
                    value = argVal(valueAt(contextNode, domainContextNode, foundArg.value), lang);
                    if (value.string.isEmpty()) {
                        throw exception("Empty result for arg " + foundArg.name + " in generator " + generatorElement.name);
                    }
                }
                break;
            case constant:
                if (foundArg == null) return null;
                value = argVal(foundArg.value, languageFromMapping);
                break;
            case position:
                value = argVal(String.valueOf(index), null);
                break;
            default:
                throw new RuntimeException("Not implemented");
        }
        return value;
    }

    public String valueAt(Node node, Node domainNode, String expression) {
        List<Node> nodes = nodeList(node, domainNode, expression);
        if (nodes.isEmpty()) return "";
        String value = nodes.get(0).getNodeValue();
        if (value == null) return "";
        return value.trim();
    }

    public List<Node> nodeList(Node node, Node domainNode, X3ML.Source source) {
        if (source != null) {
            return nodeList(node, domainNode, source.expression);
        }
        else {
            List<Node> list = new ArrayList<Node>(1);
            list.add(node);
            return list;
        }
    }

    public List<Node> nodeList(Node context, Node domainContext, String expression) {
        if (expression == null || expression.length() == 0) {
            List<Node> list = new ArrayList<Node>(1);
            list.add(context);
            return list;
        }
        try {
            Matcher matcher = DOMAIN_PLACEHOLDER.matcher(expression);
            if (matcher.find()) {
                String domainQuery = matcher.group(3);
                XPathExpression dxe = xpath().compile(domainQuery);
                String domainValue = (String) dxe.evaluate(context, XPathConstants.STRING);
                expression = matcher.group(1) + '"' + domainValue + '"' + matcher.group(5);
            }
            XPathExpression xe = xpath().compile(expression);
            NodeList nodeList = (NodeList) xe.evaluate(context, XPathConstants.NODESET);
            int nodesReturned = nodeList.getLength();
            List<Node> list = new ArrayList<Node>(nodesReturned);
            for (int index = 0; index < nodesReturned; index++) {
                list.add(nodeList.item(index));
            }
            return list;
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException("XPath Problem: " + expression, e);
        }
    }

    private static String getLanguageFromSource(Node node) {
        Node walkNode = node;
        while (walkNode != null) {
            NamedNodeMap attributes = walkNode.getAttributes();
            if (attributes != null) {
                Node lang = attributes.getNamedItemNS("http://www.w3.org/XML/1998/namespace", "lang");
                if (lang != null) {
                    return lang.getNodeValue();
                }
            }
            walkNode = walkNode.getParentNode();
        }
        return null;
    }

    private SourceType sourceType(String value, SourceType defaultType) {
        if (value == null) {
            return defaultType;
        }
        else {
            return SourceType.valueOf(value);
        }
    }

    private XPath xpath() {
        XPath path = pathFactory.newXPath();
        path.setNamespaceContext(namespaceContext);
        return path;
    }

}
