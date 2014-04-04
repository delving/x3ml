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
        List<X3ML.ValueEntry> values = entityElement.getValues(generatorContext);
        if (values.isEmpty()) return false;
        for (X3ML.ValueEntry valueEntry : values) { // todo: this will fail for multiple value entries
            switch (valueEntry.value.valueType) {
                case URI:
                    resource = modelOutput.createTypedResource(valueEntry.value.text, valueEntry.typeElement);
                    labelNodes = createLabelNodes(entityElement.labelGenerators);
                    additionalNodes = createAdditionalNodes(entityElement.additionals);
                    if (!additionalNodes.isEmpty()) {
                        System.out.println("additionals present" + entityElement);
                    }
                    break;
                case LITERAL:
                    literal = modelOutput.createLiteral(valueEntry.value.text, generatorContext.getLanguage());
                    break;
                default:
                    throw exception("Value type "+ valueEntry.value.valueType);
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

    private List<AdditionalNode> createAdditionalNodes(List<X3ML.Additional> additionalList) {
        List<AdditionalNode> additionalNodes = new ArrayList<AdditionalNode>();
        if (additionalList != null) {
            for (X3ML.Additional additional : additionalList) {
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
        public final X3ML.Additional additional;
        public final GeneratorContext generatorContext;
        public Property property;
        public EntityResolver additionalEntityResolver;

        private AdditionalNode(ModelOutput modelOutput, X3ML.Additional additional, GeneratorContext generatorContext) {
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

    private List<LabelNode> createLabelNodes(List<X3ML.GeneratorElement> generatorList) {
        List<LabelNode> labelNodes = new ArrayList<LabelNode>();
        if (generatorList != null) {
            for (X3ML.GeneratorElement generator : generatorList) {
                LabelNode labelNode = new LabelNode(generator);
                if (labelNode.resolve()) {
                    labelNodes.add(labelNode);
                }
            }
        }
        return labelNodes;
    }

    private class LabelNode {
        public final X3ML.GeneratorElement generator;
        public Property property;
        public Literal literal;

        private LabelNode(X3ML.GeneratorElement generator) {
            this.generator = generator;
        }

        public boolean resolve() {
            property = modelOutput.createProperty(new X3ML.TypeElement("rdfs:label", "http://www.w3.org/2000/01/rdf-schema#"));
            X3ML.Value value = generatorContext.generateValue(generator, null);
            if (value == null) return false;
            switch (value.valueType) {
                case URI:
                    throw exception("Label node must produce a literal");
                case LITERAL:
                    literal = modelOutput.createLiteral(value.text, generatorContext.getLanguage());
                    return true;
            }
            return false;
        }

        public void linkFrom(Resource fromResource) {
            fromResource.addLiteral(property, literal);
        }
    }

}
