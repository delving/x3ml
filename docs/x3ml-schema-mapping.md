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

## Entity

The domain and range contain *entity* blocks, which can either generate URIs or represent literals:

	<entity>
		<literal>...</literal>
	</entity>

	<entity>
		<uri-function name="...">
		    <arg name="...">
		    	<xpath>...</xpath>
		   	</arg>
		    <arg name="...">
		    	<literal>...</literal>
		    </arg>
		    <arg/>
		    ...
		</uri-function>
	</entity>
	
The arguments of the URI functions are named and optional, since there is logic within the URI Generation to anticipate omissions.  The type of argument is determined by the element encapsulating the actual value.  Initially the two options of either *literal* values or *xpath* expressions which will be evaluated in the current context (see Source below) in order to fetch information from the source record.

## Source

The source element provides the engine with the information needed to navigate the source record.  First, the source of the *domain* is used as a kind of "anchor" and then the *links* are traversed such that their rules determine what RDF statements can be made about the source.

	<source>
		<xpath>...</xpath>
	</source>

The *source* element is present in domain, path, and range, and the effect is that the XPath which is considered to be local context is extended.  This means that the *source* in the *path* is appended to the one represented in the *domain*, and likewise, that *source* in the *range* is appended further to narrow context if necessary.

## Property

The property describes the nature of the connection between the *domain* and the *range* in the target schema.

	<property>
		<uri>....</uri>
	</property>

The proprty element contains the URI pointing to its definition.

## Notes

Since X3ML will be the format in which information is stored in the **Mapping Memory** it will certainly need to properly encapsulate any associated explanatory documentation or even discussion that went into the creation of any particular mapping.

	<notes>
		<note type="...">
			....
		</note>
		<note/>
		...
	</notes>

There are *notes* elements defined at the top level where all mappings are bound together, as well as in each *domain* and each *path* and *range*.  Their content can be plain text or any other special characters cointained in a **CDATA** block.  The X3ML Engine will ignore these blocks, since they are primarly for human consumption.

Tools for managing X3ML will make use of these note elements to record all information arising from the mapping building process which may be useful people accessing the mapping memory in the future.  A simple open-ended structure where each *note* has a **type** and content allows for different tools to easily define their own note structures.  We could maintain a definition of what the different known **type** values are, and this could be easily expanded without changing the schema.

---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;



















--

