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
#if os(iOS) || os(watchOS) || os(tvOS)
    import UIKit
#endif

public class DeviceMetrics {

    ///CustomMetrics class object.
    private let customMetrics = CustomMetrics()
    
    //Label Names
    private let deviceModel : String = "model"
    private let IOSVersion : String = "os_version"
    
    //Family Name
    private let familyName : String = "openschema_ios_device_info"
    
    public init() {
        
    }
    
    public func CollectDeviceInfoMetrics() {

        var labelContainer : [(String, String)] = [(String, String)]()
        labelContainer.append((self.deviceModel, UIDevice.current.systemVersion))
        labelContainer.append((self.IOSVersion, UIDevice.current.model))
        
        if(customMetrics.CreateSimpleMetric(familyName: self.familyName, LabelContainer: labelContainer, metricValue: 0)) {
            print("Device Metrics Stored")
        } else {
            print("Failed to Store Device Metrics")
        }
    }
    
}
