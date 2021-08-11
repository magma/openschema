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
import Reachability

///This Class Handles the observer that listens to network connectivity changes. Possible Modes: Wi-Fi connected, Cellular Connected, No Connection.
public class ReachabilityObserver {
    
    ///Declare singleton to be shared on other classes.
    public static let shared = ReachabilityObserver()
    ///Creates Reachabilty object
    private let reachability = try! Reachability()
    ///Variable to compare with current SSID
    private var newSSID : String = "No Wi-Fi Connected"
    ///Current SSID
    private var currentSSID : String = "No Wi-Fi Connected"
    
    ///Initializes the class and ads an observer to constantly monitor changes in connectivity using Reachability class.
    private init() {
        NotificationCenter.default.addObserver(self, selector: #selector(reachabilityChanged(note:)), name: .reachabilityChanged, object: reachability)
            do{
              try reachability.startNotifier()
            }catch{
              print("could not start reachability notifier")
            }
        
        
    }
    
    ///This function monitors the connection status and updates accordingly
    @objc private func reachabilityChanged(note: Notification) {
        let reachability = note.object as! Reachability
        
        if reachability.connection == .wifi {
                print("Wi-Fi Connection is reachable")
                
            if self.newSSID != self.currentSSID {
                print("New SSID detected")
                self.currentSSID = self.newSSID
            }
            
        } else if reachability.connection == .cellular{
            print("Cellular Connection is reachable")
        }
        
        else if reachability.connection == .unavailable {
            print("No Connection is reachable")
        }
    }
    
    deinit {
        reachability.stopNotifier()
    }
    
}
