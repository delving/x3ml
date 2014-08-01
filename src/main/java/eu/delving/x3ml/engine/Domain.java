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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.DomainElement;
import static eu.delving.x3ml.engine.X3ML.GeneratedValue;
import static eu.delving.x3ml.engine.X3ML.PathElement;

/**
 * The domain entity handled here.  Resolution delegated.
 * Holding variables here.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class Domain extends GeneratorContext {
    public final DomainElement domain;
    public EntityResolver entityResolver;
    private Map<String, X3ML.GeneratedValue> variables = new TreeMap<String, X3ML.GeneratedValue>();

    public Domain(Root.Context context, DomainElement domain, Node node, int index) {
        super(context, null, node, index);
        this.domain = domain;
    }

    @Override
    public GeneratedValue get(String variable) {
        return variables.get(variable);
    }

    @Override
    public void put(String variable, GeneratedValue generatedValue) {
        variables.put(variable, generatedValue);
    }

    @Override
    public Node getDomainNode() {
        return node;
    }

    public boolean resolve() {
        if (conditionFails(domain.target_node.condition, this)) return false;
        entityResolver = new EntityResolver(context.output(), domain.target_node.entityElement, this);
        return entityResolver.resolve();
    }

    public List<Path> createPathContexts(PathElement path) {
        if (path.source_relation == null) throw exception("Path source absent");
        List<Path> paths = new ArrayList<Path>();
        int index = 1;
        for (Node pathNode : context.input().nodeList(node, node, path.source_relation)) {
            Path pathContext = new Path(context, this, path, pathNode, index++);
            if (pathContext.resolve()) {
                paths.add(pathContext);
            }
        }
        return paths;
    }

    public void link() {
        entityResolver.link();
    }
}
