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
import GRPC
import NIO
import NIOSSL
import Logging

/// This class handles the bootstrap process to create a GRPC connection to Magma server and get a Signed certificate from it to be able to strat pushing metrics to Magma.
public class BootstrapManager {
    
    ///Shared clientConfig class singleton instance.
    private let clientConfig = ClientConfig.shared
    ///Shared UUIDManager class singleton instance.
    private let uuidManager = UUIDManager.shared
    ///String that contains the path to the certificate to be used for connecting to the server on Bootstrap.
    private var certificateFilePath : String
    
    ///Initialize Bootstrap Class, it requires the path to the server certificate for Bootstrap.
    public init(certificateFilePath : String) {
        self.certificateFilePath = certificateFilePath
        print(self.uuidManager.getUUID())
    }

    ///This function calls BootstrapLogic and sends it to a background thread to prevent locking the UI during its execution.
    public func BootstrapNow(){
        let dispatchQueue = DispatchQueue(label: "QueueIdentification", qos: .background)
        dispatchQueue.async{
            self.BootstrapLogic()
        }
    }
    
    ///This creates a GRPC channel and tries to connect to the specified server using the certificate provided on class init. If connection is succesful a connection is created and MetricsManager CollectAndPushMetrics function is called.
    private func BootstrapLogic() {

        do {
            let pemCert = try NIOSSLCertificate.fromPEMFile(certificateFilePath)
            
            //Step ii: Create an event loop group
            let group = MultiThreadedEventLoopGroup(numberOfThreads: 1)
            
            // Setup a logger for debugging.
            var logger = Logger(label: "gRPC", factory: StreamLogHandler.standardOutput(label:))
            logger.logLevel = .debug
            
            //Step iii: Create client connection builder
            let builder: ClientConnection.Builder
            builder = ClientConnection.secure(group: group).withTLS(trustRoots: .certificates(pemCert)).withBackgroundActivityLogger(logger)
            
            //Step iv: Start the connection and create the client
            let connection = builder.connect(host: self.clientConfig.getBootstrapControllerAddress(), port: self.clientConfig.getControllerPort())
            print("Bootstrapper Connection Status=>: \(connection)")
            
            //Step v: Create client
            //use appropriate service client from .grpc server to replace the xxx call : <your .grpc.swift ServiceClient> = <XXX>ServiceClient
            let client: Magma_Orc8r_BootstrapperClient = Magma_Orc8r_BootstrapperClient(channel: connection)
            
            // Make the RPC call to the server.
            let accessGateWayID = Magma_Orc8r_AccessGatewayID.with {
                $0.id = uuidManager.getUUID()
            }
            
            let challenge = client.getChallenge(accessGateWayID)

            challenge.response.whenComplete { result in
                
                print("Output for get request: \(result)")
                
                let response = ChallengeResponse(bootstrapChallengeResult: result)
                let challengeResponse = client.requestSign(response.getResponse())
                    
                challengeResponse.response.whenComplete { result in
                        
                    print("Output for Challenge Response request: \(result)")
                        
                    do {
                        let signedCertData = try result.get().certDer
                            
                        let metricsManager = MetricsManager(signedCert: signedCertData, certificateFilePath: self.certificateFilePath)
                        metricsManager.CollectAndPushMetrics()
   
                    } catch {
                        print("Error retrieving Signed Cert Data: \(error)")
                    }
                }
            }
                    
            challenge.response.whenFailure { error in
                print("Output for failed request: \(error)")
            }
            
            do {
                let detailsStatus = try challenge.status.wait()
                print("Status:::\(detailsStatus) \n \(detailsStatus.code))")
            } catch {
                print("Error for get Request:\(error)")
            }
            
        } catch {
            print("Error getting certificate: \(error)")
        }
    }
}
