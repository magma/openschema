/*
 * Copyright (c) 2020, The Magma Authors
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Foundation

///This class handles generating, storing and retrieving the UUID.
public class UUIDManager {
    
    public static let shared = UUIDManager()
    private let KEY_UUID = "uuid"
    private var uuid : String = "UNKNOWN_UUID"
    
    private init() {
        GenerateUUID()
    }
    
    /**
    This function checks if there is a UUID already stored for the installed app.
    */
    
    private func UUIDexists(key: String) -> Bool {
        return UserDefaults.standard.object(forKey: key) != nil
    }
    
    
    /**
    This function generates if there is no UUID already stored for the installed app. Calls func UUIDexists.
    */
    private func GenerateUUID() {
        if UUIDexists(key: "uuid"){
            self.uuid = UserDefaults.standard.string(forKey: self.KEY_UUID) ?? "Error in UUID"
        } else {
            self.uuid = UUID().uuidString
            UserDefaults.standard.set(self.uuid , forKey: self.KEY_UUID)
        }
    }
    
    /**
    This function retrieves the stored UUID.
    */
    
    public func getUUID() -> String { return uuid.lowercased() }
    
    /**
    This function overrides current stored UUID. Currently unused function. Might be useful if want to register device to magma with a new UUID without uninstalling app from device.
    */
    public func setUUID(uuid : String) { self.uuid = uuid }
        
}
