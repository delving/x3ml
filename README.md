# X3ML Engine
---
## Introduction

X3ML is an XML based language which describes schema mappings in such a way that they can be collaboratively created and discussed by experts.  Mappings have been done in very many custom ways in the past, so the emphasis is on establishing a standardized mapping description which lends itself to collaboration and the building of a mapping memory to accumulate knowledge and experience.

The X3ML mappings which the project currently focuses on will be those for extracting **CIDOC-CRM** triples from **XML** source.  Eventually different source and target schemas will be explored.

## Background

This X3ML Engine implementation makes up a part of the CultureBrokers project, co-funded by the Swedish Arts Council and the British Museum.  It is being developed by Delving BV in close coordination with Martin Doerr of the Foundation for Research and Technology Hellas (FORTH) in Crete, and it is a key part of the **Mapping Reference Model** that he has proposed to the CIDOC CRM-SIG.

## Details

The most important aspects of this engine is its two main functions, and the careful separation between them:

* **[Schema Mapping](https://github.com/delving/x3ml/blob/master/docs/x3ml-schema-mapping.md)** - the mapping language XML format

* **[Value Generation](https://github.com/delving/x3ml/blob/master/docs/x3ml-value-generation.md)** - the mechanism of generating URIs and labels.

## Visualized

|[View Prezi on prezi.com](http://prezi.com/0tor__p-a0kj/?utm_campaign=share&utm_medium=copy&rc=ex0share)
|
|![Prezi](docs/X3ML-Prezi.png?raw=true =680x480)  

## Development

This project is a straightforward Maven 3 project, producing a single artifact in the form of a JAR file which contains the engine software.  The artifact will be used in a variety of different contexts, so the main focus of this project is to create exhaustively tested transformation engine.  Examples of input and expected output have been prepared by the participating organizations.

As the project progresses, more information will be provided regarding integration and deployment of this engine.

## Design Principles

* **Simplicity**

	It is easier to create complicated things than it is to find the simplicity in something that would otherwise be complex.  One important way to achieve simplicity and clarity is by carefully naming things so that their meaning is as obvious as possible to the naked eye.
	
* **Transparency**

	The most important feature of X3ML is its general application to mapping creation and execution and hopefully its longevity.  People must be able to easily understand how it works.  The **cleaner** the core design of this engine and X3ML language, and the clearer its documentation, the more readily it will get traction and become the basis for future mappings.

* **Collaborative Mapping Memory**

	The X3ML mapping descriptions must lend themselves to being stored and handled by collaborative tools, as well as potentially written by hand.  This was the motivation for choosing a simple syntax in XML, and one which does not depend on implicit knowledge.  This particular project is about developing an engine that executes X3ML to extract triples from XML, but related projects will focus on making the X3ML easy to build, discuss, and edit.

* **Separation between Schema Mapping and URI Generation**

	Schema mapping needs to be separated from the concern of generating proper URIs so that different expertise can be applied these two very different responsibilities.  The URI expert must ensure that the generated URIs match certain criteria such as consistency and uniqueness, while the Schema experts only need to concern themselves with the proper interpretation of the source.

* **Re-use of Standards and Technologies**

	The best way to build a new software module is to carefully choose its dependencies, and keeping them as small as possible.  Building on top of proven technologies is the quickest way to a dependable result.

	* **[XStream](http://xstream.codehaus.org/)** - easy reading/writing of XML 
	
	* **[Handy URI Templates](https://github.com/damnhandy/Handy-URI-Templates)** - standardized URI generation [RFC 6570](http://tools.ietf.org/html/rfc6570)
	
	* **[Jena](https://jena.apache.org/)** - in-memory building of graph for RDF output

* **Facilitating Instance Matching**

	An application of X3ML which came up during discussions at the beginning of this project involved extracting semantic information with the intent of finding correct instance URIs.  This implies a relatively small extension to the original idea of the X3ML engine because it will have to provide modified source records as well as RDF in its output.
	
	When [instance matching](http://prezi.com/povcuuboyyg5/culture-brokers-enrichment/) is performed and URIs are found, it must be explcitly known how to substitute them back into the source data.  The X3ML engine will decorate the source record tree with placeholders so that the results of the instance matching can find their way back to the right locations in the source.


---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;