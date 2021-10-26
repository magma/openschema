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

extension Date {
    var millisecondsSince1970:Int64 {
        return Int64((self.timeIntervalSince1970 * 1000.0).rounded())
    }

    init(milliseconds:Int64) {
        self = Date(timeIntervalSince1970: TimeInterval(milliseconds) / 1000)
    }
}

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
    private let iOSVersion : String
    private let deviceManufacturer : String = "Apple Inc."
    private let deviceModel : String
    private let deviceBrand : String = "Apple Inc."

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
        deviceInfo.timestamp = Date.init(milliseconds: Date.now)
        deviceInfo.offsetMinutes = 0
        
        do {
            try DBManager.shared.storyDao.save(object: deviceInfo)
            Log.debug("Collected Device Information Succesfully!")
            
        } catch {
            Log.error("Failed to Collect Device Information")
        }
    }
    
}
