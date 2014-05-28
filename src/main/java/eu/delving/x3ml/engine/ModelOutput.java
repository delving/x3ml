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
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import javax.xml.namespace.NamespaceContext;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static eu.delving.x3ml.X3MLEngine.Output;
import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.TypeElement;

/**
 * The output sent to a Jena graph model.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class ModelOutput implements Output {
    private final Model model;
    private final NamespaceContext namespaceContext;

    public ModelOutput(Model model, NamespaceContext namespaceContext) {
        this.model = model;
        this.namespaceContext = namespaceContext;
    }

    public Resource createTypedResource(String uriString, TypeElement typeElement) {
        if (typeElement == null) {
            throw exception("Missing qualified name");
        }
        String typeUri = namespaceContext.getNamespaceURI(typeElement.getPrefix());
        return model.createResource(uriString, model.createResource(typeUri + typeElement.getLocalName()));
    }

    public Property createProperty(TypeElement typeElement) {
        if (typeElement == null) {
            throw exception("Missing qualified name");
        }
        String propertyNamespace = namespaceContext.getNamespaceURI(typeElement.getPrefix());
        return model.createProperty(propertyNamespace, typeElement.getLocalName());
    }

    public Property createProperty(X3ML.Relationship relationship) {
        if (relationship == null) {
            throw exception("Missing qualified name");
        }
        String propertyNamespace = namespaceContext.getNamespaceURI(relationship.getPrefix());
        return model.createProperty(propertyNamespace, relationship.getLocalName());
    }

    public Literal createLiteral(String value, String language) {
        return model.createLiteral(value, language);
    }

    public Literal createTypedLiteral(String value, TypeElement typeElement) {
        String literalNamespace = namespaceContext.getNamespaceURI(typeElement.getPrefix());
        String typeUri = literalNamespace + typeElement.getLocalName();
        return model.createTypedLiteral(value, typeUri);
    }

    public void writeXML(PrintStream out) {
        model.write(out, "RDF/XML-ABBREV");
    }

    public void writeNTRIPLE(PrintStream out) {
        model.write(out, "N-TRIPLE");
    }

    public void writeTURTLE(PrintStream out) {
        model.write(out, "TURTLE");
    }

    public void write(PrintStream out, String format) {
        if ("application/n-triples".equalsIgnoreCase(format)) {
            writeNTRIPLE(out);
        }
        else if ("text/turtle".equalsIgnoreCase(format)) {
            writeTURTLE(out);
        }
        else if ("application/rdf+xml".equalsIgnoreCase(format)) {
            writeXML(out);
        }
        else {
            writeXML(out);
        }
    }

    public String[] toStringArray() {
        return toString().split("\n");
    }

    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeNTRIPLE(new PrintStream(baos));
        return new String(baos.toByteArray());
    }
}
