#X3ML Schema Mapping
---
## Introduction

The X3ML language was designed on the basis of work that was done by FORTH around 2006. It was adapted primarily to be more according to the DRY principle (avoiding repetition) and to be more explicit in its contract with the URI Generating process.

The requirement that X3ML mappings be built **collaboratively** and that they must be amenable to sharing via an accumulating **Mapping Memory** also has influenced its design. The languge must be explicit about any knowledge of underlying ontologies so that the engine need not access that knowledge in order to function, and so that the mapping memory can also independently exchange X3ML.

For the time being, X3ML will be restricted to consuming XML records and producing RDF in various serializations.  As a result, XPath will be the source access used.

## Mappings

At first glance, the global structure of X3ML is quite easy to understand.  It consists of some prerequisite information, and then a series of mappings.

	<mappings version="0.1">
	    <namespaces/>
	    <mapping>
	        <domain>
	            <source/>
	            <target/>
	        </domain>
	        <link>
	            <path>
	                <source/>
	                <target/>
	            </path>
	            <range>
	                <source/>
	                <target/>
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

Whether or not the engine will continue to pursue the evaluation of a particular mapping or a link can depend on certain conditions are met.  This is intended to allow mappings to be "activated" or not according to rules, and the rules are generally based on terminology usage in the source.

The elements used will be *exists*, *equals*, *narrower_than*, *and*, *or*, and *not*, at the top level enclosed in a **&lt;condition>&lt;/condition>** element.

Conditions can be situated in six different places within the mapping definition.

	<mappings version="0.1">
	    <namespaces/>
	    <mapping>
	        <domain>
	            <source>
	            	<condition/>
	            </source>
	            <target>
	            	<condition/>
	            </target>
	        </domain>
	        <link>
	            <path>
	                <source>
						<condition/>
	                </source>
	                <target>
						<condition/>
	                </target>
	            </path>
	            <range>
	                <source>
						<condition/>
	                </source>
	                <target>
						<condition/>
	                </target>
	            </range>
	        </link>
	        <link/>
	        ...
	    </mapping>
	    <mapping/>
	    ...
	</mappings>

Existence can be checked with a condition containing xpath:

    <condition>
		<exists>
			<xpath>...</xpath>
		</exists>
	</condition>

Equality is checked with both an xpath to be evaluated and a literal for comparison:

    <condition>
		<equals>
			<xpath>...</xpath>
			<literal>...</literal>
		</equals>
    </condition>

An option that will be implemented later when there is a SKOS vocabulary available for querying:

    <condition>
		<narrower_than>
			<xpath>...</xpath>
			<literal>...</literal>
		</narrower_than>
    <condition>

Conditions can also be combined into boolean expressions (here **ANY** is to mean any of the six conditional elements):

    <condition>
		<and>
			<ANY/>
			...
		</and>
    <condition>

    <condition>
		<or>
			<ANY/>
			...
		</or>
    </condition>

    <condition>
		<not>
			<ANY/>
		</and>
    </condition>


## Source

The source element provides the engine with the information needed to navigate the source record.  First, the source of the *domain* is used as a kind of "anchor" and then the *links* are traversed such that their rules determine what RDF statements can be made about the source.

	<source>
		<xpath>...</xpath>
	</source>

The *source* element is also present in path and range, and these sources are evaluated within the context of the domain/source.

	Note: Considering the range/source to be an extension of the path/source appears to be mistaken, but it is not yet clear how the two are to be used.  If they are independent and the range/source value gives a separate collection of nodes from the path/source, then how to have them result in triples domain-path-range is not clear.

## Target

The domain and range contain *target* blocks, which can either contain/generate URIs or represent literals:

	<target>
		<uri>...</uri>
	</target>

	<target>
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
	</target>

	<target>
		<class>...</class>
		<literal>...</literal>
	</target>

The arguments of the URI functions are named and optional, since there is logic within the URI Generation to anticipate omissions.  The type of argument is determined by the element encapsulating the actual value.  Initially the two options of either *literal* values or *xpath* expressions which will be evaluated in the current context (see Source below) in order to fetch information from the source record.

## Path and Range Extensions

	Note: This section is still formulated in terms of entity and property, which needs changing.  Currently reviewing documentation to find out how to do this.

The basic *path* block contains *source* and *property*, but it can also have *internal_node* instances.  Similarly, the *range* block contains *source* and *entity*, but it can also have *additional_node* instances.

	<path>
		<source/>
		<target/>
		<internal_node>
			<entity/>
			<property/>
		</internal_node>
		...
	</path>
	<range>
		<source/>
		<target/>
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

