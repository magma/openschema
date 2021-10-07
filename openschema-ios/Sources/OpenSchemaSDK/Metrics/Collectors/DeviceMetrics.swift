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
public class DeviceMetrics {
    
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
    
    ///Collects information about the device and generates a dictionary
    /*public func retrieveMetrics() -> [String:String] {
        Log.debug("MMA: Generating device metrics...")

        var metricDictionary : [String:String] = [:]
        
        metricDictionary[METRIC_OS_VERSION] = UIDevice.current.systemVersion
        metricDictionary[METRIC_MODEL] = UIDevice.current.model
        metricDictionary[METRIC_MANUFACTURER] = "Apple Inc."
        metricDictionary[METRIC_BRAND] = "Apple"
        
        //TODO: Add debugging flag to enable detailed metrics
        Log.debug(Logger.Message(stringLiteral: "MMA: Collected metrics:\n" + metricDictionary.description))

        return metricDictionary
    }*/
    
}
