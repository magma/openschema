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
    public func CollectWifiNetworkInfoMetrics() {
        
        var labelContainer : [(String, String)] = [(String, String)]()
        labelContainer.append((self.ssidLabelName, self.wifiNetworkinfo.getSSID()))
        labelContainer.append((self.bssidLabelName, self.wifiNetworkinfo.getBSSID()))
        
        if(customMetrics.CreateSimpleMetric(familyName: wifiNetworkInfoFamilyName, LabelContainer: labelContainer, metricValue: 0)) {
            print("Wi-fi Metrics Stored")
        } else {
            print("Failed to Store Wi-Fi Metrics")
        }
    }
    
}
