package eu.delving.x3ml;

import org.apache.commons.io.IOUtils;
import org.apache.xerces.impl.Constants;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Gerald de Jong <gerald@delving.eu>
 */

public class X3MLValidator {

    public List<String> validate(InputStream inputStream) throws SAXException, IOException {
        Schema schema = schemaFactory().newSchema(new StreamSource(inputStream("x3ml_v1.0.xsd")));
        Validator validator = schema.newValidator();
        final List<String> errors = new ArrayList<String>();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                errors.add(errorMessage(exception));
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                errors.add(errorMessage(exception));
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                errors.add(errorMessage(exception));
            }
        });
        StreamSource source = new StreamSource(inputStream);
        validator.validate(source);
        return errors;
    }

    private static SchemaFactory schemaFactory() {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(Constants.W3C_XML_SCHEMA10_NS_URI);
        try {
            schemaFactory.setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.CTA_FULL_XPATH_CHECKING_FEATURE, true);
            schemaFactory.setResourceResolver(new ResourceResolver());
        }
        catch (Exception e) {
            throw new RuntimeException("Configuring schema factory", e);
        }
        return schemaFactory;
    }

    private static String errorMessage(SAXParseException e) {
        return String.format(
                "%d:%d - %s",
                e.getLineNumber(), e.getColumnNumber(), e.getMessage()
        );
    }

    public static class ResourceResolver implements LSResourceResolver {

        @Override
        public LSInput resolveResource(String type, final String namespaceUri, final String publicId, final String systemId, final String baseUri) {
            return new ResourceInput(publicId, systemId, baseUri);
        }

        private static class ResourceInput implements LSInput {

            private String publicId, systemId, baseUri;

            private ResourceInput(String publicId, String systemId, String baseUri) {
                this.publicId = publicId;
                this.systemId = systemId;
                this.baseUri = baseUri;
            }

            @Override
            public Reader getCharacterStream() {
                try {
                    return new InputStreamReader(getByteStream(), "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void setCharacterStream(Reader reader) {
                throw new RuntimeException();
            }

            @Override
            public InputStream getByteStream() {
                return inputStream(systemId);
            }

            @Override
            public void setByteStream(InputStream inputStream) {
                throw new RuntimeException();
            }

            @Override
            public String getStringData() {
                try {
                    return IOUtils.toString(getByteStream());
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void setStringData(String s) {
                throw new RuntimeException();
            }

            @Override
            public String getSystemId() {
                return systemId;
            }

            @Override
            public void setSystemId(String s) {
                this.systemId = s;
            }

            @Override
            public String getPublicId() {
                return publicId;
            }

            @Override
            public void setPublicId(String s) {
                this.publicId = s;
            }

            @Override
            public String getBaseURI() {
                return baseUri;
            }

            @Override
            public void setBaseURI(String s) {
                throw new RuntimeException();
            }

            @Override
            public String getEncoding() {
                return "UTF-8";
            }

            @Override
            public void setEncoding(String s) {
                throw new RuntimeException();
            }

            @Override
            public boolean getCertifiedText() {
                return false;
            }

            @Override
            public void setCertifiedText(boolean b) {
                throw new RuntimeException();
            }
        }
    }


    private static InputStream inputStream(String fileName) {
        return X3MLValidator.class.getResourceAsStream("/validation/" + fileName);
    }
}

