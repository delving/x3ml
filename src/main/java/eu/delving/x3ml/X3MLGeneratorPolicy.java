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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.delving.x3ml.X3MLEngine.exception;
import static eu.delving.x3ml.engine.X3ML.*;
import static eu.delving.x3ml.engine.X3ML.Helper.*;
import static eu.delving.x3ml.engine.X3ML.SourceType.constant;
import static eu.delving.x3ml.engine.X3ML.SourceType.xpath;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLGeneratorPolicy implements Generator {
    private static final Pattern BRACES = Pattern.compile("\\{[?;+#]?([^}]+)\\}");
    private Map<String, GeneratorSpec> generatorMap = new TreeMap<String, GeneratorSpec>();
    private Map<String, String> namespaceMap = new TreeMap<String, String>();
    private UUIDSource uuidSource;
    private SourceType defaultSourceType;
    private String languageFromMapping;

    public static X3MLGeneratorPolicy load(InputStream inputStream, UUIDSource uuidSource) {
        return new X3MLGeneratorPolicy(inputStream, uuidSource);
    }

    public static UUIDSource createUUIDSource(boolean testUUID) {
        return testUUID ? new TestUUIDSource() : new RealUUIDSource();
    }

    private X3MLGeneratorPolicy(InputStream inputStream, UUIDSource uuidSource) {
        if (inputStream != null) {
            GeneratorPolicy policy = (GeneratorPolicy) generatorStream().fromXML(inputStream);
            for (MappingNamespace namespace : policy.namespaces) {
                namespaceMap.put(namespace.prefix, namespace.uri);
            }
            for (GeneratorSpec generator : policy.generators) {
                if (generatorMap.containsKey(generator.name)) {
                    throw exception("Duplicate generator name: " + generator.name);
                }
                generatorMap.put(generator.name, generator);
            }
        }
        if ((this.uuidSource = uuidSource) == null) throw exception("UUID Source needed");
    }

    @Override
    public void setDefaultArgType(SourceType sourceType) {
        this.defaultSourceType = sourceType;
    }

    @Override
    public void setLanguageFromMapping(String language) {
        if (language != null) {
            this.languageFromMapping = language;
        }
    }

    @Override
    public String getLanguageFromMapping() {
        return languageFromMapping;
    }

    @Override
    public Instance generate(String name, ArgValues argValues) {
        if (name == null) {
            throw exception("Value function name missing");
        }
        if ("UUID".equals(name)) {
            return uriValue(uuidSource.generateUUID());
        }
        if ("Literal".equals(name)) {
            ArgValue value = argValues.getArgValue("text", xpath);
            if (value == null) {
                throw exception("Argument failure: need one argument");
            }
            if (value.string == null || value.string.isEmpty()) {
                throw exception("Argument failure: empty argument");
            }
            return literalValue(value.string, getLanguage(value.language, argValues));
        }
        if ("Constant".equals(name)) {
            ArgValue value = argValues.getArgValue("text", constant);
            if (value == null) {
                throw exception("Argument failure: need one argument");
            }
            return literalValue(value.string, getLanguage(value.language, argValues));
        }
        GeneratorSpec generator = generatorMap.get(name);
        if (generator == null) throw exception("No generator for " + name);
        String namespaceUri = generator.prefix == null ? null : namespaceMap.get(generator.prefix);
        if (namespaceUri != null) { // use URI template
            return fromURITemplate(generator, namespaceUri, argValues);
        }
        else { // use simple substitution
            return fromSimpleTemplate(generator, argValues);
        }
    }

    private Instance fromURITemplate(GeneratorSpec generator, String namespaceUri, ArgValues argValues) {
        try {
            UriTemplate uriTemplate = UriTemplate.fromTemplate(generator.pattern);
            for (String argument : getVariables(generator.pattern)) {
                ArgValue argValue = argValues.getArgValue(argument, defaultSourceType);
                if (argValue == null || argValue.string == null) {
                    throw exception(String.format(
                            "Argument failure in generator %s: %s",
                            generator, argument
                    ));
                }
                uriTemplate.set(argument, argValue.string);
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

    private Instance fromSimpleTemplate(GeneratorSpec generator, ArgValues argValues) {
        String result = generator.pattern;
        String language = null;
        for (String argument : getVariables(generator.pattern)) {
            ArgValue argValue = argValues.getArgValue(argument, defaultSourceType);
            if (argValue == null || argValue.string == null) {
                throw exception(String.format(
                        "Argument failure in simple template %s: %s",
                        generator, argument
                ));
            }
            result = result.replace(String.format("{%s}", argument), argValue.string);
            if (language == null) language = argValue.language;
        }
        language = getLanguage(language, argValues); // perhaps override
        return literalValue(result, language != null ? language : languageFromMapping);
    }

    // == the rest is for the XML form

    private static class TestUUIDSource implements UUIDSource {
        private int count = 0;

        @Override
        public String generateUUID() {
            int highLetter = count / 26;
            int lowLetter = count % 26;
            count++;
            if (highLetter > 0) {
                return String.format("uuid:%c%c",(char)(highLetter + 'A' - 1), (char)(lowLetter + 'A'));
            }
            else {
                return String.format("uuid:%c",(char)(lowLetter + 'A'));
            }
        }
    }

    private static class RealUUIDSource implements X3MLGeneratorPolicy.UUIDSource {
        @Override
        public String generateUUID() {
            return "urn:uuid:" + UUID.randomUUID();
        }
    }

    private static List<String> getVariables(String pattern) {
        Matcher braces = BRACES.matcher(pattern);
        List<String> arguments = new ArrayList<String>();
        while (braces.find()) {
            Collections.addAll(arguments, braces.group(1).split(","));
        }
        return arguments;
    }

    private String getLanguage(String language, ArgValues argValues) {
        ArgValue languageArg = argValues.getArgValue("language", defaultSourceType);
        if (languageArg != null) {
            language = languageArg.string;
            if (language.isEmpty()) language = null; // to strip language
        }
        return language;
    }
}
