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

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.PathElement;
import static eu.delving.x3ml.engine.X3ML.Relationship;

/**
 * The path relationship handled here.  Intermediate nodes possible.
 * Expecting always one more path than entity, and they are interlaced.
 * Marshalling handled specially.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class Path extends GeneratorContext {
    public final Domain domain;
    public final PathElement path;
    public Relationship relationship;
    public Property property;
    public List<IntermediateNode> intermediateNodes;
    public List<Resource> lastResources;
    public Property lastProperty;

    public Path(Root.Context context, Domain domain, Node node, int index, PathElement path) { // todo: make index last arg
        super(context, domain, node, index);
        this.domain = domain;
        this.path = path;
    }

    public boolean resolve() {
        X3ML.TargetRelation relation = path.target_relation;
        if (conditionFails(relation.condition, this)) return false;
        if (relation.properties == null || relation.properties.isEmpty()) {
            throw exception("Target relation must have at least one property");
        }
        if (relation.entities != null) {
            if (relation.entities.size() + 1 != relation.properties.size()) {
                throw exception("Target relation must have one more property than entity");
            }
        }
        else if (relation.properties.size() != 1) {
            throw exception("Target relation must just one property if it has no entities");
        }
        relationship = relation.properties.get(0);
        property = context.output().createProperty(relationship);
        intermediateNodes = createIntermediateNodes(relation.entities, relation.properties, this);
        return true;
    }

    public void link() {
        domain.link();
        if (!domain.entityResolver.hasResources()) {
            throw exception("Domain node has no resource");
        }
        lastResources = domain.entityResolver.resources;
        lastProperty = property;
        for (IntermediateNode intermediateNode : intermediateNodes) {
            intermediateNode.entityResolver.link();
            if (!intermediateNode.entityResolver.hasResources()) {
                throw exception("Intermediate node has no resources");
            }
            for (Resource lastResource : lastResources) {
                for (Resource resolvedResource : intermediateNode.entityResolver.resources) {
                    lastResource.addProperty(lastProperty, resolvedResource);
                }
            }
            lastResources = intermediateNode.entityResolver.resources;
            lastProperty = intermediateNode.property;
        }
    }

    public List<Range> createRangeContexts(X3ML.RangeElement range) {
        if (range.source_node == null) throw exception("Range source absent: " + range);
        String pathExtension = getPathExtension(range);
        List<Node> rangeNodes = context.input().nodeList(node, pathExtension);
        List<Range> ranges = new ArrayList<Range>();
        int index = 1;
        for (Node rangeNode : rangeNodes) {
            Range rangeContext = new Range(context, this, rangeNode, index++, range);
            if (rangeContext.resolve()) {
                ranges.add(rangeContext);
            }
        }
        return ranges;
    }

    private String getPathExtension(X3ML.RangeElement range) {
        String rangeSource = range.source_node.expression;
        String pathSource = path.source_relation.expression;
        if (pathSource.length() > rangeSource.length()) {
            throw exception(String.format(
                    "Path source [%s] longer than range source [%s]",
                    pathSource, rangeSource
            ));
        }
        String base = rangeSource.substring(0, pathSource.length());
        String pathExtension = rangeSource.substring(base.length());
        if (pathExtension.length() > 0) {
            if (pathExtension.charAt(0) == '/') {
                pathExtension = pathExtension.substring(1);
            }
        }
        if (!base.equals(pathSource)) {
            throw exception(String.format(
                    "Path and Range source expressions are not compatible: %s %s",
                    pathSource, rangeSource
            ));
        }
        return pathExtension;
    }

    private List<IntermediateNode> createIntermediateNodes(List<X3ML.EntityElement> entityList, List<Relationship> propertyList, GeneratorContext generatorContext) {
        List<IntermediateNode> intermediateNodes = new ArrayList<IntermediateNode>();
        if (entityList != null) {
            Iterator<Relationship> walkProperty = propertyList.iterator();
            walkProperty.next(); // ignore
            for (X3ML.EntityElement entityElement : entityList) {
                IntermediateNode intermediateNode = new IntermediateNode(entityElement, walkProperty.next(), generatorContext);
                if (intermediateNode.resolve()) {
                    intermediateNodes.add(intermediateNode);
                }
            }
        }
        return intermediateNodes;
    }

    private class IntermediateNode {
        public final X3ML.EntityElement entityElement;
        public final Relationship relationship;
        public final GeneratorContext generatorContext;
        public EntityResolver entityResolver;
        public Property property;

        private IntermediateNode(X3ML.EntityElement entityElement, Relationship relationship, GeneratorContext generatorContext) {
            this.entityElement = entityElement;
            this.relationship = relationship;
            this.generatorContext = generatorContext;
        }

        public boolean resolve() {
            entityResolver = new EntityResolver(context.output(), entityElement, generatorContext);
            if (!entityResolver.resolve()) return false;
            property = context.output().createProperty(relationship);
            return true;
        }
    }

}
