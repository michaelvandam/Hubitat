/**
 *  Virtual Smoke Detector for alarm panel zones
 *
 *  Copyright 2019 R. Michael van Dam
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  VERSION 1.0.0
 *  UPDATE HISTORY
 *  2019-12-20 - R. Michael van Dam - Initial Release
 */

metadata {
    definition (name: "Alarm Virtual Smoke Detector", namespace: "alarm", author: "R. Michael van Dam")
    {
        capability "Smoke Detector"
        attribute "bypass", "ENUM", ["bypass", "clear"]
    }
}
