//
//  HardwareKey.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation
import CryptorECC

///This class creates hardware key from device and has functions to store it and retrieve it.
public class HardwareKEY {
    
    private let keyHelper : KeyHelper = KeyHelper()
    private var eccPrivateKey : ECPrivateKey? = nil
    private var eccPublicKey : ECPublicKey? = nil
    
    init() {
        self.GenerateECKeyPairForAlias()
    }
    
    /**
    This function checks if there is a Hardware Key stored.
    */
    
    private func HardwareKeyExists(key: String) -> Bool {
        return UserDefaults.standard.object(forKey: key) != nil
    }
    
    
    /**
    This function generates Private EC Key and stores it as user default
    */
    private func GenerateECPrivateKey() -> String {
        if HardwareKeyExists(key: "HardwareKey"){
            return  UserDefaults.standard.string(forKey: "HardwareKey") ?? "Error in UUID"
        } else {
            
            do {
                let p256PrivateKey = try ECPrivateKey.make(for: .prime256v1)
                let privateKeyPEM = p256PrivateKey.pemString
                print("First Use Generated Private ECDSA: ")
                print(privateKeyPEM)
                UserDefaults.standard.set(privateKeyPEM , forKey: "HardwareKey")
                let testEccPublicKey = try p256PrivateKey.extractPublicKey()
                print("First Use Generated Public ECDSA: ")
                print(testEccPublicKey.pemString)
                
                return privateKeyPEM
                
            } catch {
                print("Error Creating ECDSA key for Hardware key: \(error)")
            }
            
            return "ERROR"
        }
    }

    
    /**
     This function creates private and public ECDSA key for bootstrap challenge.
     TODO:
        - Store Using Keychain
     */
    private func GenerateECKeyPairForAlias() {
        
        do {
            eccPrivateKey = try ECPrivateKey(key: self.GenerateECPrivateKey())
        } catch {
            print("Error Creating Hardware Private Key: \(error)")
        }
        
        do {
            eccPublicKey = try eccPrivateKey!.extractPublicKey()
        } catch {
            print("Error Creating Hardware Public Key: \(error)")
        }

    }
    
    /**
    This function retrieves Hardware Private key as a PEM String.
     */
    func getHwPrivateKeyPEMString() -> String {
        return self.eccPrivateKey!.pemString
    }
    
    /**
    This function retrieves Hardware Public key as a PEM String.
     */
    func getHwPublicKeyPEMString() -> String {
        return self.eccPublicKey!.pemString
    }
    
    /**
    This function retrieves Hardware Private key as a ECPrivateKey Class
     */
    func getHwPrivateKey() -> ECPrivateKey {
        return self.eccPrivateKey!
    }
    
    /**
    This function retrieves Hardware Public key as a ECPrivateKey Class
     */
    func getHWPublicKey() -> ECPublicKey {
        return self.eccPublicKey!
    }
}
