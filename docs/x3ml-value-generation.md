# Value Generation
---

When transforming source XML records into RDF triples, the output needs to have values generated for the things to which it refers.  Often the information needed to generate the values is contained in the hierarchy of the source XML, but it can also make use of constants.

To make this a separate process in the workflow, there are calls made from within the **[X3ML](x3ml-schema-mapping.md)** to value generators which defined separately.  The calls look something like this:

	<instance_generator name="..." language="..">
	    <arg name="..." type="...">...</arg>
	    ...
	</instance_generator>
	
	<label_generator name="..." language="..">
	    <arg name="..." type="">...</arg>
	    ...
	</label_generator>

These are to be located within the *entity* tag, where there can be only one *instance_generator* and any number of *label_generator* calls (multiple labels).

The only contract that X3ML has with respect to the value generators is that they have a name, arguments, and optionally a language.

The argument type allows for choosing between *xpath* and *constant* and there is a special argument type called *position* which gives the value generator access to the index position of the source node within its context.

The *language* attribute is used if it is present, and if its value is present but empty the implication is that the label or instance will be generated with no language determination (number literals, for example)


## Templates

When URIs are to be generated on the basis of source record content, it is wise to leverage existing standards and re-use the associated implementations.

For template-based URI generation there is [RFC 6570](http://tools.ietf.org/html/rfc6570) and we have an implementation and have experimented with setting up a generator function on the basis of a configuration file like this:

	<generator_policy>
	    <namespaces>
	        <namespace prefix="pre" uri="http://somenamespace/#"/>
	    </namespaces>
	    <generator name="PhysicalObject" prefix="pre">
	        <pattern>/{nameOfMuseum,entry}</pattern>
	    </generator>
	    <generator name="PlaceName" prefix="pre">
	        <pattern>/{entry}</pattern>
	    </generator>
	</generator_policy>

The policy begins with a description of all the namespaces which the templates will use, so that they have the associated base URL when they use the *prefix* attribute.  When there is no *prefix*, a simple substitution is executed, and the result need not be a proper URI.
	
The *pattern* element here contains a URI template according to the RFC, and the substitution variables found within the braces are found in the call to the generator from within the X3ML.

The parameters are fetched from the source like this:

	<instance_generator name="PhysicalObject">
		<arg name="nameOfMuseum" type="constant">Museum Name</arg>
		<arg name="entry">identifier/text()</arg>
	</instance_generator>
	
The *nameOfMuseum* argument type takes the contents of the *arg* element as-is into the template, which is of course URL encoded.  The *entry* argument is of type xpath so it uses the contents of *arg* as an XPath expression.

## Default Generators

A few default generators are provided in order to create some basic forms of URIs.

* **UUID** - use the operating system's UUID function to create a string as URI, no arguments
* **Literal** - use XPath to fetch content for a literal value, single argument
* **Constant** - take the content verbatim for a literal value, single argument

---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;