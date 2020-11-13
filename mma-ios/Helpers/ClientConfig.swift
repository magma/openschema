//
//  ClientConfig.swift
//  mma-ios
//
//  Created by Rodrigo Saravia on 11/13/20.
//

import Foundation

///This class handles all the variables for the client to connect to a server. It has the values containing the server address and port, bootstrap controller address and metrics controller address.
public class ClientConfig {
    
    static let shared = ClientConfig()
    private let CONTROLLER_ADDRESS : String
    private let CONTROLLER_PORT : Int
    private let BOOTSTRAPPER_CONTROLLER_ADDRESS : String
    private let METRICS_AUTHORITY_HEADER : String
    
    private init() {
        self.CONTROLLER_ADDRESS = "controller.openschema.magma.etagecom.io"
        self.CONTROLLER_PORT = 443
        self.BOOTSTRAPPER_CONTROLLER_ADDRESS = "bootstrapper-" + CONTROLLER_ADDRESS
        self.METRICS_AUTHORITY_HEADER = "metricsd-" + CONTROLLER_ADDRESS
    }

    /**
    This function retrieves Controller Address
    */
    func getControllerAddress() -> String { return self.CONTROLLER_ADDRESS }
    
    /**
    This function retrieves Controller Port
    */
    func getControllerPort() -> Int { return self.CONTROLLER_PORT }
    
    /**
    This function retrieves Controller Address
    */
    func getBootstrapControllerAddress() -> String { return self.BOOTSTRAPPER_CONTROLLER_ADDRESS }
    
    /**
     This function retrieves Metric  Authority Header
    */
    func getMetricAuthorityHeader() -> String { return self.METRICS_AUTHORITY_HEADER }
        
}
