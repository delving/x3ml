package eu.delving.x3ml.engine;

/**
 * The place where URIs are generated
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class GeneratorFunction {
    private final Context context;
    private final Path path;
    private final URIFunction.Args args;
    private final Entity domainEntity;

    public GeneratorFunction(Context context, Entity domainEntity, Path path, URIFunction.Args args) {
        this.context = context;
        this.domainEntity = domainEntity;
        this.path = path;
        this.args = args;
    }

    // Physical Object
    // uriForPhysicalObjects(String className, String nameOfMuseum, String entry)
    // <E19_Physical_Object rdf:about="http://turistipercaso.it/Galleria_degli_Uffizi_—_Pinacoteca_(Florence)/8360_(Inv. 1890)">

    public String uriForPhysicalObject() {
        return args.get("museumName") + ":" + args.get("entry");
    }

    // uriPhysThing(String className, String thing)

    public String uriForPhysicalThing() {
        return "uri";
    }

    // uriType(String className, String type)

    public String uriForType() {
        return "uri";
    }

}
