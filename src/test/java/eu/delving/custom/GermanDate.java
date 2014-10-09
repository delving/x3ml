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

package eu.delving.custom;

import static eu.delving.x3ml.X3MLGeneratorPolicy.CustomGenerator;
import static eu.delving.x3ml.X3MLGeneratorPolicy.CustomGeneratorException;

/**
 * an excample date interpreter
 */

public class GermanDate implements CustomGenerator {

    private String text;
    private Bounds bounds;

    enum Bounds {
        Upper, Lower
    }

    @Override
    public void setArg(String name, String value) throws CustomGeneratorException {
        if ("text".equals(name)) {
            text = value;
        }
        else if ("bound".equals(name)) {
            bounds = Bounds.valueOf(value);
        }
        else {
            throw new CustomGeneratorException("Unrecognized argument name: " + name);
        }
    }


    @Override
    public String getValue() throws CustomGeneratorException {
        if (text == null) {
            throw new CustomGeneratorException("Missing text argument");
        }
        if (bounds == null) {
            throw new CustomGeneratorException("Missing bounds argument");
        }
        return lookupCheat(bounds.toString(), text);
    }

    public String getValueType() throws CustomGeneratorException {
        if (text == null) {
            throw new CustomGeneratorException("Missing text argument");
        }
        
            return "Literal";
        
    }

    
    private static String lookupCheat(String bounds, String value) {
        for (String[] entry : CHEAT) {
            if (entry[0].equals(value) && entry[1].equals(bounds)) {
                return entry[2];
            }
        }
        return String.format("Cheat needed for %s:%s", bounds, value);
    }

    private static String[][] CHEAT = {
            {"-116", "Lower", "-0116-12-31T00:00:00"},
            {"-116", "Upper", "-0116-01-01T00:00:00"},
            {"-115", "Lower", "-0115-12-31T00:00:00"},
            {"-115", "Upper", "-0115-01-01T00:00:00"}
    };
}
