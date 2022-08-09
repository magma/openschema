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

public class TotalDataUsage: DomainBaseEntity, Codable {
    
    public var startTimestamp : Int64?
    public var endTimestamp : Int64?
    public var eventWifiDataUsage : Int64?
    public var eventCellularDataUsage : Int64?
    public var wifiHourlyDataUsage : Int64?
    public var eventDuration : Int32?
    public var offsetMinutes : Int32?
    
    //TODO: check if this is the best way to store this to calculate hourly usage
    public var totalWifiDataUsage : Int64?
    public var totalCellularDataUsage : Int64?
    
    enum CodingKeys : String, CodingKey {

        case startTimestamp
        case endTimestamp
        case eventWifiDataUsage
        case eventCellularDataUsage
        case wifiHourlyDataUsage
        case eventDuration
        case offsetMinutes
        case totalWifiDataUsage
        case totalCellularDataUsage
        
    }
  
}
