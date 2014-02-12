package eu.delving.x3ml.engine;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

@XStreamAlias("uri_function")
public class URIFunction {
    @XStreamAsAttribute
    public String name;

    @XStreamImplicit
    public List<URIFunctionArg> args;

    public String generateURI(final Context context, Domain domain) {
        return generateURI(context, domain, null);
    }

    public String generateURI(final Context context, Domain domain, Path path) {
        URIGenerator generator = generators.get(name);
        if (generator == null) {
            throw new RuntimeException("No generator found for: " + name);
        }
        final Map<String, String> argMap = new TreeMap<String, String>();
        if (args != null) {
            for (URIFunctionArg functionArg : args) {
                argMap.put(functionArg.name, functionArg.evaluate(context, domain, path));
            }
        }
        String result = generator.invoke(context, domain, path, new Args() {
            @Override
            public String get(String name) {
                String value = argMap.get(name);
                if (value != null) return value;
                value = context.getConstant(name);
                if (value != null) return value;
                throw new ArgException(name);
            }
        });
        System.out.println(name + " := " + result);
        return result;
    }

    private static final String URI_FOR = "uriFor";
    private static Map<String, URIGenerator> generators = new TreeMap<String, URIGenerator>();

    static {
        for (Method method : GeneratorFunction.class.getDeclaredMethods()) {
            if (method.getName().startsWith(URI_FOR)) {
                String functionName = method.getName().substring(URI_FOR.length());
                String functionOrder = "0";
                String[] nameParts = functionName.split("_");
                if (nameParts.length > 2) throw new RuntimeException("Bad method name: " + functionName);
                if (nameParts.length == 2) {
                    functionName = nameParts[0];
                    functionOrder = nameParts[1];
                }
                URIGenerator generator = generators.get(functionName);
                if (generator == null) generators.put(functionName, generator = new URIGenerator(functionName));
                generator.setMethod(functionOrder, method);
            }
        }
    }

    public static class ArgException extends RuntimeException {
        public ArgException(String name) {
            super(name);
        }
    }

    public interface Args {
        String get(String name);
    }

    private static class URIGenerator {
        private String name;
        private Map<String, Method> methods = new TreeMap<String, Method>();

        private URIGenerator(String name) {
            this.name = name;
        }

        private void setMethod(String order, Method method) {
            this.methods.put(order, method);
        }

        public String invoke(Context context, Domain domain, Path path, Args args) {
            GeneratorFunction generatorFunction = new GeneratorFunction(context, domain, path, args);
            for (Map.Entry<String, Method> entry : methods.entrySet()) {
                try {
                    return (String) entry.getValue().invoke(generatorFunction);
                }
                catch (Exception e) {
                    if (e instanceof ArgException) {
                        // eat it, we like the taste exceptions because they tell us to try the next one in order
                        ArgException ae = (ArgException) e;
                        System.out.println("missing arg: " + ae.getMessage());
                    }
                    else {
                        throw new RuntimeException("Problem invoking: " + name, e);
                    }
                }
            }
            throw new RuntimeException("I guess we need a UUID");
        }
    }


    // =======================================================================
// Legacy work-around efforts to study:
//    public String sapply(Node node, String className) {
//        try {
//            if ("Appellation".equals(name)) {
//                // appellationURI(String className, String subjUri, String appellation)
//                argList.add(className);
////                    argList.add(domainMapResult.uri);
//                fetchArgs(node);
//            }
//            else if ("createLiteral".equals(name)) {
//                // createLiteral(String className, String type, String note)
//                argList.add(UNUSED_CLASS_NAME);
//                argList.add(node.getNodeName());
//                fetchArgs(node);
//            }
//            else if ("dimensionURI".equals(name)) {
//                // dimensionURI(String className, String subjUri, String dimensions)
//                argList.add(UNUSED_CLASS_NAME);
////                    argList.add(domainMapResult.uri);
//                fetchArgs(node);
//            }
//            else if ("uriConceptual".equals(name)) {
//                // uriConceptual(String className, String thing)
//                argList.add(className);
//                fetchArgs(node);
//            }
//            else if ("uriEvents".equals(name)) {
//                // uriEvents(String className, String authority, String eventID, String subjUri)
//                argList.add(className);
////                    argList.add(domainMapResult.uri);
//                fetchArgs(node);
//            }
//            else if ("uriForActors".equals(name)) {
//                // uriForActors(String className, String authority, String id, String name, String birthDate)
//                argList.add(className);
//                fetchArgs(node);
//            }
//            else if ("PhysicalObject".equals(name)) {
//                // uriForPhysicalObjects(String className, String nameOfMuseum, String entry)
//                argList.add(UNUSED_CLASS_NAME);
//                fetchArgs(node);
//            }
//            else if ("Place".equals(name)) {
//                // uriForPlaces(String className, String placeName, String authority, String placeID,
//                //              Stribng coordinates, String spaces)
//                argList.add(UNUSED_CLASS_NAME);
//                fetchArg(node, 0);
//                fetchArg(node, 1);
//                fetchArg(node, 2);
//                fetchArg(node, 3); // coordinates never really used
//                argList.add(getPartOfPlaceHack(node));
//            }
//            else if ("PhysicalThing".equals(name)) {
//                // uriPhysThing(String className, String thing)
//                argList.add(className);
//                fetchArgs(node);
//            }
//            else if ("uriTimeSpan".equals(name)) {
//                // uriTimeSpan(String className, String timespan)
//                argList.add(UNUSED_CLASS_NAME);
//                fetchArgs(node);
//            }
//            else if ("Type".equals(name)) {
//                // uriType(String className, String type)
//                argList.add(className);
//                fetchArgs(node);
//            }
//            else {
//                throw new RuntimeException("Unknown function name: " + name);
//            }
//            Class<?>[] types = new Class<?>[argList.size()];
//            Arrays.fill(types, String.class);
//            try {
//                Method method = POLICIES.getClass().getMethod(name, types);
//                return (String) method.invoke(POLICIES, argList.toArray());
//            }
//            catch (NoSuchMethodException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void fetchArgs(Node node) {
//        for (URIFunctionArg a : args) {
//            argList.add(valueAt(node, a.content));
//        }
//    }
//
//    private void fetchArg(Node node, int index) {
//        argList.add(valueAt(node, args.get(index).content));
//    }
//
//    private String getPartOfPlaceHack(Node node) {
//        try { // iterate into partOfPlace fetching names and then join them with dash
//            List<String> places = new ArrayList<String>();
//            while (node != null) {
//                XPathExpression expr = path().compile("lido:namePlaceSet/lido:appellationValue/text()");
//                String placeName = (String) expr.evaluate(node, XPathConstants.STRING);
//                places.add(placeName);
//                expr = path().compile("lido:partOfPlace");
//                node = (Node) expr.evaluate(node, XPathConstants.NODE);
//            }
//            return StringUtils.join(places, '-');
//        }
//        catch (XPathExpressionException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
}
