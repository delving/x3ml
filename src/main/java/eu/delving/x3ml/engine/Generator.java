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
package eu.delving.x3ml.engine;

import static eu.delving.x3ml.engine.X3ML.ArgValue;
import static eu.delving.x3ml.engine.X3ML.GeneratedValue;
import static eu.delving.x3ml.engine.X3ML.SourceType;

/**
 * This is what a generator looks like to the internal code.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */

public interface Generator {

    interface UUIDSource {
        String generateUUID();
    }

    void setDefaultArgType(SourceType sourceType);

    void setLanguageFromMapping(String language);

    void setNamespace(String prefix, String uri);

    String getLanguageFromMapping();

    public interface ArgValues {
        ArgValue getArgValue(String name, SourceType sourceType);
    }

    GeneratedValue generate(String name, ArgValues arguments);
}
