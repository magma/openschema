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

///This Class Handles the observer that listens SSID being Changed.
public class WifiObserver {
    
    ///Singleton to be shared by other classes.
    public static let shared = WifiObserver()
    ///WifiNetworkInfo shared Singleton
    private let wifiNetworkInfo = WifiNetworkInfo.shared
    private let wifiNetworkMetrics = WifiNetworkMetrics()
    
    ///Initialize class and call CreateSSIDObserver
    private init() {
        
        self.CreateSSIDObserver()
        
    }

    ///This function creates the observer to check for any change on SSID value.
    private func CreateSSIDObserver() {
        let observer : UnsafeRawPointer! = UnsafeRawPointer(Unmanaged.passUnretained(self).toOpaque())
        let object : UnsafeRawPointer! = nil
        
        let callback: CFNotificationCallback = { center, observer, name, object, info in
            print("Wi-Fi SSID name changed")
            
            let mySelf = Unmanaged<WifiObserver>.fromOpaque(UnsafeRawPointer(observer!)).takeUnretainedValue()
            // Call instance method:
            mySelf.wifiNetworkInfo.updateWifiNetworkInfo()
            mySelf.wifiNetworkMetrics.CollectWifiNetworkInfoMetrics()

        }

        CFNotificationCenterAddObserver(CFNotificationCenterGetDarwinNotifyCenter(),
                                        observer,
                                        callback,
                                        "com.apple.system.config.network_change" as CFString,
                                        object,
                                        .deliverImmediately)
    }
    
}
