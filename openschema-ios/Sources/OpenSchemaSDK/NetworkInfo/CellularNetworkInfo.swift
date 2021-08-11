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
import CoreTelephony

///This class retrieves currently connected Cellular Network Information.
public class CellularNetworkInfo {
    
    private let networkInfo = CTTelephonyNetworkInfo()
    
    private var firstCarrier = "Unknown"
    private var firstMobileNetworkCode = "Unknown"
    private var firstMobileCountryCode = "Unknown"
    private var firstIsoCountryCode = "Unknown"
    
    public init(){
      getFirstCarrierInfo()
    }
    
    public func getCarrierName() ->  String { return firstCarrier }
    public func getMobileCountryCode() -> String { return firstMobileCountryCode }
    public func getMobileNetworkCode() -> String { return firstMobileNetworkCode }
    public func getIsoCountryCode() -> String { return firstIsoCountryCode }
    
    ///This function gets Carrier, radio and Service Identifier Information from device.
    public func Info() -> Void {

        let carrierInfo = networkInfo.serviceSubscriberCellularProviders
        dump(carrierInfo)
        
        let radioInfo = networkInfo.serviceCurrentRadioAccessTechnology
        dump(radioInfo)
        
        let serviceIdentifier = String(networkInfo.dataServiceIdentifier ?? "Unknown service Identifier")
        print(serviceIdentifier)
    }

    ///This function gets current connected carrier if none available returns "Unkonwn".
    public func getFirstCarrierInfo() -> Void {
        
        let serviceSubscriberCellularProviders = networkInfo.serviceSubscriberCellularProviders
        let firstCarrier = serviceSubscriberCellularProviders?.values.first
        
        self.firstCarrier = firstCarrier?.carrierName ?? "Unknown"
        self.firstMobileNetworkCode = firstCarrier?.mobileNetworkCode ?? "Unknown"
        self.firstMobileCountryCode = firstCarrier?.mobileCountryCode ?? "Unknown"
        self.firstIsoCountryCode = firstCarrier?.isoCountryCode ?? "Unknown"

    }
    
    ///This function gets current conneted Radio Access Rechnology.
    public func getCurrentRadioAccessTechnology() -> String {

        let currCarrierType: String?

        // get curr value:
        guard let dict = networkInfo.serviceCurrentRadioAccessTechnology else{
            return "Unknown"
        }
        
        if dict.isEmpty {
            return "No Service"
        }
        // as apple states
        // https://developer.apple.com/documentation/coretelephony/cttelephonynetworkinfo/3024510-servicecurrentradioaccesstechnol
        // 1st value is our string:
        let key = dict.keys.first! // Apple assures is present...
        // use it on previous dict:
        let carrierType = dict[key]
        currCarrierType = carrierType

        switch currCarrierType{
        case CTRadioAccessTechnologyGPRS:
            return "2G" + " (GPRS)"

        case CTRadioAccessTechnologyEdge:
            return "2G" + " (Edge)"

        case CTRadioAccessTechnologyCDMA1x:
            return "2G" + " (CDMA1x)"

        case CTRadioAccessTechnologyWCDMA:
            return "3G" + " (WCDMA)"

        case CTRadioAccessTechnologyHSDPA:
            return "3G" + " (HSDPA)"

        case CTRadioAccessTechnologyHSUPA:
            return "3G" + " (HSUPA)"

        case CTRadioAccessTechnologyCDMAEVDORev0:
            return "3G" + " (CDMAEVDORev0)"

        case CTRadioAccessTechnologyCDMAEVDORevA:
            return "3G" + " (CDMAEVDORevA)"

        case CTRadioAccessTechnologyCDMAEVDORevB:
            return "3G" + " (CDMAEVDORevB)"

        case CTRadioAccessTechnologyeHRPD:
            return "3G" + " (eHRPD)"

        case CTRadioAccessTechnologyLTE:
            return "4G" + " (LTE)"

        default:
            break;
        }

        return "newer type!"
    }
    
}
