//
//  UUIDManager.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation

///This class handles generating, storing and retrieving the UUID.
public class UUIDManager {
    
    static let shared = UUIDManager()
    private let KEY_UUID = "uuid"
    private var uuid : String = "UNKNOWN_UUID"
    
    private init() {
        GenerateUUID()
    }
    
    /**
    This function checks if there is a UUID already stored for the installed app.
    */
    
    private func UUIDexists(key: String) -> Bool {
        return UserDefaults.standard.object(forKey: key) != nil
    }
    
    
    /**
    This function generates if there is no UUID already stored for the installed app. Calls func UUIDexists.
    */
    private func GenerateUUID() {
        if UUIDexists(key: "uuid"){
            self.uuid = UserDefaults.standard.string(forKey: self.KEY_UUID) ?? "Error in UUID"
        } else {
            self.uuid = UUID().uuidString
            UserDefaults.standard.set(self.uuid , forKey: self.KEY_UUID)
        }
    }
    
    /**
    This function retrieves the stored UUID.
    */
    
    func getUUID() -> String { return uuid }
    
    /**
    This function overrides current stored UUID. Currently unused function. Might be useful if want to register device to magma with a new UUID without uninstalling app from device.
    */
    func setUUID(uuid : String) { self.uuid = uuid }
        
}
