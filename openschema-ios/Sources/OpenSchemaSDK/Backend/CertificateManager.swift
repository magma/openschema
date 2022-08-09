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

///Class in charge of handling all the custom self-signed certificates used in the metrics agent flow. This class pins the server certificate to verify the server origin.
public class NSURLSessionPinningDelegate : NSObject, URLSessionDelegate {
    
    private var serverAuthCertPath : String
    
    public init(serverAuthCertPath : String) {
        self.serverAuthCertPath = serverAuthCertPath
    }
    
    ///This extends the urlSession function to verify server identity
    public func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {

        print("*** received SESSION challenge...\(challenge)")
        let trust = challenge.protectionSpace.serverTrust!
        let credential = URLCredential(trust: trust)
        
        var remoteCertMatchesPinnedCert = false
        
        if let pinnedCertData = NSData(contentsOfFile: self.serverAuthCertPath) {
            
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
