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
import eu.delving.x3ml.X3MLEngine;

import java.util.ArrayList;
import java.util.List;

import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.Additional;
import static eu.delving.x3ml.engine.X3ML.GeneratedValue;
import static eu.delving.x3ml.engine.X3ML.GeneratorElement;
import static eu.delving.x3ml.engine.X3ML.TypeElement;

/**
 * The entity resolver creates the related model elements by calling generator functions.
 * <p/>
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
    public List<Resource> resources;
    public Literal literal;
    private boolean failed;

    EntityResolver(ModelOutput modelOutput, X3ML.EntityElement entityElement, GeneratorContext generatorContext) {
        this.modelOutput = modelOutput;
        this.entityElement = entityElement;
        this.generatorContext = generatorContext;
    }

    boolean resolve() {
        if (entityElement == null) {
            throw exception("Missing entity");
        }
        if (failed) return false;
        if (resources == null) {
            StringBuilder unique = new StringBuilder();
            for (TypeElement typeElement : entityElement.typeElements) {
                unique.append('-').append(typeElement.tag);
            }
            GeneratedValue generatedValue = entityElement.getInstance(generatorContext, unique.toString());
            if (generatedValue == null) {
                failed = true;
                return false;
            }
            switch (generatedValue.type) {
                case URI:
                    if (resources == null) {
                        resources = new ArrayList<Resource>();
                        for (TypeElement typeElement : entityElement.typeElements) {
                            resources.add(modelOutput.createTypedResource(generatedValue.text, typeElement));
                        }
                    }
                    labelNodes = createLabelNodes(entityElement.labelGenerators);
                    additionalNodes = createAdditionalNodes(entityElement.additionals);
                    break;
                case LITERAL:
                    literal = modelOutput.createLiteral(generatedValue.text, generatedValue.language);
                    break;
                case TYPED_LITERAL:
                    if (entityElement.typeElements.size() != 1) {
                        throw new X3MLEngine.X3MLException("Expected one type in\n" + entityElement);
                    }
                    TypeElement typeElement = entityElement.typeElements.get(0);
                    literal = modelOutput.createTypedLiteral(generatedValue.text, typeElement);
                    break;
                default:
                    throw exception("Value type " + generatedValue.type);
            }
        }
        return hasResources() || hasLiteral();
    }

    boolean hasResources() {
        return resources != null && !resources.isEmpty();
    }

    boolean hasLiteral() {
        return literal != null;
    }

    void link() {
        if (resources == null) {
            System.out.println("No resources!");
            return;
        }
        for (Resource resource : resources) {
            if (labelNodes != null) {
                for (LabelNode labelNode : labelNodes) {
                    labelNode.linkFrom(resource);
                }
            }
            if (additionalNodes != null) {
                for (AdditionalNode additionalNode : additionalNodes) {
//                    System.out.println("Additional link " + additionalNode.additionalEntityResolver.resources + " from " + resource + additionalNode.additionalEntityResolver.entityElement);
                    additionalNode.linkFrom(resource);
                }
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
            return property != null && additionalEntityResolver.resolve();
        }

        public void linkFrom(Resource fromResource) {
            additionalEntityResolver.link();
            if (additionalEntityResolver.hasResources()) {
                for (Resource resource : additionalEntityResolver.resources) {
                    fromResource.addProperty(property, resource);
                }
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
            GeneratedValue generatedValue = generatorContext.getInstance(generator, null, "-" + generator.name); //todo: are you sure?
            if (generatedValue == null) return false;
            switch (generatedValue.type) {
                case URI:
                    throw exception("Label node must produce a literal");
                case LITERAL:
                    literal = modelOutput.createLiteral(generatedValue.text, generatedValue.language);
                    return true;
            }
            return false;
        }

        public void linkFrom(Resource fromResource) {
            fromResource.addLiteral(property, literal);
        }
    }

}
