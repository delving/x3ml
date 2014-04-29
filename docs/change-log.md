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

---

Contact: Gerald de Jong &lt;gerald@delving.eu&gt;



