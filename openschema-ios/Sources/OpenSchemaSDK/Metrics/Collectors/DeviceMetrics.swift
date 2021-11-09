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
import UIKit

///Collects metrics related to the device.
public class DeviceMetrics : SyncMetrics{
    
    private let Log : Logger = Logger(label: "DeviceMetrics")
    
    ///Metric name to be used for the collected information.
    public let METRIC_NAME : String = "openschemaDeviceInfo";
    private let METRIC_OS_VERSION : String = "osVersion";
    private let METRIC_MODEL : String = "model";
    private let METRIC_MANUFACTURER : String = "manufacturer";
    private let METRIC_BRAND : String = "brand";
    private let METRIC_OPENSCHEMA_VERSION : String = "openschemaVersion"; //TODO: need to decide on a versioning scheme

    ///Metric values.
    private let iOSVersion : String
    private let deviceManufacturer : String = "Apple Inc"
    private let deviceModel : String
    private let deviceBrand : String = "Apple Inc"

    public init() {
        self.iOSVersion = String((UIDevice.current.systemVersion as NSString).intValue) //UIDevice.current.systemVersion -> Gives string with like 15.0.2
        self.deviceModel = UIDevice.current.model
    }
    
    public func storeInCoredata() -> Void {
        let deviceInfo = DeviceInfo()
        deviceInfo.iOSVersion = iOSVersion
        deviceInfo.deviceManufacturer = deviceManufacturer
        deviceInfo.deviceBrand = deviceBrand
        deviceInfo.deviceModel = deviceModel
        deviceInfo.timestamp = Date().millisecondsSince1970
        deviceInfo.offsetMinutes = 0
        
        do {
            try CoreDataController.shared.deviceInfoDao.save(object: deviceInfo)
            Log.debug("Collected Device Information Succesfully!")
            
        } catch {
            Log.error("Failed to Collect Device Information: \(error)")
        }
    }
    
    public func retrieveMetrics() -> [[String : Any]] {
        
        //For current server version cast OS version to Int
        let iosVersion : Int = Int(self.iOSVersion) ?? 0
        
        let json : [[String : Any]] = [
            ["first" : METRIC_OS_VERSION , "second" : iosVersion ],
            ["first" : METRIC_MODEL , "second" : self.deviceModel ],
            ["first" : METRIC_MANUFACTURER , "second" : self.deviceManufacturer ],
            ["first" : METRIC_BRAND , "second" : self.deviceBrand ]
        ]
        
        return json
    }
    
}
