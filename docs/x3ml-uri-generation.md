# URI Generation
---

When transforming source XML records into RDF triples, the nested structure of the XML contains information that must be translated into URIs for the purpose of making subjects and objects of RDF statements.

Since it is possible to generate URIs which are not correct according to various specifications, the URI generating mechanism must take into account that the results will be validated and that failures can result.  Not only will the character string be evaluated, but in some cases there is also a need to check for uniqueness within a dataset.

## Templates

When URIs are to be generated on the basis of source record content, it is wise to leverage existing standards and re-use the associated implementations.

For template-based URI generation there is [RFC 6570](http://tools.ietf.org/html/rfc6570) and we have an implementation and have experimented with setting up a URI function on the basis of a configuration file like this:

	<uri-policy>
	    <template name="PhysicalObject">
	        <pattern>http://purl.org/NET/cidoc-crm/core#{className}/{nameOfMuseum,entry}</pattern>
	    </template>
	    <template name="Type">
	        <pattern>http://purl.org/NET/cidoc-cr/core#{className}/{entry}</pattern>
	    </template>
	</uri-policy>

The *pattern* element here contains a URI template according to the RFC, and the parameters are fetched from the source like this:

	<uri_function name="PhysicalObject">
		<arg name="nameOfMuseum">
			<xpath>museum/text()</xpath>
		</arg>
		<arg name="entry">
			<xpath>identifier/text()</xpath>
		</arg>
	</uri_function>


## Simple UUID & Label

(needs clarification)

---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;