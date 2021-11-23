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

import Logging
import Foundation

///Wi-Fi and Cellular usage collected on an hourly basis.
public class NetworkHourlyMetrics : SyncMetrics{
    
    private let Log : Logger = Logger(label: "networkHourlyMetrics")
    private var wifiNetworkInfo = WifiNetworkInfo.shared
    private var cellularNetworkInfo = CellularNetworkInfo()
    
    ///Metric name to be used for the collected information.
    public let METRIC_NAME : String = "openschemaUsageHourly"
    private let METRIC_TRANSPORT_TYPE : String = "transportType"
    private let METRIC_RX_BYTES : String = "rxBytes"
    private let METRIC_TX_BYTES : String = "txBytes"
    private let METRIC_SEGMENT_START_TIME : String = "segmentStartTime"

    ///Metric values.
    private let transportType : String
    private var rxBytes : Int64
    private var txBytes : Int64
    private let segmentStartTime : Int64

    public init() {
        self.transportType = "Wi-Fi"
        self.rxBytes = 0
        self.txBytes = 0
        self.segmentStartTime = Date().millisecondsSince1970
    }
    
    public func storeInCoredata() -> Void {
        
        self.calculateHourlyUsage()
        
        let hourlyData = HourlyData()
        hourlyData.transportType = self.transportType
        hourlyData.rxBytes = self.rxBytes
        hourlyData.txBytes = self.txBytes
        hourlyData.segmentStartTime = Date().millisecondsSince1970

        do {
            try CoreDataController.shared.hourlyDataDao.save(object: hourlyData)
            Log.debug("Collected Network Hourly Data Succesfully!")
            
        } catch {
            Log.error("Failed to Collect Network Hourly Data: \(error)")
        }
    }
    
    private func storeTotalDataUsage() -> Void {
        let totalDataUsage : TotalDataUsage = TotalDataUsage()
        totalDataUsage.wifiRxBytes = Int64(wifiNetworkInfo.getRxBytes())
        totalDataUsage.wifiTxBytes = Int64(wifiNetworkInfo.getTxBytes())
        totalDataUsage.cellularRxBytes = Int64(cellularNetworkInfo.getRxBytes())
        totalDataUsage.cellularTxBytes = Int64(cellularNetworkInfo.getTxBytes())
        totalDataUsage.timestamp = Date().millisecondsSince1970
        
        
        do {
            try CoreDataController.shared.totalDataUsageDao.save(object: totalDataUsage)
            print("Collected Total Data Usage Succesfully!")
            
        } catch {
            print("Failed to Collect Data Usage: \(error)")
        }
    }
    
    private func calculateHourlyUsage() -> Void {
        
        guard let previousTotalUsage = CoreDataController.shared.totalDataUsageDao.fetchLastItem(entityName: "TotalDataUsageEntity", dateItemName: "timestamp") else {
            self.rxBytes = 0
            self.txBytes = 0
            print("Error This means there was no total usage")
            return
        }
        
        let hourRxBytes = Int64(wifiNetworkInfo.getRxBytes()) - previousTotalUsage.wifiRxBytes
        let hourTxBytes = Int64(wifiNetworkInfo.getTxBytes()) - previousTotalUsage.wifiTxBytes
        
        self.rxBytes = hourRxBytes
        self.txBytes = hourTxBytes
        
        self.storeTotalDataUsage()

    }
    
    public func retrieveMetrics() -> [[String : Any]] {
        
        let json : [[String : Any]] = [
            ["first" : METRIC_TRANSPORT_TYPE , "second" : self.transportType ],
            ["first" : METRIC_RX_BYTES , "second" : self.rxBytes ],
            ["first" : METRIC_TX_BYTES , "second" : self.txBytes ],
            ["first" : METRIC_SEGMENT_START_TIME , "second" : self.segmentStartTime ]
        ]
        
        return json
    }
    
}
