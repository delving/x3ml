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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.delving.x3ml.engine.X3ML.GeneratedValue;

/**
 * The root of the mapping is where the domain contexts are created.  They then fabricate
 * path contexts which in turn make range contexts.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class Root {
    private final Element rootNode;
    private final ModelOutput modelOutput;
    private final XPathInput xpathInput;
    private final Context context;
    private final Map<String, GeneratedValue> generated = new HashMap<String, GeneratedValue>();

    public Root(Element rootNode, final Generator generator, NamespaceContext namespaceContext, List<String> prefixes) {
        this.rootNode = rootNode;
        Model model = ModelFactory.createDefaultModel();
        for (String prefix : prefixes) {
            model.setNsPrefix(prefix, namespaceContext.getNamespaceURI(prefix));
        }
        this.modelOutput = new ModelOutput(model, namespaceContext);
        this.xpathInput = new XPathInput(rootNode, namespaceContext, generator.getLanguageFromMapping());
        this.context = new Context() {

            @Override
            public XPathInput input() {
                return xpathInput;
            }

            @Override
            public ModelOutput output() {
                return modelOutput;
            }

            @Override
            public Generator policy() {
                return generator;
            }

            @Override
            public GeneratedValue getGeneratedValue(String xpath) {
                return generated.get(xpath);
            }

            @Override
            public void putGeneratedValue(String xpath, GeneratedValue generatedValue) {
                switch (generatedValue.type) {
                    case URI:
                        generated.put(xpath, generatedValue);
                        break;
                    case LITERAL:
                        break;
                    case TYPED_LITERAL:
                        break;
                }
            }
        };
    }

    public ModelOutput getModelOutput() {
        return modelOutput;
    }

    public List<Domain> createDomainContexts(X3ML.DomainElement domain) {
        List<Node> domainNodes = xpathInput.nodeList(rootNode, domain.source_node);
        List<Domain> domains = new ArrayList<Domain>();
        int index = 1;
        for (Node domainNode : domainNodes) {
            Domain domainContext = new Domain(context, domain, domainNode, index++);
            if (domainContext.resolve()) {
                domains.add(domainContext);
            }
            else {
                System.out.println("Unresolved: "+domainContext);
            }
        }
        return domains;
    }

    public interface Context {
        XPathInput input();
        ModelOutput output();
        Generator policy();
        GeneratedValue getGeneratedValue(String xpath);
        void putGeneratedValue(String xpath, GeneratedValue generatedValue);
    }
}
