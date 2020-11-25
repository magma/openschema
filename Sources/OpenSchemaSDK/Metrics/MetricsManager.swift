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
import SwiftProtobuf
import NIOHTTP1
import NIOHTTP2
import NIOHPACK
import Logging
import Dispatch

extension Date {
    var millisecondsSince1970:Int64 {
        return Int64((self.timeIntervalSince1970 * 1000.0).rounded())
    }

    init(milliseconds:Int64) {
        self = Date(timeIntervalSince1970: TimeInterval(milliseconds) / 1000)
    }
}

///This class handles the GRPC connection to magma to collect and push metrics.
public class MetricsManager {
    
    private let signedCert : Data
    private let clientConfig = ClientConfig.shared
    private let uuidManager = UUIDManager.shared
    private let keyHelper = KeyHelper()
    private let cellularNetworkMetrics : CellularNetworkMetrics = CellularNetworkMetrics()
    private let wifiNetworkMetrics : WifiNetworkMetrics = WifiNetworkMetrics()
    private var certificateFilePath : String

    init(signedCert : Data , certificateFilePath : String) {
        
        self.signedCert = signedCert
        self.certificateFilePath = certificateFilePath
        
    }
    
    public func CheckCertValidity() {
        //TODO: Implement a way to check Bootstrap certificate is still valid
    }
    
    public func CollectAndPushMetrics() {
        
        do {
            
            //Step i: get certificates
            let pemCert = try NIOSSLCertificate.fromPEMFile(certificateFilePath) // this is ROOTCA
            print("Signed Cert: " + NSData(data: signedCert).base64EncodedString())
            
            let serverSignedCert = try NIOSSLCertificate(bytes: [UInt8](signedCert), format: .der ) // This is Signed Cert
     
            //Get RSA Private Key
            let privateKeyData = keyHelper.getKeyAsData(alias: "csrKeyPrivate", keyType: kSecAttrKeyTypeRSA)
            print("RSA private Key for TLS: " + NSData(data: privateKeyData).base64EncodedString())
            let privatekeyBytes = [UInt8](privateKeyData)
            let privKey = try NIOSSLPrivateKey(bytes: privatekeyBytes, format: .der)// This is RSA Key that signed CSR
                
            //Step ii: Create an event loop group
            let group = MultiThreadedEventLoopGroup(numberOfThreads: 1)
            
            // Setup a logger for debugging.
            var logger = Logger(label: "gRPC", factory: StreamLogHandler.standardOutput(label:))
            logger.logLevel = .debug

            var configTLS = TLSConfiguration.forClient()
            configTLS.trustRoots = .certificates(pemCert)
            configTLS.certificateChain = [.certificate(serverSignedCert)]
            configTLS.privateKey = .privateKey(privKey)
            configTLS.minimumTLSVersion = TLSVersion.tlsv12
            configTLS.maximumTLSVersion = TLSVersion.tlsv12
            configTLS.certificateVerification = .fullVerification
            configTLS.applicationProtocols = ["grpc-exp", "h2"]
            configTLS.signingSignatureAlgorithms = [SignatureAlgorithm.rsaPkcs1Sha256]
            configTLS.verifySignatureAlgorithms = [SignatureAlgorithm.rsaPkcs1Sha256]

            let config = ClientConnection.Configuration(target: ConnectionTarget.hostAndPort(self.clientConfig.getControllerAddress(), self.clientConfig.getControllerPort()), eventLoopGroup: group, errorDelegate: LoggingClientErrorDelegate(), connectivityStateDelegate: nil, connectivityStateDelegateQueue: nil, tls: ClientConnection.Configuration.TLS.init(configuration: configTLS, hostnameOverride: self.clientConfig.getMetricAuthorityHeader()), connectionBackoff: ConnectionBackoff(), connectionKeepalive: ClientConnectionKeepalive(), connectionIdleTimeout: .minutes(30), callStartBehavior: .waitsForConnectivity, httpTargetWindowSize: 65535, backgroundActivityLogger: logger, debugChannelInitializer: nil)
            
            let connection = ClientConnection.init(configuration: config)
            //connection.authority = clientConfig.getMetricAuthorityHeader()
            print("Metricsd Connection Status=>: \(connection)")

            //Step v: Create client
            let client: Magma_Orc8r_MetricsControllerClient = Magma_Orc8r_MetricsControllerClient(channel: connection)
            
            // Make the RPC call to the server.
            let collect = client.collect(cellularNetworkMetrics.CollectCellularNetworkInfoMetrics())
            print("Succesfully called Collect")
            
            collect.response.whenComplete { result in
                print("Output for Collect Response request: \(result)")
                
            }
            
            collect.response.whenFailure { error in
                print("Output for Collect Response failed request: \(error)")
            }

        } catch {
            print("Error Creating NIOSSLCertificate from signed Cert: \(error)")
        }
    
    }
    
}
