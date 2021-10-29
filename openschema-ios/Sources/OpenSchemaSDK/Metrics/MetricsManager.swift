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

///Class in charge of handling pushing metrics to the controller.
public class MetricsManager {
    
    private static let TAG : String = "MetricsManager"

    private let uuidManager = UUIDManager.shared
    private let cellularNetworkMetrics : CellularNetworkMetrics = CellularNetworkMetrics()
    private let wifiNetworkMetrics : WifiNetworkMetrics = WifiNetworkMetrics()
    private let deviceMetrics : DeviceMetrics = DeviceMetrics()

    public init() {
        CoreDataController.setup(storageContext: CoreDataStorageContext())
    }

    private func IsCoreDataEmpty() -> Bool {
     
        //Testing

            
        /*print("CoreData has " + String(count) + " DeviceInfo Entities")
        
        if (count == 0) {
            print("Database is empty")
            return true
        }*/
        
        
 
        return false
    }
    
    ///This function is called after a succesful bootstrap to push available metrics to server
    public func CollectAndPushMetrics() {
        
        if (!IsCoreDataEmpty())
        {
            
        } else {
            print("No Metrics stored to be collected")
        }
    }
    
    public func countObjects() -> Int {
        CoreDataController.shared.deviceInfoDao.countObjects(entityName: "DeviceInfoEntity")
    }
    
    public func fetchAllByEntity(entityName: String) -> [Storable] {
        return CoreDataController.shared.deviceInfoDao.fetchAllByEntity(entityName: entityName)
    }
    
    public func encodeDeviceInfo() {
        let filter = "Apple Inc."
        let deviceInfoObjects : [DeviceInfo] = CoreDataController.shared.deviceInfoDao.fetch(predicate: NSPredicate(format: "deviceManufacturer == %@",filter))
        
        print(deviceInfoObjects.count)
    }
    
    public func deleteAllCoreData() {
        do {
            try CoreDataController.shared.deviceInfoDao.deleteAll()
            
        } catch {
            
        }
    }
}
