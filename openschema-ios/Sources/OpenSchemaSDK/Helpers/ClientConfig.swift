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

///This class handles all the variables for the client to connect to a server. It has the values containing the server address and port, bootstrap controller address and metrics controller address.
public class ClientConfig {
    
    ///Shared ClientConfig Singleton.
    public static let shared = ClientConfig()
    ///It is the server controller URL.
    private let CONTROLLER_ADDRESS : String
    ///Port to be used to connect to Controller Address
    private let CONTROLLER_PORT : Int
    ///Server Bootstrap Controller URL
    private let BOOTSTRAPPER_CONTROLLER_ADDRESS : String
    ///Server Metrics Controller URL
    private let METRICS_AUTHORITY_HEADER : String
    
    ///Initialize singleton class. Currently uses magma server address.
    private init() {
        self.CONTROLLER_ADDRESS = "controller.openschema.magma.etagecom.io"
        self.CONTROLLER_PORT = 443
        self.BOOTSTRAPPER_CONTROLLER_ADDRESS = "bootstrapper-" + CONTROLLER_ADDRESS
        self.METRICS_AUTHORITY_HEADER = "metricsd-" + CONTROLLER_ADDRESS
    }


    ///This function retrieves Controller Address.
    public func getControllerAddress() -> String { return self.CONTROLLER_ADDRESS }
    
    ///This function retrieves Controller Port.
    public func getControllerPort() -> Int { return self.CONTROLLER_PORT }
    
    ///This function retrieves Controller Address.
    public func getBootstrapControllerAddress() -> String { return self.BOOTSTRAPPER_CONTROLLER_ADDRESS }
    
    ///This function retrieves Metric  Authority Header.
    public func getMetricAuthorityHeader() -> String { return self.METRICS_AUTHORITY_HEADER }
        
}
