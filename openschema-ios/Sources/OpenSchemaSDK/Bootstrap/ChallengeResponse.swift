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
import SwiftProtobuf
import CryptorECC

///This class creates the response for the challenge received from the server. It handles checking if the challenge result is valid.
public class ChallengeResponse {
    
    ///Raw result of the server it can be either a Magma_Orc8r_Challenge or an Error.
    private var bootstrapChallengeResult : Result<Magma_Orc8r_Challenge, Error>
    ///Result of the Server of type Magma_Orc8r_Challenge.
    private var succesfulResultValue : Magma_Orc8r_Challenge?
    ///Challenge result signed by Hardware Key.
    private var resultKeySignature : ECSignature?
    ///Response for the Server of type Magma_Orc8r_Response.
    private var response : Magma_Orc8r_Response?
    ///Shared UUIDManager class singleton instance.
    private let uuidManager = UUIDManager.shared
    ///KeyHelper class instance.
    private let keyHelper = KeyHelper()
    ///CertSignRequest class instance.
    private let certSignRequest = CertSignRequest()
    ///HardwareKey class instance.
    private let hardwareKey = HardwareKEY()
    
    ///Initiliazes the class calling CreateResponse(). It will fail if result received is an error
    public init(bootstrapChallengeResult : Result<Magma_Orc8r_Challenge, Error>) {
        self.bootstrapChallengeResult = bootstrapChallengeResult
        CreateResponse()
    }
    
    ///This function check that the result received from the server is valid
    private func CheckSuccesfulResult() -> Bool {
        do {
            succesfulResultValue = try self.bootstrapChallengeResult.get()
            print("Challenge Result received from server succesful")
            return true
            
        } catch {

            print("Challenge Result received from server has an error: \(error)")
        }
        
        return false
    }
    
    ///This function check that the challenge can be signed with our private Hardware Key
    private func CheckChallengeKeySignature() -> Bool {
        
        do {
            self.resultKeySignature = try succesfulResultValue?.challenge.sign(with: self.hardwareKey.getHwPrivateKey())
            print("Challenge Result sign succesful")
            return true
            
        } catch {
            
            print("Challenge Result sign unsuccesful: \(error)")
            
        }
        
        return false
    }
    
    ///This creates the ECDSA response component for the response.
    ///Returns: Magma_Orc8r_Response.ECDSA
    private func CreateECDSAResponse() -> Magma_Orc8r_Response.ECDSA {
        
        self.keyHelper.DeleteKeyFromKeyChain(alias: "csrKeyPrivate", keyType: kSecAttrKeyTypeRSA)
        self.keyHelper.DeleteKeyFromKeyChain(alias: "csrKeyPublic", keyType: kSecAttrKeyTypeRSA)
        self.keyHelper.generateRSAKeyPairForAlias(alias: "csrKey")

        let ecdsaResponse = Magma_Orc8r_Response.ECDSA.with {
            $0.r = self.resultKeySignature!.r
            $0.s = self.resultKeySignature!.s
        }

        return ecdsaResponse
    }
    
    ///This creates the CSR component for the response.
    ///Returns: Magma_Orc8r_CSR
    private func CreateCSR() -> Magma_Orc8r_CSR {
        
        print("Private RSA Key: " + self.keyHelper.getKeyAsBase64String(alias: "csrKeyPrivate", keyType: kSecAttrKeyTypeRSA))
        print("Public RSA Key: " + self.keyHelper.getKeyAsBase64String(alias: "csrKeyPublic", keyType: kSecAttrKeyTypeRSA))
        
        print("RSA private Key for CSR: " + NSData(data: self.keyHelper.getKeyAsData(alias : "csrKeyPrivate", keyType: kSecAttrKeyTypeRSA)).base64EncodedString())
        print("csr: " + self.certSignRequest.getCSRString())

        let csrMagma = Magma_Orc8r_CSR.with {
            $0.certType = Magma_Orc8r_CertType(rawValue: 0)!
            $0.id = Magma_Orc8r_Identity.with {
                $0.gateway = Magma_Orc8r_Identity.Gateway.with {
                    $0.hardwareID = self.uuidManager.getUUID()
                }
            }
            $0.validTime = SwiftProtobuf.Google_Protobuf_Duration.init(seconds: 10000, nanos: 10000)
            $0.csrDer = self.certSignRequest.getBuiltCSR()
        }
        
        return csrMagma
    }
    
    ///This function creates the Response for the Challenge received from the server. It won't create if server result is not valid ot if Challenge can't be signed with our Key.
    private func CreateResponse() {
        
        if CheckSuccesfulResult() {
            if CheckChallengeKeySignature() {
                let accessGateWayID = Magma_Orc8r_AccessGatewayID.with {
                    $0.id = uuidManager.getUUID()
                }
                
                let response = Magma_Orc8r_Response.with {
                    $0.hwID = accessGateWayID
                    $0.challenge = succesfulResultValue!.challenge
                    $0.ecdsaResponse = CreateECDSAResponse()
                    $0.csr = CreateCSR()
                }
                
                self.response = response
            }
        }
    }
    
    ///This gets the response generetated from CreateResponse().
    public func getResponse() -> Magma_Orc8r_Response {
        return self.response!
    }
    
}
