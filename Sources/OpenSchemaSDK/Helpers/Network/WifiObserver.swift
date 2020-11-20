//
//  WifiObserver.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation

///This Class Handles the observer that listens SSID being Changed.
public class WifiObserver {
    
    static let shared = WifiObserver()
    private let wifiNetworkInfo = WifiNetworkInfo.shared
    private var SSIDChanged : Bool = false

    private init() {
        
        self.CreateSSIDObserver()
        
    }

    private func CreateSSIDObserver() {
        let observer : UnsafeRawPointer! = UnsafeRawPointer(Unmanaged.passUnretained(self).toOpaque())
        let object : UnsafeRawPointer! = nil
        
        let callback: CFNotificationCallback = { center, observer, name, object, info in
            print("Wi-Fi SSID name changed")
            
            let mySelf = Unmanaged<WifiObserver>.fromOpaque(UnsafeRawPointer(observer!)).takeUnretainedValue()
            // Call instance method:
            mySelf.SSIDChanged = true

        }

        CFNotificationCenterAddObserver(CFNotificationCenterGetDarwinNotifyCenter(),
                                        observer,
                                        callback,
                                        "com.apple.system.config.network_change" as CFString,
                                        object,
                                        .deliverImmediately)
    }
    
}
