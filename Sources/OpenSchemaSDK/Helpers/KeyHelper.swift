//
//  KeyHelper.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation

/// This is a class created for handling Key related functions for TLS communication
public class KeyHelper {
    
    init(){}
    
    /**
    This function creates a 2048 bytes RSA public and private key and stores it in keychain. It is used to create the key to create the csr request.
    */
    func generateRSAKeyPairForAlias (alias : String) {
        
        var publicKeyMaybe: SecKey? = nil
        var privateKeyMaybe: SecKey? = nil
        
        let publicKeyAttributes = [
            kSecAttrApplicationTag as String: alias + "Public",
            kSecAttrLabel as String:          alias + "Public"
        ] as CFDictionary
        
        let privateKeyAttributes = [
            kSecAttrApplicationTag as String: alias + "Private",
            kSecAttrLabel as String:          alias + "Private"
        ] as CFDictionary
        
        let err = SecKeyGeneratePair( [
            kSecAttrIsPermanent as String:    kCFBooleanTrue!,
            kSecAttrKeyType as String:        kSecAttrKeyTypeRSA,
            kSecAttrKeySizeInBits as String: 2048,
            kSecPrivateKeyAttrs as String: privateKeyAttributes,
            kSecPublicKeyAttrs as String: publicKeyAttributes
        ] as NSDictionary, &publicKeyMaybe, &privateKeyMaybe)
        assert(err == errSecSuccess)
        
    }
    
    /**
    This function retrieves a key from keychain as Data type. Assumes there exists a key with the attributes specified.
     */
    func getKeyAsData(alias : String, keyType : CFString) -> Data {
        
        var tempKeyRef: CFTypeRef?
        let getDataQuery : [String: Any]  = [   kSecClass as String: kSecClassKey,
                                                kSecAttrApplicationTag as String: alias,
                                                kSecAttrKeyType as String: keyType,
                                                kSecReturnData as String: true]
        
        SecItemCopyMatching(getDataQuery as CFDictionary, &tempKeyRef)
        
        return tempKeyRef as! Data
    }
    
    /**
    This function retrieves a key from keychain as SecKey type. Assumes there exists a key with the attributes specified.
     */
    func getKeyAsSecKey(alias : String, keyType : CFString) -> SecKey {
        var tempKeyRef: CFTypeRef?
        let getDataQuery : [String: Any]  = [   kSecClass as String: kSecClassKey,
                                                kSecAttrApplicationTag as String: alias,
                                                kSecAttrKeyType as String: keyType,
                                                kSecReturnRef as String: true]
        
        SecItemCopyMatching(getDataQuery as CFDictionary, &tempKeyRef)
        return tempKeyRef as! SecKey
    }
    
    /**
    This function deletes a key from keychain. Assumes there exists a key with the attributes specified.
     */
    func DeleteKeyFromKeyChain(alias : String, keyType : CFString) {
        let deleteKeyQuery : [String: Any] = [ kSecClass as String: kSecClassKey,
                            kSecAttrApplicationTag as String: alias,
                            kSecAttrKeyType as String: keyType
        ]
        
        SecItemDelete(deleteKeyQuery as CFDictionary)
    }
    
    /**
    This function deletes a key from keychain. Assumes there exists a key with the attributes specified.
     */
    func getKeyAsBase64String(alias : String, keyType : CFString) -> String {
        var tempKeyRef: CFTypeRef?
        let getDataQuery : [String: Any]  = [   kSecClass as String: kSecClassKey,
                                                kSecAttrApplicationTag as String: alias,
                                                kSecAttrKeyType as String: keyType,
                                                kSecReturnData as String: true]
        
        SecItemCopyMatching(getDataQuery as CFDictionary, &tempKeyRef)
        
        return NSData(data: tempKeyRef as! Data).base64EncodedString()
    }
}
