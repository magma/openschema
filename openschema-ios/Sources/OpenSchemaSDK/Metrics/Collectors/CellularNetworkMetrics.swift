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

}
