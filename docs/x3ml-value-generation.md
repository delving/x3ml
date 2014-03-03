# Value Generation
---

When transforming source XML records into RDF triples, the nested structure of the XML contains information that must be translated into URIs for the purpose of making subjects and objects of RDF statements.  Also labels must be created from pieces of information in the source.

## Generator Functions

To make this a separate process in the workflow, there are calls made from within the **[X3ML](x3ml-schema-mapping.md)** to value generators, defined separately.

	<value_generator name="...">
	    <arg name="...">...</arg>
	    <arg/>
	    ...
	</value_generator>

The only contract that X3ML has with respect to the value generators is that they have names and arguments.  The types of arguments are determined by the generators themselves, and validity must be checked.

XPATH arguments fetch data values from the source XML DOM, and QNAME arguments have access to the *qname* associated with the surrounding property or entity;

Since it is possible to generate URIs which are not correct according to various specifications, the URI generating mechanism must take into account that the results will be validated and that failures can result.  Not only will the character string be evaluated, but in some cases there is also a need to check for uniqueness within a dataset.

## Simple UUID & Label

A simple strategy has been used as a starting point which depends only on UUIDs and labels.

There are two value functions available to the X3ML Engine.

* **UUID** - generate a UUID
* **UUID_Label** generate a UUID, and add a label using arguments:
	* **labelQName** - qualified name of the label element
	* **labelXPath** - contents of the label fetched from the source

Currently this is only implemented in a test scenario.

## Templates

When URIs are to be generated on the basis of source record content, it is wise to leverage existing standards and re-use the associated implementations.

For template-based URI generation there is [RFC 6570](http://tools.ietf.org/html/rfc6570) and we have an implementation and have experimented with setting up a URI function on the basis of a configuration file like this:

	<value-policy>
	    <generator name="PhysicalObject">
	        <pattern>http://purl.org/NET/cidoc-crm/core#{className}/{nameOfMuseum,entry}</pattern>
	    </generator>
	    <generator name="Type">
	        <pattern>http://purl.org/NET/cidoc-cr/core#{className}/{entry}</pattern>
	    </generator>
	</value-policy>
	
* **NOTE: this xml definition will probably be generalized for value generators**

The *pattern* element here contains a URI template according to the RFC, and the parameters are fetched from the source like this:

	<value_generator name="PhysicalObject">
		<arg name="nameOfMuseum">museum/text()</arg>
		<arg name="entry">identifier/text()</arg>
	</value_generator>


---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;