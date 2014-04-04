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

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import java.util.ArrayList;
import java.util.List;

import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.*;

/**
 * The entity resolver creates the related model elements by calling generator functions.
 *
 * Handles label nodes and additional nodes with their properties
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class EntityResolver {
    public final ModelOutput modelOutput;
    public final X3ML.EntityElement entityElement;
    public final GeneratorContext generatorContext;
    public List<LabelNode> labelNodes;
    public List<AdditionalNode> additionalNodes;
    public Resource resource;
    public Literal literal;

    EntityResolver(ModelOutput modelOutput, X3ML.EntityElement entityElement, GeneratorContext generatorContext) {
        this.modelOutput = modelOutput;
        this.entityElement = entityElement;
        this.generatorContext = generatorContext;
    }

    boolean resolve() {
        if (entityElement == null) {
            throw exception("Missing entity");
        }
        if (entityElement.variable == null) {
            return resolveResource();
        }
        resource = generatorContext.get(entityElement.variable);
        if (resource != null) return true;
        if (!resolveResource()) return false;
        generatorContext.put(entityElement.variable, resource);
        return true;
    }

    private boolean resolveResource() {
        List<InstanceEntry> values = entityElement.getInstances(generatorContext);
        if (values.isEmpty()) return false;
        for (InstanceEntry instanceEntry : values) { // todo: this will fail for multiple value entries
            switch (instanceEntry.instance.type) {
                case URI:
                    resource = modelOutput.createTypedResource(instanceEntry.instance.text, instanceEntry.typeElement);
                    labelNodes = createLabelNodes(entityElement.labelGenerators);
                    additionalNodes = createAdditionalNodes(entityElement.additionals);
                    if (!additionalNodes.isEmpty()) {
                        System.out.println("additionals present" + entityElement);
                    }
                    break;
                case LITERAL:
                    literal = modelOutput.createLiteral(instanceEntry.instance.text, generatorContext.getLanguage());
                    break;
                default:
                    throw exception("Value type "+ instanceEntry.instance.type);
            }
        }
        return hasResource() || hasLiteral();
    }

    boolean hasResource() {
        return resource != null;
    }

    boolean hasLiteral() {
        return literal != null;
    }

    void link() {
        if (resource != null && labelNodes != null) {
            for (LabelNode labelNode : labelNodes) {
                labelNode.linkFrom(resource);
            }
            for (AdditionalNode additionalNode : additionalNodes) {
                additionalNode.linkFrom(resource);
            }
        }
    }

    private List<AdditionalNode> createAdditionalNodes(List<Additional> additionalList) {
        List<AdditionalNode> additionalNodes = new ArrayList<AdditionalNode>();
        if (additionalList != null) {
            for (Additional additional : additionalList) {
                AdditionalNode additionalNode = new AdditionalNode(modelOutput, additional, generatorContext);
                if (additionalNode.resolve()) {
                    additionalNodes.add(additionalNode);
                }
            }
        }
        return additionalNodes;
    }

    private static class AdditionalNode {
        public final ModelOutput modelOutput;
        public final Additional additional;
        public final GeneratorContext generatorContext;
        public Property property;
        public EntityResolver additionalEntityResolver;

        private AdditionalNode(ModelOutput modelOutput, Additional additional, GeneratorContext generatorContext) {
            this.modelOutput = modelOutput;
            this.additional = additional;
            this.generatorContext = generatorContext;
        }

        public boolean resolve() {
            property = modelOutput.createProperty(additional.relationship);
            additionalEntityResolver = new EntityResolver(modelOutput, additional.entityElement, generatorContext);
            if (property == null) return false;
            return additionalEntityResolver.resolve();
        }

        public void linkFrom(Resource fromResource) {
            additionalEntityResolver.link();
            if (additionalEntityResolver.hasResource()) {
                fromResource.addProperty(property, additionalEntityResolver.resource);
            }
            else if (additionalEntityResolver.hasLiteral()) {
                fromResource.addLiteral(property, additionalEntityResolver.literal);
            }
            else {
                throw exception("Cannot link without property or literal");
            }
        }
    }

    private List<LabelNode> createLabelNodes(List<GeneratorElement> generatorList) {
        List<LabelNode> labelNodes = new ArrayList<LabelNode>();
        if (generatorList != null) {
            for (GeneratorElement generator : generatorList) {
                LabelNode labelNode = new LabelNode(generator);
                if (labelNode.resolve()) {
                    labelNodes.add(labelNode);
                }
            }
        }
        return labelNodes;
    }

    private class LabelNode {
        public final GeneratorElement generator;
        public Property property;
        public Literal literal;

        private LabelNode(GeneratorElement generator) {
            this.generator = generator;
        }

        public boolean resolve() {
            property = modelOutput.createProperty(new TypeElement("rdfs:label", "http://www.w3.org/2000/01/rdf-schema#"));
            X3ML.Instance instance = generatorContext.getInstance(generator, null);
            if (instance == null) return false;
            switch (instance.type) {
                case URI:
                    throw exception("Label node must produce a literal");
                case LITERAL:
                    literal = modelOutput.createLiteral(instance.text, generatorContext.getLanguage());
                    return true;
            }
            return false;
        }

        public void linkFrom(Resource fromResource) {
            fromResource.addLiteral(property, literal);
        }
    }

}
