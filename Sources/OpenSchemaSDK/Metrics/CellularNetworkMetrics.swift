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

///This class handles the GRPC creation of cellular bundles and metrics that will be pushed using MetricsManager.
public class CellularNetworkMetrics {
    
    ///CellularNetworkInfo class object.
    private let cellularNetworkInfo : CellularNetworkInfo = CellularNetworkInfo()
    ///CustomMetrics class object.
    private let customMetrics = CustomMetrics()
    
    //Label Names
    ///Carrier Label Name
    private let carrierNameLabelName : String = "carrier_name"
    ///Mobile Network Code Label Name
    private let mobileNetworkCodeLabelName : String = "mobile_network_code"
    ///Mobile Country Code Label Name
    private let mobileCountryCodeLabelName : String = "mobile_country_code"
    ///ISO  Country Code Label Name
    private let isoCountryCodeLabelName : String = "iso_country_code"
    ///Radio Technology Label Name
    private let radioTechnologyLabelName : String = "radio_technology_code"
    
    //Family Name
    ///Cellular Network Info Family Name
    private let cellularNetworkInfoFamilyName = "openschema_ios_cellular_network_info"
    
    public init(){
        
    }
    
    ///Using CreateGRPCStringMetric it collects the values from cellular connection  and return a Magma_Orc8r_MetricsContainer with them
    public func CollectCellularNetworkInfoMetrics() -> Magma_Orc8r_MetricFamily {

        let carrierLabel = customMetrics.CreateLabelPair(labelName: self.carrierNameLabelName, labelValue: self.cellularNetworkInfo.getCarrierName())
        let mobileNetworkCodeLabel = customMetrics.CreateLabelPair(labelName: self.mobileNetworkCodeLabelName, labelValue: self.cellularNetworkInfo.getMobileNetworkCode())
        let mobileCountryCodeLabel = customMetrics.CreateLabelPair(labelName: self.mobileCountryCodeLabelName, labelValue: self.cellularNetworkInfo.getMobileCountryCode())
        let isoCountryCodeLabel = customMetrics.CreateLabelPair(labelName: self.isoCountryCodeLabelName, labelValue: self.cellularNetworkInfo.getIsoCountryCode())
        let radioTechnologyLabel = customMetrics.CreateLabelPair(labelName: self.radioTechnologyLabelName, labelValue: self.cellularNetworkInfo.getCurrentRadioAccessTechnology())

        var labelContainer : MagmaLabelContainer = MagmaLabelContainer()
        labelContainer.append(carrierLabel)
        labelContainer.append(mobileNetworkCodeLabel)
        labelContainer.append(mobileCountryCodeLabel)
        labelContainer.append(isoCountryCodeLabel)
        labelContainer.append(radioTechnologyLabel)
        
        let cellularMetrics = customMetrics.CreateSimpleMetric(simpleMetricType: .gauge, labelContainer: labelContainer, value: 1)
        
        var metricContainer : MagmaMetricContainer = MagmaMetricContainer()
        metricContainer.append(cellularMetrics)
        
        return customMetrics.CreateFamilyForSimpleMetric(simpleMetricType: .gauge, metrics: metricContainer, familyName: self.cellularNetworkInfoFamilyName)
    }
}
