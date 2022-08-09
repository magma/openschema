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
    
    //Might be needed for a more robust data usage
    private func bootTime() -> Date? {
        var tv = timeval()
        var tvSize = MemoryLayout<timeval>.size
        let err = sysctlbyname("kern.boottime", &tv, &tvSize, nil, 0);
        guard err == 0, tvSize == MemoryLayout<timeval>.size else {
            return nil
        }
        return Date(timeIntervalSince1970: Double(tv.tv_sec) + Double(tv.tv_usec) / 1_000_000.0)
    }

    private func storeTotalDataUsage() -> Void {
        
        let totalWifiDataUsage = Int64(wifiNetworkInfo.getWifiDataUsage())
        let totalCellularDataUsage = Int64(cellularNetworkInfo.getCellularDataUsage())
        
        let totalDataUsage : TotalDataUsage = TotalDataUsage()
        totalDataUsage.eventWifiDataUsage = 0
        totalDataUsage.eventCellularDataUsage = 0
        let firstTimestamp = Date().millisecondsSince1970
        totalDataUsage.startTimestamp = firstTimestamp
        totalDataUsage.endTimestamp = firstTimestamp
        totalDataUsage.offsetMinutes = Int32(TimeZone.current.secondsFromGMT() * 1000)
        totalDataUsage.eventDuration = 0
        
        //TODO: might need to remove this in the future
        totalDataUsage.totalWifiDataUsage = totalWifiDataUsage
        totalDataUsage.totalCellularDataUsage = totalCellularDataUsage
    
        do {
            try CoreDataController.shared.totalDataUsageDao.save(object: totalDataUsage)
            print("Collected Total Data Usage Succesfully!")
            
        } catch {
            print("Failed to Collect Data Usage: \(error)")
        }
    }
    
    private func storeTotalDataUsage(previousTimestamp: Int64, previousTotalWifiUsage : Int64, previousTotalCellularUsage: Int64) -> Void {
        
        let totalWifiDataUsage = Int64(wifiNetworkInfo.getWifiDataUsage())
        let totalCellularDataUsage = Int64(cellularNetworkInfo.getCellularDataUsage())
        
        let totalDataUsage : TotalDataUsage = TotalDataUsage()
        
        //Handle phone restart
        //TODO: maybe need to know phone uptime possible issue time zone change? https://developer.apple.com/forums/thread/101874
        if(totalWifiDataUsage < previousTotalWifiUsage || totalCellularDataUsage < previousTotalCellularUsage)
        {
            totalDataUsage.eventWifiDataUsage = totalWifiDataUsage
            totalDataUsage.eventCellularDataUsage = totalCellularDataUsage
        }
        else {
            //Default case where phone has not rebooted and API not reset
            totalDataUsage.eventWifiDataUsage = totalWifiDataUsage - previousTotalWifiUsage
            totalDataUsage.eventCellularDataUsage = totalCellularDataUsage - previousTotalCellularUsage
        }
        totalDataUsage.startTimestamp = previousTimestamp
        totalDataUsage.endTimestamp = Date().millisecondsSince1970
        totalDataUsage.offsetMinutes = Int32(TimeZone.current.secondsFromGMT() * 1000)
        totalDataUsage.eventDuration = Int32(totalDataUsage.endTimestamp! - previousTimestamp)
        
        //TODO: might need to remove this in the future
        totalDataUsage.totalWifiDataUsage = totalWifiDataUsage
        totalDataUsage.totalCellularDataUsage = totalCellularDataUsage
    
        do {
            try CoreDataController.shared.totalDataUsageDao.save(object: totalDataUsage)
            print("Collected Total Data Usage Succesfully!")
            
        } catch {
            print("Failed to Collect Data Usage: \(error)")
        }
    }
    
    public func calculateHourlyUsage() -> Void {
        
        guard let previousTotalUsage = CoreDataController.shared.totalDataUsageDao.fetchLastItem(entityName: "TotalDataUsageEntity", dateItemName: "endTimestamp") else {
            self.storeTotalDataUsage()
            print("This means there was no total usage or that this is first run")
            return
        }
 
        self.storeTotalDataUsage(previousTimestamp: previousTotalUsage.endTimestamp, previousTotalWifiUsage: previousTotalUsage.totalWifiDataUsage, previousTotalCellularUsage: previousTotalUsage.totalCellularDataUsage)

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
