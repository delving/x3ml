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
import org.w3c.dom.Node;

import java.util.List;

import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.ArgValue;
import static eu.delving.x3ml.engine.X3ML.Condition;
import static eu.delving.x3ml.engine.X3ML.GeneratedValue;
import static eu.delving.x3ml.engine.X3ML.GeneratorElement;
import static eu.delving.x3ml.engine.X3ML.SourceType;
import static org.joox.JOOX.$;

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
    public final int index;

    protected GeneratorContext(Root.Context context, GeneratorContext parent, Node node, int index) {
        this.context = context;
        this.parent = parent;
        this.node = node;
        this.index = index;
    }

    public List<Resource> get(String variable) {
        if (parent == null) throw exception("Parent context missing");
        return parent.get(variable);
    }

    public void put(String variable, List<Resource> resources) {
        if (parent == null) throw exception("Parent context missing");
        parent.put(variable, resources);
    }

    public Node getDomainNode() {
        if (parent == null) throw exception("Parent context missing");
        return parent.getDomainNode();
    }

    public String evaluate(String expression) {
        return context.input().valueAt(node, getDomainNode(), expression);
    }

    public GeneratedValue getInstance(final GeneratorElement generator, String variable) {
        if (generator == null) {
            throw exception("Value generator missing");
        }
        String nodeName = String.format(
                "%s-%s(%d)",
                $(node).xpath(), generator.name, generator.index
        );
        GeneratedValue generatedValue = context.getGeneratedValue(nodeName);
        if (generatedValue != null) {
            System.out.println(nodeName + " <====== " + generatedValue);
            return generatedValue;
        }
        generatedValue = context.policy().generate(generator.name, new Generator.ArgValues() {
            @Override
            public ArgValue getArgValue(String name, SourceType sourceType) {
                return context.input().evaluateArgument(node, getDomainNode(), index, generator, name, sourceType);
            }
        });
        if (generatedValue == null) {
            throw exception("Empty value produced");
        }
        System.out.println(nodeName +" := " +generatedValue);
        context.putGeneratedValue(nodeName, generatedValue);
        return generatedValue;
    }

    public boolean conditionFails(Condition condition, GeneratorContext context) {
        return condition != null && condition.failure(context);
    }

    public String toString() {
        return $(node).xpath();
    }
}
