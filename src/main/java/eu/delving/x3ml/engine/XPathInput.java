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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.*;
import java.util.ArrayList;
import java.util.List;

import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.Helper.argQName;
import static eu.delving.x3ml.engine.X3ML.Helper.argVal;

/**
 * The source data is accessed using xpath to fetch nodes from a DOM tree.
 *
 * Here we have tools for evaluating xpaths in various contexts.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class XPathInput {
    private final XPathFactory pathFactory = net.sf.saxon.xpath.XPathFactoryImpl.newInstance();
    private final NamespaceContext namespaceContext;

    public XPathInput(NamespaceContext namespaceContext) {
        this.namespaceContext = namespaceContext;
    }

    public X3ML.ArgValue evaluateArgument(Node contextNode, X3ML.GeneratorElement function, String argName, X3ML.ArgType type, X3ML.TypeElement typeElement) {
        X3ML.GeneratorArg foundArg = null;
        if (function.args != null) {
            if (function.args.size() == 1 && function.args.get(0).name == null) {
                foundArg = function.args.get(0);
                foundArg.name = argName;
            }
            else {
                for (X3ML.GeneratorArg arg : function.args) {
                    if (arg.name.equals(argName)) {
                        foundArg = arg;
                    }
                }
            }
        }
        X3ML.ArgValue value;
        switch (type) {
            case XPATH:
                if (foundArg == null) {
                    return null;
                }
                value = argVal(valueAt(contextNode, foundArg.value));
                if (value.string.isEmpty()) {
                    throw exception("Empty result");
                }
                break;
            case QNAME:
                value = argQName(typeElement, argName);
                break;
            case CONSTANT:
                if (foundArg == null) {
                    return null;
                }
                value = argVal(foundArg.value);
                break;
            default:
                throw new RuntimeException("Not implemented");
        }
        return value;
    }

    public String valueAt(Node node, String expression) {
        List<Node> nodes = nodeList(node, expression);
        if (nodes.isEmpty()) return "";
        String value = nodes.get(0).getNodeValue();
        if (value == null) return "";
        return value.trim();
    }

    public List<Node> nodeList(Node node, X3ML.Source source) {
        if (source != null) {
            return nodeList(node, source.expression);
        }
        else {
            List<Node> list = new ArrayList<Node>(1);
            list.add(node);
            return list;
        }
    }

    public List<Node> nodeList(Node context, String expression) {
        if (expression == null || expression.length() == 0) {
            List<Node> list = new ArrayList<Node>(1);
            list.add(context);
            return list;
        }
        try {
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

    private XPath xpath() {
        XPath path = pathFactory.newXPath();
        path.setNamespaceContext(namespaceContext);
        return path;
    }

}
