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
import NetworkExtension
import SystemConfiguration.CaptiveNetwork

///This class retrieves currently connected Wi-Fi information.
public class WifiNetworkInfo : ObservableObject {
    
    public static let shared = WifiNetworkInfo()
    @Published public private(set) var SSID = "Unable to get value"
    @Published public private(set) var BSSID = "Unable to get value"
    @Published public private(set) var signalStrength = 0.0

    private init(){
        
    }
    
    //TODO: Implement beter version with as this is deprecated for iOS 14 onwards
    ///This function returns current SSID and BSSID info from the connnected Wi-Fi.
    public func getWiFiInfoOld() {
        if let interfaces = CNCopySupportedInterfaces() {
            for i in 0..<CFArrayGetCount(interfaces) {
                let interfaceName: UnsafeRawPointer = CFArrayGetValueAtIndex(interfaces, i)
                let rec = unsafeBitCast(interfaceName, to: AnyObject.self)
                let unsafeInterfaceData = CNCopyCurrentNetworkInfo("\(rec)" as CFString)
                if let interfaceData = unsafeInterfaceData as? [String: AnyObject] {
                    self.SSID = interfaceData[kCNNetworkInfoKeySSID as String] as! String
                    print("fetched ssid: " + (interfaceData[kCNNetworkInfoKeySSID as String] as! String))
                    self.BSSID = interfaceData["BSSID"] as! String
                    //let SSIDDATA = interfaceData["SSIDDATA"] as! String
                }
            }
        }
    }
    
    public func getWiFiInfo() {
        if #available(iOS 14.0, *) {
            NEHotspotNetwork.fetchCurrent(completionHandler: { (network) in
                if let unwrappedNetwork = network {
                    self.SSID = unwrappedNetwork.ssid
                    self.BSSID = unwrappedNetwork.bssid
                    self.signalStrength = unwrappedNetwork.signalStrength
                    
                    print("Retrieved SSID: " + self.SSID)
                    
                } else {
                    print("No available network")
                }
            })
        } else {
            // Fallback on earlier versions
        }
    }
    
    public func getWifiDataUsage() -> UInt64{
        return SystemDataUsage.wifiCompelete
    }
    
    /*///This function returns current SSID and BSSID info from the connnected Wi-Fi.
    private func getWiFiInfo() {
        guard let interfaces = CNCopySupportedInterfaces() as? [String] else {
            print("No Wi-Fi interfaces found!")
            return
        }
        
        for interface in interfaces {
            
            print("Number of Wi-Fi interfaces: \(interfaces.count)")
            
            guard let interfaceInfo = CNCopyCurrentNetworkInfo(interface as CFString) as NSDictionary? else {
                print("Wi-Fi Interface info Failed!")
                return
            }
            guard let ssid = interfaceInfo[kCNNetworkInfoKeySSID as String] as? String else {
                print("Wi-Fi SSID could not be retrieved")
                return
            }
            guard let bssid = interfaceInfo[kCNNetworkInfoKeyBSSID as String] as? String else {
                print("Wi-Fi BSSID could not be retrieved")
                return
            }
            
            self.SSID = ssid
            self.BSSID = bssid
            break
        }
    }*/
    
    ///Calls fetchSSIDInfo used to update values on Wi-Fi change. For example to refresh UI.
    public func updateWifiNetworkInfo() -> Void {
        self.getWiFiInfo()
    }
    
}
