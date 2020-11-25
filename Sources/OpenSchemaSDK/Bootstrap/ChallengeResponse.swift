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
    
    /**
    This function return the built CSR to as a String to be able to print is information on console.
    */
    func getCSRString() -> String {
        let publicKeyBits = self.keyHelper.getKeyAsData(alias : "csrKeyPublic", keyType: kSecAttrKeyTypeRSA)
        let privateKey = self.keyHelper.getKeyAsSecKey(alias : "csrKeyPrivate", keyType: kSecAttrKeyTypeRSA)

        return csr!.buildCSRAndReturnString(publicKeyBits, privateKey: privateKey)!
    }
    
}
