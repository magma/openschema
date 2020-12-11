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
import UIKit

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
    
    public func CollectDeviceInfoMetrics() -> Magma_Orc8r_MetricFamily {
        
        let deviceModelLabel = customMetrics.CreateLabelPair(labelName: self.deviceModel, labelValue: UIDevice.current.systemVersion)
        let iosVersionLabel = customMetrics.CreateLabelPair(labelName: self.IOSVersion, labelValue: UIDevice.current.model)
        var labelContainer : LabelContainer = LabelContainer()
        labelContainer.append(deviceModelLabel)
        labelContainer.append(iosVersionLabel)
        
        let deviceMetrics = customMetrics.CreateSimpleMetric(simpleMetricType: .gauge, labelContainer: labelContainer, value: 1)
        
        var metricContainer : MetricContainer = MetricContainer()
        metricContainer.append(deviceMetrics)
        
        return customMetrics.CreateFamilyForSimpleMetric(simpleMetricType: .gauge, metrics: metricContainer, familyName: self.familyName)
    }
    
}
