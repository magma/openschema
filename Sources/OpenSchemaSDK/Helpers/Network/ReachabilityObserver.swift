//
//  ReachabilityObserver.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation
import Reachability

///This Class Handles the observer that listens to network connectivity changes. Possible Modes: Wi-Fi connected, Cellular Connected, No Connection.
public class ReachabilityObserver {
    
    public static let shared = ReachabilityObserver()
    private let reachability = try! Reachability()
    private var newSSID : String = "No Wi-Fi Connected"
    private var currentSSID : String = "No Wi-Fi Connected"
    
    private init() {
        NotificationCenter.default.addObserver(self, selector: #selector(reachabilityChanged(note:)), name: .reachabilityChanged, object: reachability)
            do{
              try reachability.startNotifier()
            }catch{
              print("could not start reachability notifier")
            }
        
        
    }
    
    @objc private func reachabilityChanged(note: Notification) {
        let reachability = note.object as! Reachability
        
        if reachability.connection == .wifi {
                print("Wi-Fi Connection is reachable")
                
            if self.newSSID != self.currentSSID {
                print("New SSID detected")
                self.currentSSID = self.newSSID
            }
            
        } else if reachability.connection == .cellular{
            print("Cellular Connection is reachable")
        }
        
        else if reachability.connection == .unavailable {
            print("No Connection is reachable")
        }
    }
    
    deinit {
        reachability.stopNotifier()
    }
    
}
