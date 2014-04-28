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
import static eu.delving.x3ml.engine.X3ML.*;

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

    public String evaluate(String expression) {
        return context.input().valueAt(node, expression);
    }

    public Instance getInstance(final GeneratorElement generator) {
        if (generator == null) {
            throw exception("Value generator missing");
        }
        Instance instance = context.policy().generate(generator.name, generator.language, new ArgValues() {
            @Override
            public ArgValue getArgValue(String name, SourceType sourceType) {
                return context.input().evaluateArgument(node, index, generator, name, sourceType);
            }
        });
        if (instance == null) {
            throw exception("Empty value produced");
        }
        return instance;
    }

    public boolean conditionFails(Condition condition, GeneratorContext context) {
        return condition != null && condition.failure(context);
    }

}
