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

///This class Registers the device to the server.
public class RegistrationManager {
    
    private let hardwareKey = HardwareKEY()
    private let uuidManager = UUIDManager.shared
    private var registerServerAddress : String
    private var registerServerAuthCertPath : String
    
    private var authorityRequiresAuthentication : Bool
    private var requestAuthorityUsername : String
    private var requestAuthorityPassword : String
    
    public init(registerServerAddress : String, registerServerAuthCertPath : String, authorityRequiresAuthentication : Bool, requestAuthorityUsername : String, requestAuthorityPassword : String) {
        self.registerServerAddress = registerServerAddress
        self.registerServerAuthCertPath = registerServerAuthCertPath
        self.authorityRequiresAuthentication = authorityRequiresAuthentication
        self.requestAuthorityUsername = requestAuthorityUsername
        self.requestAuthorityPassword = requestAuthorityPassword
        
        self.registerDevice()
    }
    
    /**
    Remove Header, Footer and spaces to just send the key to Registration Server.
    */
    private func trimPublicKeyPEMString() -> String {
        
        var publicKeyString : String = hardwareKey.getHwPublicKeyPEMString()
        
        print("Before Trim: \n" + publicKeyString)
        
        var range = publicKeyString.index(publicKeyString.endIndex, offsetBy: -25)..<publicKeyString.endIndex
        publicKeyString.removeSubrange(range)

        range = publicKeyString.index(publicKeyString.startIndex, offsetBy: 0)..<publicKeyString.index(publicKeyString.startIndex, offsetBy: 27)
        publicKeyString.removeSubrange(range)

        publicKeyString = publicKeyString.trimmingCharacters(in: .whitespacesAndNewlines)
        
        
        print("Before Trim: \n" + publicKeyString)
        
        return publicKeyString
    }
    
    /**
    This function Registers the device UUID and Public Hardware Key to the server to be able to collect and push analytics to it.
    */
    public func registerDevice() {

        let Url = String(format: self.registerServerAddress)
        
        guard let serviceUrl = URL(string: Url) else { return }
        let parameters: [String: Any] = [
            "uuid" : self.uuidManager.getUUID(),
            "publicKey": self.trimPublicKeyPEMString()
        ]

        var request = URLRequest(url: serviceUrl)
        request.httpMethod = "POST"
        
        if (self.authorityRequiresAuthentication) {
            let loginString = String(format: "%@:%@", "Dw5QIUx0Ric2yLo7IogrfHZGiJ6s3gC+FQaekCw3pY8/FlG+9g+Xp8Fo1fuADpRhBmQNsyeuhmjbq+A4QEz1VLeyHFHkNilhQE9NsUArSC1UjyiO/CY01vxVaIcydOSW", "Dw5QIUx0Ric2yLo7IogrfHZGiJ6s3gC+FQaekCw3pY8/FlG+9g+Xp8Fo1fuADpRhBmQNsyeuhmjbq+A4QEz1VLeyHFHkNilhQE9NsUArSC1UjyiO/CY01vxVaIcydOSW")
            let loginData = loginString.data(using: String.Encoding.utf8)!
            let base64LoginString = loginData.base64EncodedString()
            request.setValue("Basic \(base64LoginString)", forHTTPHeaderField: "Authorization")
        }
        
        request.setValue("Application/json", forHTTPHeaderField: "Content-Type")
        guard let httpBody = try? JSONSerialization.data(withJSONObject: parameters, options: []) else {
            return
        }
        
        request.httpBody = httpBody
        request.timeoutInterval = 20
        let session = URLSession(configuration: URLSessionConfiguration.ephemeral, delegate: NSURLSessionPinningDelegate(registerServerAuthCertPath: self.registerServerAuthCertPath), delegateQueue: nil)
        session.dataTask(with: request) { (data, response, error) in
            if let response = response {
                print(response)
            }
            if let data = data {
                do {
                    let json = try JSONSerialization.jsonObject(with: data, options: [])
                    print(json)
                } catch {
                    print(error)
                }
            }
        }.resume()
    }
    
}

///This class pins the server certificate to verify the server origin.
class NSURLSessionPinningDelegate : NSObject, URLSessionDelegate {
    
    private var registerServerAuthCertPath : String
    
    init(registerServerAuthCertPath : String) {
        self.registerServerAuthCertPath = registerServerAuthCertPath
    }
    
    /**
    This extends the urlSession function to verify server identity
    */
    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {

        print("*** received SESSION challenge...\(challenge)")
        let trust = challenge.protectionSpace.serverTrust!
        let credential = URLCredential(trust: trust)
        
        var remoteCertMatchesPinnedCert = false
        
        if let pinnedCertData = NSData(contentsOfFile: self.registerServerAuthCertPath) {
            
            // Compare certificate data
            let remoteCertData: NSData = SecCertificateCopyData(SecTrustGetCertificateAtIndex(trust, 0)!)
            if remoteCertData.isEqual(to: pinnedCertData as Data) {
                print("*** CERTIFICATE DATA MATCHES")
                remoteCertMatchesPinnedCert = true
                
            } else {
                print("*** MISMATCH IN CERT DATA.... :(")
            }
            
        } else {
            print("*** Couldn't read pinning certificate data")
        }

        if remoteCertMatchesPinnedCert {
            print("*** TRUSTING CERTIFICATE")
            completionHandler(.useCredential, credential)
        } else {
            print("NOT TRUSTING CERTIFICATE")
            completionHandler(.rejectProtectionSpace, nil)
        }
    }
}
