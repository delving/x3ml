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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static eu.delving.x3ml.X3MLEngine.exception;

/**
 * The domain entity handled here.  Resolution delegated.
 * Holding variables here.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class Domain extends GeneratorContext {
    public final X3ML.DomainElement domain;
    public EntityResolver entityResolver;
    private Map<String, List<Resource>> variables = new TreeMap<String, List<Resource>>();

    public Domain(Root.Context context, X3ML.DomainElement domain, Node node, int index) {
        super(context, null, node, index);
        this.domain = domain;
    }

    @Override
    public List<Resource> get(String variable) {
        return variables.get(variable);
    }

    @Override
    public void put(String variable, List<Resource> resources) {
        variables.put(variable, resources);
    }

    public boolean resolve() {
        if (conditionFails(domain.target_node.condition, this)) return false;
        entityResolver = new EntityResolver(context.output(), domain.target_node.entityElement, this);
        return entityResolver.resolve();
    }

    public List<Path> createPathContexts(X3ML.PathElement path) {
        if (path.source_relation == null) throw exception("Path source absent");
        List<Path> paths = new ArrayList<Path>();
        int index = 0;
        for (Node pathNode : context.input().nodeList(node, path.source_relation)) {
            Path pathContext = new Path(context, this, pathNode, index++, path);
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
