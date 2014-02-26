#X3ML Schema Mapping
---
## Introduction

The X3ML language was designed on the basis of work that was done by FORTH around 2006. It was adapted primarily to be more according to the DRY principle (avoiding repetition) and to be more explicit in its contract with the URI Generating process.

The requirement that X3ML mappings be built **collaboratively** and that they must be amenable to sharing via an accumulating **Mapping Memory** also has influenced its design. The languge must be explicit about any knowledge of underlying ontologies so that the engine need not access that knowledge in order to function, and so that the mapping memory can also independently exchange X3ML.

For the time being, X3ML will be restricted to consuming XML records and producing RDF in various serializations.  As a result, XPath will be the source access used.

## Mappings

At first glance, the global structure of X3ML is quite easy to understand.  It consists of some prerequisite information, and then a series of mappings.

	<mappings version="0.1">
	    <notes/>
	    <namespaces/>
	    <mapping>
	        <domain>
	            <notes/>
	            <source/>
	            <entity/>
	        </domain>
	        <link>
	            <condition/>
	            <path>
		            <notes/>
	                <source/>
	                <property/>
	            </path>
	            <range>
		            <notes/>
	                <source/>
	                <entity/>
	            </range>
	        </link>
	        <link/>
	        ...
	    </mapping>
	    <mapping/>
	    ...
	</mappings>

Each *mapping* contains a *domain* and a number of *links*, and links are built of of a *path* and a *range*.

## Conditions

Whether or not the engine will continue to pursue the evaluation of a mapping or a link can depend on certain conditions, either checking the existence of an element or attribute in the source, or checking whether it is equal to a certain value or matches a regular expression.

The elements used will be *exists*, *equals*, *matches*, *and*, *or*, and *not*, at the top level enclosed in a **&lt;condition>&lt;/condition>** element.

Conditions can be situated within *entity* and *property* and their purpose is to describe when the engine should pursue the creation of these output elements or not.  A failing condition in the *domain* entity will abort the production of any of the RDF specified by the entire mapping.  If a *path* property condition or the *range* entity condition fails, the associated link will abort.

Existence can be checked with a condition containing xpath:

	<exists>
		<xpath>...</xpath>
	</exists>

Equality is checked with both an xpath to be evaluated and a literal for comparison:

	<equals>
		<xpath>...</xpath>
		<literal>...</literal>
	</equals>

Patterns can be checked by replacing the literal with a regular expression:

	<matches>
		<xpath>...</xpath>
		<regex>...</regex>
	</matches>

Conditions can also be combined into boolean expressions (here **ANY** is to mean any of the six conditional elements):

	<and>
		<ANY/>
		...
	</and>
	<or>
		<ANY/>
		...
	</or>
	<not>
		<ANY/>
	</and>

A condition in context will be encapsulated in a *condition* element like this:

    <entity>
    	<condition>
    		... boolean tree of conditions ...
    	</condition>
    	... other entity content ...
    </entity>

    <property>
    	<condition>
    		... boolean tree of conditions ...
    	</condition>
    	... other property content ...
    </property>


## Path and Range Extensions

The basic *path* block contains *source* and *property*, but it can also have *internal_node* instances.  Similarly, the *range* block contains *source* and *entity*, but it can also have *additional_node* instances.

	<path>
		<source/>
		<property/>
		<internal_node>
			<entity/>
			<property/>
		</internal_node>
		...
	</path>
	<range>
		<source/>
		<entity/>
		<additional_node>
			<property/>
			<entity/>
		</additional_node>
		...
	</range>

The internal node allows a *path* to result in a chain, rather than only a property:

	property => property-entity-property

The additional node allows the *range* to result in a chain rather than only an entity:

	entity => entity-property-entity

## Entity

The domain and range contain *entity* blocks, which can either generate URIs or represent literals:

	<entity>
		<condition/>
		<literal>...</literal>
	</entity>

	<entity>
		<condition/>
		<uri_function name="...">
		    <arg name="...">
		    	<xpath>...</xpath>
		   	</arg>
		    <arg name="...">
		    	<literal>...</literal>
		    </arg>
		    <arg/>
		    ...
		</uri_function>
	</entity>
	
The arguments of the URI functions are named and optional, since there is logic within the URI Generation to anticipate omissions.  The type of argument is determined by the element encapsulating the actual value.  Initially the two options of either *literal* values or *xpath* expressions which will be evaluated in the current context (see Source below) in order to fetch information from the source record.

## Property

The property describes the nature of the connection between the *domain* and the *range* in the target schema.

	<property>
		<uri>....</uri>
	</property>

The proprty element contains the URI pointing to its definition.

## Source

The source element provides the engine with the information needed to navigate the source record.  First, the source of the *domain* is used as a kind of "anchor" and then the *links* are traversed such that their rules determine what RDF statements can be made about the source.

	<source>
		<xpath>...</xpath>
	</source>

The *source* element is present in domain, path, and range, and the effect is that the XPath which is considered to be local context is extended.  This means that the *source* in the *path* is appended to the one represented in the *domain*, and likewise, that *source* in the *range* is appended further to narrow context if necessary.

## Notes

Tools for managing X3ML will make use of these note elements to record all information arising from the mapping building process which may be useful people accessing the *Mapping Memory* in the future.  

A simple open-ended structure where each *note* has a **type** and content allows for different tools to easily define their own note structures.  We could maintain a definition of what the different known **type** values are, and this could be easily expanded without changing the schema.

	<notes>
		<note type="...">
			....
		</note>
		<note/>
		...
	</notes>

There are *notes* elements defined at the top level where all mappings are bound together, as well as in each *domain* and each *path* and *range*.

The content of a note can be any arbitrary XML, and the X3ML Engine will ignore these blocks, since they are for human consumption.


---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;



















--

