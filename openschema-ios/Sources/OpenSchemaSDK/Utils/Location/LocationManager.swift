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
import CoreLocation
import Combine

///This class asks users for location permission and handles the location information.
public class LocationManager: NSObject, ObservableObject {
    
    ///Initialize LocationManager. First  This request the user for permission to use location
    override public init() {
        super.init()
        self.locationManager.delegate = self
        self.locationManager.desiredAccuracy = kCLLocationAccuracyBest
        self.locationManager.requestWhenInUseAuthorization()
        self.locationManager.startUpdatingLocation()
    }
    
    ///This variable tracks if location permission has changed. Example revoked manually by user.
    @Published public var locationStatus: CLAuthorizationStatus? {
        willSet {
            objectWillChange.send()
        }
    }
    
     ///This variable tracks the location from the device.
    @Published public var lastLocation: CLLocation? {
        willSet {
            objectWillChange.send()
        }
    }
    
    ///Return current permission status for location
    ///Posible values:
    ///1. notDetermined
    ///2. authorizedWhenInUse
    ///3. authorizedAlways
    ///4. restricted
    ///5. denied
    ///6. unknown
    public var statusString: String {
        guard let status = locationStatus else {
            return "unknown"
        }

        switch status {
        case .notDetermined: return "notDetermined"
        case .authorizedWhenInUse: return "authorizedWhenInUse"
        case .authorizedAlways: return "authorizedAlways"
        case .restricted: return "restricted"
        case .denied: return "denied"
        default: return "unknown"
        }

    }

    public let objectWillChange = PassthroughSubject<Void, Never>()

    public let locationManager = CLLocationManager()
}

extension LocationManager: CLLocationManagerDelegate {

    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        self.locationStatus = status
        //print(#function, statusString)
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        self.lastLocation = location
        //print(#function, location)
    }

}
