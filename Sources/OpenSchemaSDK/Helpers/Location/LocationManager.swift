//
//  LocationManager.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation
import CoreLocation
import Combine

///This class asks users for location permission and handles the location information.
public class LocationManager: NSObject, ObservableObject {

    override public init() {
        super.init()
        self.locationManager.delegate = self
        self.locationManager.desiredAccuracy = kCLLocationAccuracyBest
        self.locationManager.requestWhenInUseAuthorization()
        self.locationManager.startUpdatingLocation()
    }

    @Published public var locationStatus: CLAuthorizationStatus? {
        willSet {
            objectWillChange.send()
        }
    }

    @Published public var lastLocation: CLLocation? {
        willSet {
            objectWillChange.send()
        }
    }

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
        print(#function, statusString)
    }

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        self.lastLocation = location
        print(#function, location)
    }

}
