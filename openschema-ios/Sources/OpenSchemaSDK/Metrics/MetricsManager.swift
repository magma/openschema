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

import Logging
import Dispatch
import CoreData

extension Date {
    var millisecondsSince1970:Int64 {
        return Int64((self.timeIntervalSince1970 * 1000.0).rounded())
    }

    init(milliseconds:Int64) {
        self = Date(timeIntervalSince1970: TimeInterval(milliseconds) / 1000)
    }
}

///Class in charge of handling pushing metrics to the controller.
public class MetricsManager {
    
    private static let TAG : String = "MetricsManager"
    
    private let signedCert : Data
    private let uuidManager = UUIDManager.shared
    private let cellularNetworkMetrics : CellularNetworkMetrics = CellularNetworkMetrics()
    private let wifiNetworkMetrics : WifiNetworkMetrics = WifiNetworkMetrics()
    private let deviceMetrics : DeviceMetrics = DeviceMetrics()
    private var certificateFilePath : String


    init(signedCert : Data , certificateFilePath : String) {
        
        self.signedCert = signedCert
        self.certificateFilePath = certificateFilePath
        
    }

    /*private func IsCoreDataEmpty() -> Bool {
        do {
            
            let request = NSFetchRequest<NSFetchRequestResult>(entityName: "MetricFamily")
            let count  = try coreDataController.managedObjectContext.count(for: request)
                
            if (count == 0) {
                print("Database is empty")
                return true
            }
                
            } catch {
                return true
            }
        
        return false
    }*/
    
    ///This function is called after a succesful bootstrap to push available metrics to server
    /*public func CollectAndPushMetrics() {
        
        if (!IsCoreDataEmpty())
        {
            
        } else {
            print("No Metrics stored to be collected")
        }
    }*/
    
}
