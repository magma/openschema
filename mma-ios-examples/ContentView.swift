//
//  ContentView.swift
//  mma-ios-examples
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import SwiftUI
import mma_ios

struct ContentView: View {
    
    @ObservedObject var locationManager = LocationManager()
    var wifiNetworkInfo = WifiNetworkInfo.shared
    var cellularNetworkInfo = CellularNetworkInfo()
    private let uuidManager = UUIDManager.shared
    private let bootstrapManager = BootstrapManager()
    private let hardwareKey = HardwareKEY()
    private let reachabilityObserver = ReachabilityObserver.shared
    
    func ShareRegisteInformation() {
        let bootstrapInfo = "Registration items for Bootstrap\n"
        let uuidTittle = "This is your UUID:\n"
        let uuid = uuidManager.getUUID()
        let publicHwkeyTittle = "\nThis is your Public Hardware Key: \n"
        let publicHwKey = hardwareKey.getHWPublicKey().pemString
        let av = UIActivityViewController(activityItems: [bootstrapInfo, uuidTittle, uuid, publicHwkeyTittle, publicHwKey], applicationActivities: nil)
        UIApplication.shared.windows.first?.rootViewController?.present(av, animated: true, completion: nil)
    }
    
    var body: some View {
        
        NavigationView {

            List {
                import Foundation

                ///This class Registers the device to the server.
                public class Register {
                    
                    private let hardwareKey = HardwareKEY()
                    private let uuidManager = UUIDManager.shared
                    private let registerServerAddress : String = "https://13.52.214.86:3100/register"
                    
                    init() {
                    }
                    
                    /**
                    Remove Header, Footer and spaces to just send the key to Registration Server.
                    */
                    private func trimPublicKeyPEMString() -> String {
                        
                        var publicKeyString : String = hardwareKey.getHwPublicKeyPEMString()
                        var range = publicKeyString.index(publicKeyString.endIndex, offsetBy: -25)..<publicKeyString.endIndex
                        publicKeyString.removeSubrange(range)

                        range = publicKeyString.index(publicKeyString.startIndex, offsetBy: 0)..<publicKeyString.index(publicKeyString.startIndex, offsetBy: 27)
                        publicKeyString.removeSubrange(range)

                        publicKeyString = publicKeyString.trimmingCharacters(in: .whitespacesAndNewlines)
                        
                        print(publicKeyString)
                        
                        return publicKeyString
                    }
                    
                    /**
                    This function Registers the device UUID and Public Hardware Key to the server to be able to collect and push analytics to it.
                    */
                    func registerDevice() {

                        let Url = String(format: self.registerServerAddress)
                            guard let serviceUrl = URL(string: Url) else { return }
                            let parameters: [String: Any] = [
                                "uuid" : uuidManager.getUUID(),
                                "publicKey": self.trimPublicKeyPEMString
                            ]
                            var request = URLRequest(url: serviceUrl)
                            request.httpMethod = "POST"
                            request.setValue("Application/json", forHTTPHeaderField: "Content-Type")
                            guard let httpBody = try? JSONSerialization.data(withJSONObject: parameters, options: []) else {
                                return
                            }
                            request.httpBody = httpBody
                            request.timeoutInterval = 20
                            let session = URLSession(configuration: URLSessionConfiguration.ephemeral, delegate: NSURLSessionPinningDelegate(), delegateQueue: nil)
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
                class NSURLSessionPinningDelegate: NSObject, URLSessionDelegate {
                    
                    private let serverCertificateName : String = "server"
                    private let serverCertificateExtension : String = "der"
                    
                    /**
                    This extends the urlSession function to verify server identity
                    */
                    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {

                        print("*** received SESSION challenge...\(challenge)")
                        let trust = challenge.protectionSpace.serverTrust!
                        let credential = URLCredential(trust: trust)
                        
                        var remoteCertMatchesPinnedCert = false
                        
                        if let myCertPath = Bundle.main.path(forResource: self.serverCertificateName, ofType: self.serverCertificateExtension) {
                            if let pinnedCertData = NSData(contentsOfFile: myCertPath) {
                                
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
                        } else {
                            print("*** Couldn't load pinning certificate!")
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

                Section (header: Text("Bootstrap Testing Info")) {

                    HStack {
                        Text("Share Bootstrap Info: ").bold()
                        Button(action: ShareRegisteInformation) {
                            Image(systemName: "square.and.arrow.up")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 24, height: 24)
                        }
                        
                    }
                    
                    HStack {
                        Text("Run Complete Booststrap Flow ").bold()
                        Button(action: {bootstrapManager.BootstrapNow()}, label: {
                            Text("Bootstrap Now")
                        })
                    }
                    
                }
                
                Section (header: Text("Cellular Info") ) {
                    
                    HStack {
                        Text("Carrier Name: ").bold()
                        Text(cellularNetworkInfo.getCarrierName())
                    }
                    
                    HStack {
                        Text("Mobile Network Code: ").bold()
                        Text(cellularNetworkInfo.getMobileNetworkCode())
                    }
                    
                    HStack {
                        Text("Mobile Country Code: ").bold()
                        Text(cellularNetworkInfo.getMobileCountryCode())
                    }
                    
                    HStack {
                        Text("ISO Country Code: ").bold()
                        Text(cellularNetworkInfo.getIsoCountryCode())
                    }
                    
                    HStack {
                        Text("Radio Technology: ").bold()
                        Text(cellularNetworkInfo.getCurrentRadioAccessTechnology())
                    }

                }
                
                Section (header: Text("Wi-Fi Info") ) {
                    
                    HStack {
                        Text("SSID: ").bold()
                        Text(wifiNetworkInfo.SSID)
                    }
                    
                    HStack {
                        Text("BSSID: ").bold()
                        Text(wifiNetworkInfo.BSSID)
                    }

                }
                
                Section (header: Text("Location") ) {
                    
                    HStack {
                        Text("Permission Status: ").bold()
                        Text(locationManager.statusString)
                    }
                 
                }
                
            }.navigationBarTitle("Openschema MMA")
                .listStyle(GroupedListStyle())
                
        }
        
        
    }
    
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
