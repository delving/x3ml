# X3ML Language

The X3ML language was designed on the basis of work that was done by FORTH around 2006. It was adapted primarily to be more according to the DRY principle (avoiding repetition) and to be more explicit in its contract with the URI Generating process.

For the time being, X3ML will be restricted to consuming XML records and producing RDF in various serializations.  As a result, XPath will be the source access used.

At first glance, the global structure of X3ML is quite easy to understand. It consists of some prerequisite information, and then a series of mappings. Each mapping contains a domain and a number of links, and links are built of a path and a range.

	<x3ml>
	    <namespaces/>
	    <mappings>
	        <mapping>
	            <domain>
	                <source_node/>
	                <target_node/>
	            </domain>
	            <link>
	                <path>
	                    <source_relation/>
	                    <target_relation/>
	                </path>
	                <range>
	                    <source_node/>
	                    <target_node/>
	                </range>
	            </link>
	            ... more links ...
	        </mapping>
	        ... more mappings ...
	    </mappings>
	</x3ml>

The *source_node* element, described in more detail below, provides the information needed to navigate to the source record. The *source_node* of the domain is used as an “anchor” in order to use multiple links which are traversed in order to determine the statements that are made about the source. The *source_node* and *source_relation* elements are also present in path and range, and these sources are evaluated within the context of the *domain/source_node*. The two are typically identical, but they represent a statement about the semantic origin of the resulting relationship and entity. When they are not identical, the *range/source_node* extends the *path/source_relation*.

### Info and Comments

The X3ML format intended to bridge the gap between human author and machine executor, so the format allows for *info* and *comment* blocks throughout the mapping specification. These blocks can contain the information needed for humans to understand what the mapping instructions are to accomplish.

	<x3ml>
		<info>
			... various fields describing the mapping ...
		</info>
	    <namespaces/>
	    <mappings>
	        <mapping>
	            <domain>
					<comments>
						... various notes about the domain ...
					</comments>
	            </domain>
	            <link>
	                <path>
						<comments>
							... various notes about the path ...
						</comments>
	                </path>
	                <range>
						<comments>
							... various notes about the range ...
						</comments>
	                </range>
	            </link>
	        </mapping>
	    </mappings>
		<comments>
			... various notes about the mappings ...
		</comments>
	</x3ml>

Tools for managing X3ML will make use of these note elements to record all information arising from the mapping building process which may be useful people accessing the *Mapping Memory* in the future.  

From the point of view of the Mapping Engine, the content of a note can be any arbitrary XML, and it will ignore these blocks.

## Structure of a Mapping

The domain and range contain target blocks, which can either contain or generate URIs or represent literals to identify the schema elements to which the sources are matched.  The target blocks can also contain criteria upon which the mapping to a target will take place, and they allow for extensions generating intermediate nodes and relations.  The basic structure of an individual mapping with its six components is represented graphically in Figure 1.

---
![Fig 1](images/X3ML-1.png?raw=true =680x480)

**Figure 1**: Basic Structure of an Individual Mapping

---

## Source Node and Relation

The source element provides the engine with the information needed to navigate the source record. The expected content depends on the top-level attribute *source_type*, which will only allow "XPATH" for the time being.

First, the source of the *domain* is used as a kind of "anchor" and then the *links* are traversed such that their rules determine what RDF statements can be made about the source.

	<source_node>[xpath]</source_node>
	<source_relation>[xpath]</source_relation>

The *source* element is also present in path and range, and these sources are evaluated within the context of the domain/source.  The two are typically identical, but they represent a statement about the semantic origin of the resulting relationship and entity.  When they are not identical, the range/source extends the path/source.

## Target Entities and Relations

As we have seen in Figure 1, the *target_node* contains an *entity* block, and this is the information leading to the generation of resource URIs for output graph.

	<entity>
	    <type>[prefix:localName or URI]</type>
	    <instance_generator name="[name]">
	        ... arguments ...
	    </instance_generator>
	    <label_generator name="[name]">
			... [arguments] ...
	    </label_generator>
	    ... more label generators ...
	</entity>

The typed resources and labels resulting from an *entity* block are generated on the basis of constants and data extracted from the source document.  This is described more in detail below in the **Value Generation**.

Relations are simpler because they do not depend on the generation of URIs:

	<target_relation>
	    <relationship>[prefix:localName or URI]</relationship>
	</target_relation>

The relationship URI is specified either with a prefix (defined above in *namespaces*) and local name or a full URI value.

### Variables

Sometimes it is necessary to generate an instance in X3ML only once for a given input record, and re-use it in a number of locations in the mapping.  For example, in the example above for Intermediate Nodes there is a value **PRODUCTION** (a production event) introduced.  It could be that several different mappings in the same X3ML file need to re-use this single production event for attaching several things to it.  In these cases, an entity can be assigned to a variable:

	<entity variable="p1">
	    [generate the value]
	</entity>

Entity blocks with their variables set will only generate the associated values once, and then re-use it whenever the variable name (in this caes *p1*) is used again, in the scope of the whole X3ML file.

### Conditions

The conditional expressions within the *target_node* and *target_relation* can check for existence and equality of values and can be combined into boolean expressions.  Conditions check existence or equality, and between the tags is the XPath expression to evaluate:

    <if>
       <exists>[xpath]</exists>
    </if>
    
    <if><not>
       <if><exists>[xpath]</exists></if>
    </not></if>
    
    <if>
       <equals value="[value-for-comparison]">[xpath]</equals>
    </if>
    
    <if><not>
       <if><equals value="[value-for-comparison]">[xpath]</equals></if>
    </not></if>

An option that will be implemented later when there is a SKOS vocabulary available for querying:

    <if><narrower value="[URI-value]">[xpath]</narrower></if>

Multiple conditions can also be combined into boolean expressions:

    <if>
        <and> <if/> <if/> [more...] </and>
    </if>
    <if>
        <or> <if/> <if/> [more...] </or>
    </if>

## Extensions

Beyond the six-component structure of a mapping in Figure 1, X3ML allows for extending the generated graph for a mapping in each of the three pairs of source-target blocks.

The extensions are placed witin the *entity* tags when there is to be additional nodes containing more information about the source domain or the source range.

### Additional Nodes

When additional properties and entities need to be added to a target entity, the *additional* element can be used.  It contains the entity which will be attached to the target entity, and the relationship describing the link.

	<range>
		<source_node/>
		<target_node>
			<entity>
				<additional>
					<relationship/>
					<entity/>
				</additional>
				... more additionals ...
			</entity>
		</target_node>
	</range>

Note that the target allows multiple additional nodes.



---
![Fig 2](images/X3ML-2.png?raw=true =680x540)

**Figure 2**: Extension of the Source Domain

---



---
![Fig 3](images/X3ML-3.png?raw=true =680x480)

**Figure 3**: Extension of the Source Range

---

### Intermediate Nodes

Sometimes a path in the source schema needs to become more than just a path in the output.  Instead, an intermediate entity must be introduced.

	source:
	record => has_descriptor => term
	
	target:
	man-made-object => was_produced_by => PRODUCTION => used_general_technique => type

This is formulated using the *intermediate* element:

	<path>
		<source_relation/>
		<target_relation>
			<relationship/>
			<entity/>
			<relationship/>
		</target>
	</path>


---
![Fig 4](images/X3ML-4.png?raw=true =680x480)

**Figure 3**

---


# Value Generation

The X3ML Engine takes source XML records and generates RDF triples consisting of subject, predicate, and object.  The subject and the object are "values", generally consisting of Uniform Resource Identifier, but objects can also be labels or literal values.

Value generation is a separate process from the schema mapping of X3ML, so the X3ML Engine delegates this work to a **Generator** by making calls with arguments.  A call is made to generate an **instance** from within X3ML looks like this:

	<instance_generator name="[gen-name]">
	    <arg name="[arg-name]" type="[arg-type]">[arg-value]</arg>
	    ...
	</instance_generator>

To generate a **label** instead, an identical structure is used:

	<label_generator name="[gen-name]">
	    <arg name="[arg-name]" type="[arg-type]">[arg-value]</arg>
	    <arg name="language" type="constant">[language-code]</arg>
	    ...
	</label_generator>

For any one entity there can be an *instance_generator* and any number of subsequent *label_generator* blocks.

The argument type allows for choosing between *xpath* and *constant* and there is a special argument type called *position* which gives the value generator access to the index position of the source node within its context.

The *language* argument is used if it is present, and if its value is present but empty the implication is that the label or instance will be generated with no language determination (number literals, for example)

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

The default implementation class of *Generator* (see below) is called *X3MLGeneratorPolicy* and it is configured by means of an XML file that looks like this:

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

Note that the namespaces are defined in the X3ML file, so in the Generator Policy File the prefixes can be used for URI templates.

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

## Programmatic Value Generator

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


---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;



















--

