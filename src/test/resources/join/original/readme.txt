1. 
04.Coin-Join
In this example there is a data base join (<source type=JOIN>) which is denoted in the source path as "COIN/FIND_SPOT_ID = FIND_SPOT/FS_ID"
This means that there is a join between the tables COIN (field FIND_SPOT_ID) and FIND_SPOT (field FS_ID).

The example also includes  an internal node.

2. 
05.Coin-Join-InternalNode
This example is the same as 04 but it introduces a variable for the Internal Node. 
The scope of this variable is the Domain so it will keep the same value for each instance of the Domain.
The first time that a URI function is called and there is a variable involved, the URI function should create the URI and "associate" it with the variable.
So if the variable is encountered later on, the URI will not be created again. 
In this example  the variable is used only once so  it makes no difference in the produced rdf compared to example 04 that has no variable.

3.
06.Coin-doubleE10-doubleIN-Literal
This example has two internal nodes that have the same variable, so they map to the same object. 
E10_Transfer_of_Custody (tc1) is created once for the COIN/FIND_SPOT_ID = FIND_SPOT/FS_ID and once for COIN.FIND_DATE
The URI function is not called the second time since the URI has already been produced and the variable tc1 is associated with the produced URI.

Also, in this example the COIN.FIND_DATE has 2 internal nodes and a Literal.

For all 3 examples, I send you the input x3ml (old format), and the output in  RDF/XML and NT format (thanks to Barry's suggestion)

