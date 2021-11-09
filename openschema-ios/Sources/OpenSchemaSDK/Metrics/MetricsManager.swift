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
    
    public func countObjects() -> Int {
        CoreDataController.shared.deviceInfoDao.countObjects(entityName: "DeviceInfoEntity")
    }
    
    public func fetchAllByEntity(entityName: String) -> [Storable] {
        return CoreDataController.shared.deviceInfoDao.fetchAllByEntity(entityName: entityName)
    }
    
    public func deleteAllCoreData() {
        do {
            try CoreDataController.shared.deviceInfoDao.deleteAll()
            
        } catch {
            
        }
    }
}
