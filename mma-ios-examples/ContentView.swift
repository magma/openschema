//
//  ContentView.swift
//  mma-ios-examples
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import SwiftUI

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
