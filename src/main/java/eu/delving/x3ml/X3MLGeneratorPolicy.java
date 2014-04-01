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
package eu.delving.x3ml;

import com.damnhandy.uri.template.MalformedUriTemplateException;
import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.VariableExpansionException;
import eu.delving.x3ml.engine.Generator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.*;
import static eu.delving.x3ml.engine.X3ML.ArgType.*;
import static eu.delving.x3ml.engine.X3ML.Helper.*;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLGeneratorPolicy implements Generator {
    private static final Pattern BRACES = Pattern.compile("\\{[?;+#]?([^}]+)\\}");
    private Map<String, GeneratorSpec> generatorMap = new TreeMap<String, GeneratorSpec>();
    private Map<String, String> namespaceMap = new TreeMap<String, String>();
    private char uuidLetter = 'A';

    public static X3MLGeneratorPolicy load(InputStream inputStream) {
        return new X3MLGeneratorPolicy(inputStream);
    }

    private X3MLGeneratorPolicy(InputStream inputStream) {
        if (inputStream != null) {
            GeneratorPolicy policy = (GeneratorPolicy) generatorStream().fromXML(inputStream);
            for (MappingNamespace namespace : policy.namespaces) {
                namespaceMap.put(namespace.prefix, namespace.uri);
            }
            for (GeneratorSpec generator : policy.generators) {
                generatorMap.put(generator.name, generator);
            }
        }
    }

    @Override
    public Value generateValue(String name, ArgValues args) {
        if (name == null) {
            throw exception("Value function name missing");
        }
        if ("UUID".equals(name)) {
            return uriValue(createUUID());
        }
        if ("Literal".equals(name)) {
            ArgValue literalXPath = args.getArgValue(null, XPATH);
            if (literalXPath == null) {
                throw exception("Argument failure: need one argument");
            }
            if (literalXPath.string == null || literalXPath.string.isEmpty()) {
                throw exception("Argument failure: empty argument");
            }
            return literalValue(literalXPath.string);
        }
        if ("Constant".equals(name)) {
            ArgValue constant = args.getArgValue(null, CONSTANT);
            if (constant == null) {
                throw exception("Argument failure: need one argument");
            }
            return literalValue(constant.string);
        }
        GeneratorSpec generator = generatorMap.get(name);
        if (generator == null) throw exception("No generator for " + name);
        String namespaceUri = generator.prefix == null ? null : namespaceMap.get(generator.prefix);
        if (namespaceUri != null) { // use URI template
            return generateFromURITemplate(generator, namespaceUri, args);
        }
        else { // use simple substitution
            return generateFromSimpleTemplate(generator.pattern);
        }
    }

    private Value generateFromURITemplate(GeneratorSpec generator, String namespaceUri, ArgValues argValues) {
        try {
            UriTemplate uriTemplate = UriTemplate.fromTemplate(stripTypes(generator.pattern));
            for (TypedArgument argument : getTypedVariables(generator.pattern)) {
                ArgValue argValue = argValues.getArgValue(argument.name, argument.argType);
                if (argValue == null || argValue.string == null) {
                    throw exception(String.format(
                            "Argument failure in generator %s: %s",
                            generator, argument
                    ));
                }
                uriTemplate.set(argument.name, argValue.string);
            }
            return uriValue(namespaceUri + uriTemplate.expand());
        }
        catch (MalformedUriTemplateException e) {
            throw exception("Malformed", e);
        }
        catch (VariableExpansionException e) {
            throw exception("Variable", e);
        }
    }

    private Value generateFromSimpleTemplate(String pattern) {
        throw new RuntimeException("Not implemented");
    }

    // == the rest is for the XML form

    private String createUUID() {
        return "uuid:" + (uuidLetter++);
    }

    private static class TypedArgument {
        public final ArgType argType;
        public final String name;

        private TypedArgument(ArgType argType, String name) {
            this.argType = argType;
            this.name = name;
        }

        public String toString() {
            return argType + ":" + name;
        }
    }

    private static List<TypedArgument> getTypedVariables(String pattern) {
        Matcher braces = BRACES.matcher(pattern);
        List<TypedArgument> typedArguments = new ArrayList<TypedArgument>();
        while (braces.find()) {
            for (String argString : braces.group(1).split(",")) {
                ArgType argType = CONSTANT;
                String argName = argString;
                int colon = argString.indexOf(':');
                if (colon > 0) {
                    String typeString = argString.substring(0, colon).toUpperCase();
                    argType = valueOf(typeString);
                    argName = argString.substring(colon + 1);
                }
                typedArguments.add(new TypedArgument(argType, argName));
            }
        }
        return typedArguments;
    }

    private static String stripTypes(String pattern) {
        return pattern
                .replaceAll("xpath:", "")
                .replaceAll("qname:", "")
                .replaceAll("constant:", "");
    }

}
