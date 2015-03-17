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

/**
 * Combining path and range for when the source_relation is a key comparison
 * rather than xpath.
 *
 * @author Gerald de Jong <gerald@delving.eu>
 */
public class Link {

    public final Path path;
    public final Range range;

    public Link(Path path, Range range) {
        this.range = range;
        this.path = path;
    }

    public boolean resolve() {
        boolean resolved = path.resolve();
        if (resolved) {
            range.resolve();
        }
        return resolved;
    }

}
