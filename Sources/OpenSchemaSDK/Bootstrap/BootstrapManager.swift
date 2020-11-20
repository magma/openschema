//
//  BootstrapManager.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation
import GRPC
import NIO
import NIOSSL
import SwiftProtobuf
import Logging

/// This class handles the bootstrap process to create a GRPC connection to Magma server and get a Signed certificate from it to be able to strat pushing metrics to Magma.
public class BootstrapManager {
    
    private let clientConfig = ClientConfig.shared
    private let uuidManager = UUIDManager.shared
    private let wifiNetworkinfo = WifiNetworkInfo.shared
    let keyHelper = KeyHelper()
    let certSignRequest = CertSignRequest()

    public init() {
        print(self.uuidManager.getUUID())
        CreateSSIDObserver()
        //BootstrapNow()
    }
    
    private func CreateSSIDObserver() {
        let observer : UnsafeRawPointer! = UnsafeRawPointer(Unmanaged.passUnretained(self).toOpaque())
        let object : UnsafeRawPointer! = nil
        
        let callback: CFNotificationCallback = { center, observer, name, object, info in
            print("Wi-Fi SSID name changed")
            
            let mySelf = Unmanaged<BootstrapManager>.fromOpaque(UnsafeRawPointer(observer!)).takeUnretainedValue()
            // Call instance method:
            mySelf.wifiNetworkinfo.fetchSSIDInfo()
            mySelf.BootstrapNow()

        }

        CFNotificationCenterAddObserver(CFNotificationCenterGetDarwinNotifyCenter(),
                                        observer,
                                        callback,
                                        "com.apple.system.config.network_change" as CFString,
                                        object,
                                        .deliverImmediately)
    }
    
    
    public func BootstrapNow(){
        let dispatchQueue = DispatchQueue(label: "QueueIdentification", qos: .background)
        dispatchQueue.async{
            self.BootstrapLogic()
        }
    }
    
    private func BootstrapLogic() {
        //Step i: get certificate; Tutorial at: https://medium.com/@ambrose12silveira/ios-swift-grpc-integration-with-tls-client-authentication-f2e2164ed125
        let certificateFileName = "rootca"
        
        let certificateFilePath = Bundle.main.path(forResource: certificateFileName, ofType: "pem")
 
        do {
            let pemCert = try NIOSSLCertificate.fromPEMFile(certificateFilePath!)
            
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
            
            //Step vi: Call specific service request
            let accessGateWayID = Magma_Orc8r_AccessGatewayID.with {
                $0.id = uuidManager.getUUID()
            }

            // Make the RPC call to the server.
            let hardwareKey = HardwareKEY()
            print("Private ECDSA Key: " + hardwareKey.getHwPrivateKeyPEMString())
            print("Public ECDSA Key: " + hardwareKey.getHwPublicKeyPEMString())
            
            let challenge = client.getChallenge(accessGateWayID)

            challenge.response.whenComplete { result in
                
                print("Output for get request: \(result)")
                
                do {
                    
                    let challengeResult = try result.get()
                    
                    let signature = try challengeResult.challenge.sign(with: hardwareKey.getHwPrivateKey())
                    
                    let ecdsaResponse = Magma_Orc8r_Response.ECDSA.with {
                        $0.r = signature.r
                        $0.s = signature.s
                    }

                    self.keyHelper.DeleteKeyFromKeyChain(alias: "csrKeyPrivate", keyType: kSecAttrKeyTypeRSA)
                    self.keyHelper.DeleteKeyFromKeyChain(alias: "csrKeyPublic", keyType: kSecAttrKeyTypeRSA)
                    self.keyHelper.generateRSAKeyPairForAlias(alias: "csrKey")
                    
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
                    
                    let response = Magma_Orc8r_Response.with {
                        $0.hwID = accessGateWayID
                        $0.challenge = challengeResult.challenge
                        $0.ecdsaResponse = ecdsaResponse
                        $0.csr = csrMagma
                    }
                    
                    let challengeResponse = client.requestSign(response)
                    
                    challengeResponse.response.whenComplete { result in
                        
                        print("Output for Challenge Response request: \(result)")
                        
                        do {
                            let signedCertData = try result.get().certDer
                            
                            let metricsManager = MetricsManager(signedCert: signedCertData)
                            metricsManager.CollectAndPushMetrics()
                            
                            
                        } catch {

                            print("Error retrieving Signed Cert Data: \(error)")
                        }
                        

                    }
                    
                    challengeResponse.response.whenFailure { error in
                        print("Output for Challenge Response failed request: \(error)")
                    }
                    
                } catch {
                    print("Error for get Request:\(error)")
                }
    
            }
            
            challenge.response.whenFailure { error in
                print("Output for failed request: \(error)")
            }
            
            do {
                let detailsStatus = try challenge.status.wait()
                print("Staus:::\(detailsStatus) \n \(detailsStatus.code))")
            } catch {
                print("Error for get Request:\(error)")
            }
            
        } catch {
            print("Error getting rootca certificate: \(error)")
        }
    }
}
