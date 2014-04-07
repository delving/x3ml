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

/**
 * The range entity handled here.  Resolution delegated.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class Range extends GeneratorContext {
    public final Path path;
    public final X3ML.RangeElement range;
    public EntityResolver rangeResolver;

    public Range(Root.Context context, Path path, Node node, X3ML.RangeElement range) {
        super(context, path, node);
        this.path = path;
        this.range = range;
    }

    public boolean resolve() {
        if (conditionFails(range.target_node.condition, this)) return false;
        rangeResolver = new EntityResolver(context.output(), range.target_node.entityElement, this);
        return rangeResolver.resolve();
    }

    public void link() {
        path.link();
        if (rangeResolver.hasResources()) {
            rangeResolver.link();
            for (Resource lastResource : path.lastResources) {
                for (Resource resolvedResource : rangeResolver.resources) {
                    lastResource.addProperty(path.lastProperty, resolvedResource);
                }
            }
        }
        else if (rangeResolver.hasLiteral()) {
            for (Resource lastResource : path.lastResources) {
                lastResource.addLiteral(path.lastProperty, rangeResolver.literal);
            }
        }
    }
}
