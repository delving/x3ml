# Value Generation
---

When transforming source XML records into RDF triples, the nested structure of the XML contains information that must be gathered to build URIs and labels.

To make this a separate process in the workflow, there are calls made from within the **[X3ML](x3ml-schema-mapping.md)** to value generators, defined separately.

	<value_generator name="...">
	    <arg name="...">...</arg>
	    <arg/>
	    ...
	</value_generator>

The only contract that X3ML has with respect to the value generators is that they have names and arguments.  The types of arguments are determined by the generators themselves, and validity must be checked.

**NOTE: There is still some flux in this definition, because the design is under discussion**

## Templates

When URIs are to be generated on the basis of source record content, it is wise to leverage existing standards and re-use the associated implementations.

For template-based URI generation there is [RFC 6570](http://tools.ietf.org/html/rfc6570) and we have an implementation and have experimented with setting up a URI function on the basis of a configuration file like this:

	<generator_policy>
	    <namespaces>
	        <namespace prefix="pre" uri="http://somenamespace/#"/>
	    </namespaces>
	    <generator name="PhysicalObject" prefix="pre">
	        <pattern>/{constant:nameOfMuseum,xpath:entry}</pattern>
	    </generator>
	    <generator name="PlaceName" prefix="pre">
	        <pattern>/{xpath:entry}</pattern>
	    </generator>
	</generator_policy>

The policy begins with a description of all the namespaces which the templates will use, so that they have the associated base URL when they use the *prefix* attribute.
	
The *pattern* element here contains a URI template according to the RFC, with the modification that each substitution (between braces) is **prefixed** with a type.

The types of arguments can be one of the following:

* **xpath** - interpret an XPath expression in context
* **qname** - interpret the prefix:localName as a qualified name
* **constant** - take the content verbatim

The parameters are fetched from the source like this:

	<value_generator name="PhysicalObject">
		<arg name="nameOfMuseum">Museum Name</arg>
		<arg name="entry">identifier/text()</arg>
	</value_generator>
	
The *nameOfMuseum* argument type takes the contents of the *arg* element as-is into the template, which is of course URL encoded.  The *entry* argument is of type xpath so it uses the contents of *arg* as an XPath expression.

## Default Generators

A few default generators are provided in order to create some basic forms of URIs.

* **UUID** - use the operating system's UUID function to create a string as URI
* **Literal** - use XPath to fetch content for a literal value
* **Constant** - take the content verbatim for a literal value

---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;