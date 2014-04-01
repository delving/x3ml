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

import com.hp.hpl.jena.rdf.model.Resource;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import static eu.delving.x3ml.X3MLEngine.exception;

/**
 * This abstract class is above Domain, Path, and Range and carries most of their
 * contextual information.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public abstract class GeneratorContext {
    public final Root.Context context;
    public final GeneratorContext parent;
    public final Node node;

    public GeneratorContext(Root.Context context, GeneratorContext parent, Node node) {
        this.context = context;
        this.parent = parent;
        this.node = node;
    }

    public Resource get(String variable) {
        if (parent == null) throw exception("Parent context missing");
        return parent.get(variable);
    }

    public void put(String variable, Resource resource) {
        if (parent == null) throw exception("Parent context missing");
        parent.put(variable, resource);
    }

    public String evaluate(String expression) {
        return context.input().valueAt(node, expression);
    }

    public X3ML.Value generateValue(final X3ML.GeneratorElement generator, final X3ML.TypeElement typeElement) {
        if (generator == null) {
            throw exception("Value generator missing");
        }
        X3ML.Value value = context.policy().generateValue(generator.name, new X3ML.ArgValues() {
            @Override
            public X3ML.ArgValue getArgValue(String name, X3ML.ArgType type) {
                return context.input().evaluateArgument(node, generator, name, type, typeElement);
            }
        });
        if (value == null) {
            throw exception("Empty value produced");
        }
        return value;
    }

    public String getLanguage() {
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
        throw exception("Missing language");
    }

    public boolean conditionFails(X3ML.Condition condition, GeneratorContext context) {
        return condition != null && condition.failure(context);
    }

}
