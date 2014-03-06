package eu.delving.x3ml;

import org.apache.commons.cli.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class Command {
    static final CommandLineParser PARSER = new PosixParser();
    static final HelpFormatter HELP = new HelpFormatter();
    static Options options = new Options();

    static void error(String message) {
        HELP.setDescPadding(5);
        HELP.setLeftPadding(5);
        HELP.printHelp(
                200,
                "x3ml -xml <input records> -x3ml <mapping file> [ -rdf <output file> ] [ -format <format> ]",
                "Options",
                options,
                message
        );
        System.exit(1);
    }

    public static void main(String[] args) {
        Option xml = new Option(
                "xml", true,
                "XML input records"
        );
        xml.setRequired(true);
        Option x3ml = new Option(
                "x3ml", true,
                "X3ML mapping definition"
        );
        x3ml.setRequired(true);
        Option rdf = new Option(
                "rdf", true,
                "The RDF output file name"
        );
        Option policy = new Option(
                "policy", true,
                "The value policy file"
        );
        Option rdfFormat = new Option(
                "format", true,
                "Output format: XML (default), N-TRIPLE, TURTLE"
        );
        options.addOption(rdfFormat).addOption(rdf).addOption(x3ml).addOption(xml).addOption(policy);
        try {
            CommandLine cli = PARSER.parse(options, args);
            go(
                    cli.getOptionValue("xml"),
                    cli.getOptionValue("x3ml"),
                    cli.getOptionValue("policy"),
                    cli.getOptionValue("rdf"),
                    cli.getOptionValue("format")
            );
        }
        catch (ParseException e) {
            error(e.getMessage());
        }

    }

    static File file(String name) {
        File file = new File(name);
        if (!file.exists() || !file.isFile()) {
            error("File does not exist: " + name);
        }
        return file;
    }

    static DocumentBuilderFactory documentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory;
    }

    static Element xml(File file) {
        try {
            DocumentBuilder builder = documentBuilderFactory().newDocumentBuilder();
            FileInputStream inputStream = getStream(file);
            Document document = builder.parse(inputStream);
            return document.getDocumentElement();
        }
        catch (Exception e) {
            throw new X3MLException("Unable to parse " + file.getName());
        }
    }

    static FileInputStream getStream(File file) {
        try {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    static int uuidVal = 1;
    static String uuid() {
        return String.valueOf(uuidVal++);
    }

    static X3ML.ValuePolicy getValuePolicy(String policy) {
        if (policy == null) {
            return new X3ML.ValuePolicy() {
                @Override
                public X3ML.Value generateValue(String name, X3ML.ValueFunctionArgs arguments) {
                    X3ML.Value value = new X3ML.Value();
                    if ("UUID".equals(name)) {
                        value.uri = uuid();
                    }
                    else if ("UUID_Label".equals(name)) {
                        X3ML.ArgValue labelQName = arguments.getArgValue("labelQName", X3ML.SourceType.QNAME);
                        if (labelQName == null || labelQName.qualifiedName == null) {
                            throw new X3MLException("Argument failure: labelQName");
                        }
                        value.uri = uuid();
                        value.labelQName = labelQName.qualifiedName;
                        X3ML.ArgValue labelXPath = arguments.getArgValue("labelXPath", X3ML.SourceType.XPATH);
                        if (labelXPath == null || labelXPath.string == null) {
                            throw new X3MLException("Argument failure: labelXPath");
                        }
                        value.labelValue = labelXPath.string;
                    }
                    return value;
                }
            };
        }
        else {
            return X3MLValuePolicy.load(getStream(file(policy)));
        }
    }

    static PrintStream rdf(String file) {
        if (file != null) {
            try {
                return new PrintStream(new File(file));
            }
            catch (FileNotFoundException e) {
                error(e.getMessage());
                return null;
            }
        }
        else {
            return System.out;
        }
    }

    static void go(String xml, String x3ml, String policy, String rdf, String rdfFormat) {
        Element xmlElement = xml(file(xml));
        X3MLEngine engine = X3MLEngine.load(getStream(file(x3ml)));
        X3MLContext context = engine.execute(xmlElement, getValuePolicy(policy));
        context.write(rdf(rdf), rdfFormat);
    }
}
