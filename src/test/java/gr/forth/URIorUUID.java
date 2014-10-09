//===========================================================================
//    Copyright 2014 Delving B.V.
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//===========================================================================
package gr.forth;

import static eu.delving.x3ml.X3MLGeneratorPolicy.CustomGenerator;
import static eu.delving.x3ml.X3MLGeneratorPolicy.CustomGeneratorException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * an excample date interpreter
 */
public class URIorUUID implements CustomGenerator {

    private String text;

    @Override
    public void setArg(String name, String value) throws CustomGeneratorException {
        if ("text".equals(name)) {
            text = value;
        } else {
            throw new CustomGeneratorException("Unrecognized argument name: " + name);
        }
    }

    @Override
    public String getValue() throws CustomGeneratorException {
        if (text == null) {
            throw new CustomGeneratorException("Missing text argument");
        }
        return text;
    }

    @Override
    public String getValueType() throws CustomGeneratorException {
        if (text == null) {
            throw new CustomGeneratorException("Missing text argument");
        }       
        if (isValidURL(text)) {
            return "URI";
        } else {
            return "UUID";
        }
    }

    private boolean isValidURL(String url) {

        URL u = null;

        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }

        try {
            u.toURI();
        } catch (URISyntaxException e) {
            return false;
        }

        return true;
    }
    
//     private boolean isValidURI(String uri) {
//        URI u = null;
//        try {
//
//            u = new URI(uri);
//
//           
//        } catch (URISyntaxException e) {
//             System.out.println("3");
//            e.printStackTrace();
//            return false;
//        } 
//        return true;
//    }

}
