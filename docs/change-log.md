# Change Log

---

## 27 Mar 2014: Initial release v1.0

* XSD Schema validation of X3ML integrated
* Command line has -validation option
* Multiple <class> within <entity> needs some work
* Approach to define generator policy needs to be discussed

## 29 April 2014: V 1.1

* Major refactor for code legibility, licensing, and javadoc
* The *property* no longer contains *class* but instead itself holds the qualified name
* The tag *property* has been replaced with *relationship*.
* The *class* element is now *type*.
* The *value_generator* is now *instance_generator*
* Multiple *type* possible within entity, variables hold lists now
* Generator argument types and global *source_type* now lowercase
* Argument type *position* introduced, giving the index of the node in its node list
* Language handling
	* An XPath argument tries to find language from xml:lang in source
	* Generators can override using *language* attribute, language="" for none at all
* Command line formats now "application/rdf+xml", "text/turtle" and "application/n-triples"

## 6 July 2014: V 1.2

* Entity element now has *instance_info* attribute 
* *type* attribute added to instance_generator arg
* *ArgValues* interface moved inside *Generator* interface
* Introduced usage of *createTypedLiteral* 
* For specialized instance generation code, introduced *CustomGenerator* interface
* Added *custom* element to GeneratorPolicy, with *generatorClass* which implements *CustomGenerator*
* Better generation of test UUIDs, command line can specify size.
* *DomainContext(path)* syntax added to XPath for non-hierarchical source
* Removed *namespaces* from generator policy, inheriting from X3ML namespaces instead

## 21 July 2014: V 1.2.1

* fixed bug with variables - labels and additionals were ignored if not in the first var usage


---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;



