//
//  ChallengeResponse.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation
import CertificateSigningRequest

///This class handles  creating the CSR for bootstrap class. It uses https://github.com/cbaker6/CertificateSigningRequest to create the properly format of the CSR.
public class CertSignRequest {

    private let keyAlgorithm = KeyAlgorithm.rsa(signatureType: .sha256)
    private let uuidManager = UUIDManager.shared
    private let keyHelper = KeyHelper()
    private var csr : CertificateSigningRequest? = nil
    
    /**
    Initialize
    */
    init(){
        self.csr = CertificateSigningRequest(commonName: self.uuidManager.getUUID(), organizationName: nil, organizationUnitName: nil, countryName: nil, stateOrProvinceName: nil, localityName: nil, keyAlgorithm: keyAlgorithm)
    }
    
    /**
    This function return the built CSR to send on bootstrap manager to magma.
    */
    func getBuiltCSR() -> Data {
        let publicKeyBits = self.keyHelper.getKeyAsData(alias : "csrKeyPublic", keyType: kSecAttrKeyTypeRSA)
        let privateKey = self.keyHelper.getKeyAsSecKey(alias : "csrKeyPrivate", keyType: kSecAttrKeyTypeRSA)
        return csr!.build(publicKeyBits, privateKey: privateKey)!
        
    }
    
    func getCSRString() -> String {
        let publicKeyBits = self.keyHelper.getKeyAsData(alias : "csrKeyPublic", keyType: kSecAttrKeyTypeRSA)
        let privateKey = self.keyHelper.getKeyAsSecKey(alias : "csrKeyPrivate", keyType: kSecAttrKeyTypeRSA)

        return csr!.buildCSRAndReturnString(publicKeyBits, privateKey: privateKey)!
    }
    
}
