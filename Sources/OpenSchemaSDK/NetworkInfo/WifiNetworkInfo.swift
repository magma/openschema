//
//  WifiNetworkInfo.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation
import NetworkExtension
import SystemConfiguration.CaptiveNetwork

///This class retrieves currently connected Wi-Fi information.
public class WifiNetworkInfo {
    
    public static let shared = WifiNetworkInfo()
    private var SSID = "Unable to get value"
    private var BSSID = "Unable to get value"
    
    private init(){
      fetchSSIDInfo()
    }
    
    public func getSSID() -> String { return self.SSID }
    public func getBSSID() -> String { return self.BSSID }
    
    public func fetchSSIDInfo() -> Void {
        if let interfaces = CNCopySupportedInterfaces() {
            for i in 0..<CFArrayGetCount(interfaces) {
                let interfaceName: UnsafeRawPointer = CFArrayGetValueAtIndex(interfaces, i)
                let rec = unsafeBitCast(interfaceName, to: AnyObject.self)
                let unsafeInterfaceData = CNCopyCurrentNetworkInfo("\(rec)" as CFString)
                if let interfaceData = unsafeInterfaceData as? [String: AnyObject] {
                    self.SSID = interfaceData["SSID"] as! String
                    self.BSSID = interfaceData["BSSID"] as! String
                    //let SSIDDATA = interfaceData["SSIDDATA"] as! String
                }
            }
        }
    }
    
    public func updateWifiNetworkInfo() -> Void {
        self.fetchSSIDInfo()
    }
    
}
