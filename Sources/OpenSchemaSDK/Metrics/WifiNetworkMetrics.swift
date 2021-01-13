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

///This class handles the GRPC creation of Wi-Fi bundles and metrics that will be pushed using MetricsManager.
public class WifiNetworkMetrics {
    
    private let wifiNetworkinfo = WifiNetworkInfo.shared
    private let customMetrics = CustomMetrics()
    
    //Label Names
    private let ssidLabelName : String = "ssid"
    private let bssidLabelName : String = "bssid"

    //Family Name
    private let wifiNetworkInfoFamilyName = "openschema_ios_wifi_network_info"
    
    public init(){
        
    }

    ///Using CreateGRPCStringMetric it collects the values from Wi-Fi connection  and return a Magma_Orc8r_MetricsContainer with them.
    public func CollectWifiNetworkInfoMetrics() -> Magma_Orc8r_MetricFamily {
        
        let ssidLabel = customMetrics.CreateLabelPair(labelName: self.ssidLabelName, labelValue: self.wifiNetworkinfo.getSSID())
        let bssidLabel = customMetrics.CreateLabelPair(labelName: self.bssidLabelName, labelValue: self.wifiNetworkinfo.getBSSID())
        var labelContainer : MagmaLabelContainer = MagmaLabelContainer()
        labelContainer.append(ssidLabel)
        labelContainer.append(bssidLabel)
        
        let wifiMetrics = customMetrics.CreateSimpleMetric(simpleMetricType: .gauge, labelContainer: labelContainer, value: 1)
        
        var metricContainer : MagmaMetricContainer = MagmaMetricContainer()
        metricContainer.append(wifiMetrics)
        
        return customMetrics.CreateFamilyForSimpleMetric(simpleMetricType: .gauge, metrics: metricContainer, familyName: self.wifiNetworkInfoFamilyName)
    }
    
}
