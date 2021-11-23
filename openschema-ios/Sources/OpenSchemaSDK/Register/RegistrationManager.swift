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

/**
 * DEPRECATED. Might get reused in the future for a more robust device registration into data lake.
 * Class in charge of registering the UE using a generated UUID and Key.
 */
public class RegistrationManager {
    
    private let uuidManager = UUIDManager.shared
    private var serverAddress : String
    private var serverAuthCertPath : String
    
    private var authorityRequiresAuthentication : Bool
    private var requestAuthorityUsername : String
    private var requestAuthorityPassword : String
    
    private let registerRequest : RegisterRequest
    
    public init(serverAddress : String, registerServerAuthCertPath : String, authorityRequiresAuthentication : Bool, requestAuthorityUsername : String, requestAuthorityPassword : String) {
        self.serverAddress = serverAddress
        self.serverAuthCertPath = registerServerAuthCertPath
        self.authorityRequiresAuthentication = authorityRequiresAuthentication
        self.requestAuthorityUsername = requestAuthorityUsername
        self.requestAuthorityPassword = requestAuthorityPassword
        self.registerRequest = RegisterRequest(uuid: uuidManager.getUUID())
        
    }
    
    /**
    This function Registers the device UUID and Public Hardware Key to the server to be able to collect and push analytics to it.
    */
    public func registerDevice() {

        let Url = String(format: self.serverAddress)
        
        guard let serviceUrl = URL(string: Url) else { return }
        
        let parameters: [String: Any] = registerRequest.buildRegisterRequest()
        
        var request = URLRequest(url: serviceUrl)
        request.httpMethod = "POST"
        
        if (self.authorityRequiresAuthentication) {
            let loginString = String(format: "%@:%@", self.requestAuthorityUsername, self.requestAuthorityPassword)
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
        let session = URLSession(configuration: URLSessionConfiguration.ephemeral, delegate: NSURLSessionPinningDelegate(serverAuthCertPath: self.serverAuthCertPath), delegateQueue: nil)
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
