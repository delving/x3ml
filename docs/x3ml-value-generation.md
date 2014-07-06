# Value Generation
---

The X3ML Engine takes source XML records and generates RDF triples consisting of subject, predicate, and object.  The subject and the object are "values", generally consisting of Uniform Resource Identifier, but objects can also be labels or literal values.

Value generation is a separate process from the schema mapping of X3ML, so the X3ML Engine delegates this work to a **Generator** by making calls with arguments.  A call is made to generate an **instance** from within X3ML looks like this:

	<instance_generator name="[gen-name]">
	    <arg name="[arg-name]" type="[arg-type]">[arg-value]</arg>
	    ...
	</instance_generator>

To generate a **label** instead, an identical structure is used:

	<label_generator name="[gen-name">
	    <arg name="[arg-name]" type="[arg-type]">[arg-value]</arg>
	    <arg name="language" type="constant">[language-code]</arg>
	    ...
	</label_generator>

For any one entity there can be an *instance_generator* and any number of subsequent *label_generator* blocks.

The argument type allows for choosing between *xpath* and *constant* and there is a special argument type called *position* which gives the value generator access to the index position of the source node within its context.

The *language* argument is used if it is present, and if its value is present but empty the implication is that the label or instance will be generated with no language determination (number literals, for example)

## Value Generator

A value generator is a class which implements the *Generator* interface, which has this as its core method:

	GeneratedValue generate(String name, ArgValues arguments);

The call is made to a generator with the given *name* and with the *ArgValues* available for the generator to access if necessary.

The return value contains *text*, with also a type attribute and an optional language attribute. The *GeneratedType* values can be one of the following three:

* URI
* LITERAL
* TYPED_LITERAL

The full interface for a *Generator* is this:

	public interface Generator {
	
	    interface UUIDSource {
	        String generateUUID();
	    }
	
	    void setDefaultArgType(SourceType sourceType);
	
	    void setLanguageFromMapping(String language);
	
	    void setNamespace(String prefix, String uri);

	    String getLanguageFromMapping();
	
        public interface ArgValues {
	        ArgValue getArgValue(String name, SourceType sourceType);
	    }
	
	    GeneratedValue generate(String name, ArgValues arguments);
	}

There is an abstraction for generating UUIDs, and there is some information that the *Generator* needs to get from the X3ML.

The set methods are called at the moment that the X3ML Engine is executed so that the instance generator is aware of the default argument type, the language, and the namespaces from the X3ML mapping file.

	generator.setDefaultArgType(rootElement.sourceType);
	generator.setLanguageFromMapping(rootElement.language);
	if (rootElement.namespaces != null) {
	    for (MappingNamespace mn : rootElement.namespaces) {
	        generator.setNamespace(mn.prefix, mn.uri);
	    }
	}

Any class implementing this *Generator* interface can perform the function of value generator, but the X3ML library provides a default implementation.

## Default Generators

A few default generators are provided in order to create some basic forms of URIs.

* **UUID** - use the operating system's UUID function to create a string as URI
	
		<instance_generator name="UUID"/>
		
* **Literal** - use XPath to fetch content for a literal value

		<label_generator name="Literal">
		    <arg name="text">text()</arg>
		    <arg name="language"/>
		</label_generator>

* **Constant** - take the content verbatim for a literal value, single argument

		<label_generator name="Constant">
		    <arg>production</arg>
		</label_generator>

## Generator Policy File

The default implementation class of *Generator* is called *X3MLGeneratorPolicy* and it is configured by means of an XML file that looks like this:

	<?xml version="1.0" encoding="UTF-8"?>
	<generator_policy>
	    <generator name="" prefix="">
	        <pattern>...</pattern>
	    </generator>
	    <generator name="">
	        <custom generatorClass="">
	            <set-arg name="" type=""/>
	            <set-arg name=""/>
	        </custom>
	    </generator>
	</generator_policy>

The *generator_policy* is described in this file to be a set of named *generator* functions, which must be identical to the ones used in the X3ML to make the calls.  

### Simple Templates

To build values up in a simple way, the generator policy implementation has a straightforward string substitution mechanism, in the form of a template.  A simple example from the policy file might be this:

    <generator name="TwoPartLabel">
        <pattern>{part1}:{part2}</pattern>
    </generator>
    
Values within braces are the substitution names, and there may be no extra whitespace included.

This generator would then be called from within X3ML with these arguments:

	<label_generator name="TwoPartLabel">
	    <arg name="part1">thing_primary/text()</arg>
	    <arg name="part2">thing_extra/text()</arg>
	    <arg name="language"/>
	</label_generator>
	
The *language* argument here specifies that **NO** language annotation is to be added to the literal produced.  Omitting *language* will cause the literal to be expressed in the default language, and of course the language can be explicitly set with this argument.

    <arg name="language">de</arg>

### URI Templates

When URIs are to be generated on the basis of source record content, it is wise to leverage existing standards and re-use the associated implementations.  For template-based URI generation there is [RFC 6570](http://tools.ietf.org/html/rfc6570), so the *X3MLGeneratorPolicy* uses an existing implementation [library](https://github.com/damnhandy/Handy-URI-Templates).

	namespace prefix from from X3ML:
    <namespace prefix="adw" uri="http://www.oeaw.ac.at/"/>

	from Policy file:
    <generator name="LocalTermURI" prefix="adw">
        <pattern>{hierarchy}/{term}</pattern>
    </generator>

The *pattern* element here contains a URI template according to the RFC, and the substitution variables found within the braces are found in the call to the generator from within the X3ML.

	call from X3ML:
	<instance_generator name="LocalTermURI">
	    <arg name="hierarchy" type="constant">languages</arg>
	    <arg name="term" type="constant">german</arg>
	</instance_generator>

This example has two constant arguments, but the values could also just as well be fetched from the source XML as in the *part1* and *part2* from the label example of the previous section.

### Custom Generators

Whenever the required URIs or labels cannot be generated by the default generators, the simple templates, or the URI templates, it is always possible to insert a special generator in the form of a class implementing the *CustomGenerator* interface.

    public interface CustomGenerator {
        void setArg(String name, String value) throws CustomGeneratorException;
        String getValue() throws CustomGeneratorException;
    }

Arguments are set and then the resulting value is extracted. A custom generator is identified as an entry in the policy file with the following structure:
  
    <generator name="GermanDateTime">
        <custom generatorClass="eu.delving.custom.GermanDate">
            <set-arg name="bound" type="constant"/>
            <set-arg name="text"/>
        </custom>
    </generator>

The *generatorClass* must be the fully qualified name of a Java class available in the classpath and implementing *CustomGenerator*. The arguments which are to be pulled in from the X3ML call to this generator and pushed into the implementing class are identified with *set-arg* entries.  The call from X3ML then would look like this:
    
	<entity>
		<type>xsd:dateTime</type>
		<instance_generator name="GermanDateTime">
			<arg name="bound">Lower</arg>
			<arg name="text">text()</arg>
		</instance_generator>
	</entity>

Note that the *bound* argument in this example has its type determined by the *set-arg* block, so the call in X3ML need not determine type.  Also note that only the second argument is actually fetched from the source data, since the first is just a constant.

---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;