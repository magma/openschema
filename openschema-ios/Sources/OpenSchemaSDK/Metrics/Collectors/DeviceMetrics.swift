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
    public let METRIC_OS_VERSION : String = "osVersion";
    public let METRIC_MODEL : String = "model";
    public let METRIC_MANUFACTURER : String = "manufacturer";
    public let METRIC_BRAND : String = "brand";
    public let METRIC_OPENSCHEMA_VERSION : String = "openschemaVersion"; //TODO: need to decide on a versioning scheme

    ///Metric name to be used for the collected information.
    public let iOSVersion : String
    public let deviceManufacturer : String = "Apple Inc."
    public let deviceModel : String
    public let deviceBrand : String = "Apple Inc."

    public init() {
        self.iOSVersion = UIDevice.current.systemVersion
        self.deviceModel = UIDevice.current.model
    }
    
    public func retrieveMetrics() -> Void {
        
        let deviceInfo = DeviceInfo()
        deviceInfo.iOSVersion = iOSVersion
        deviceInfo.deviceManufacturer = deviceManufacturer
        deviceInfo.deviceBrand = deviceBrand
        deviceInfo.deviceModel = deviceModel
        deviceInfo.timestamp = Date().millisecondsSince1970
        deviceInfo.offsetMinutes = 0
        
        do {
            try CoreDataController.shared.deviceInfoDao.save(object: deviceInfo)
            print("Collected Device Information Succesfully!")
            Log.debug("Collected Device Information Succesfully!")
            
        } catch {
            print("error")
            Log.error("Failed to Collect Device Information")
        }
    }
    
}
