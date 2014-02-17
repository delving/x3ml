package eu.delving.x3ml;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class URIPolicy implements X3ML.URIPolicy {

    private Map<String, Generator> generatorMap = new TreeMap<String, Generator>();

    public URIPolicy() {
        addGenerator(new PhysicalObject());
        addGenerator(new Type());
    }

    private void addGenerator(Generator generator) {
        String qualifiedName = generator.getClass().getName();
        String name = qualifiedName.substring(qualifiedName.lastIndexOf('$') + 1);
        generatorMap.put(name, generator);
    }

    @Override
    public String generateUri(String name, X3ML.URIArguments arguments) {
        Generator generator = generatorMap.get(name);
        if (generator == null) throw new X3MLException("No generator found for " + name);
        return generator.generateUri(arguments);
    }

    private interface Generator {
        String generateUri(X3ML.URIArguments arguments);
    }

    private class PhysicalObject implements Generator {
        @Override
        public String generateUri(X3ML.URIArguments arguments) {
            return arguments.getClassName() + ":" + encode(arguments.getArgument("nameOfMuseum") + ", " + arguments.getArgument("entry"));
        }
    }

    private class Type implements Generator {
        @Override
        public String generateUri(X3ML.URIArguments arguments) {
            return arguments.getClassName() + ":" + encode(arguments.getArgument("nameOfMuseum"));
        }
    }

    private static String encode(String unencoded) {
        try {
            return URLEncoder.encode(unencoded, "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new X3MLException("Unable to encode: "+unencoded);
        }
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
