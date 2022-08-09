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

public class BytesUnitConverter {
    
    public let UNITS_PER_POWER_SI : Double = 1000.0
    public let UNITS_PER_POWER_BINARY : Double = 1024.0
    public let DEFAULT_UNITS : Double
    
    //For rounding
    private let DIVISOR : Double = 100.0

    public init() {
        self.DEFAULT_UNITS = self.UNITS_PER_POWER_SI
    }
    
    public func bytesToKB(bytes : Int64) -> Double {
        return ((Double(bytes) / self.DEFAULT_UNITS) * DIVISOR).rounded() / DIVISOR
    }
    
    public func bytesToMB(bytes : Int64) -> Double {
        return ((Double(bytes) / self.DEFAULT_UNITS / self.DEFAULT_UNITS) * DIVISOR).rounded() / DIVISOR
    }
    
    public func bytesToGB(bytes : Int64) -> Double {
        return ((Double(bytes) / self.DEFAULT_UNITS / self.DEFAULT_UNITS / self.DEFAULT_UNITS) * DIVISOR).rounded() / DIVISOR
    }
    
    public func bytesToString(bytes : Int64) -> String {
        
        if(bytes < Int64(DEFAULT_UNITS)) {
            return "\(String(format: "%.1f", bytes)) B"
        }
        
        else if(bytes < Int64(DEFAULT_UNITS * DEFAULT_UNITS)) {
            return "\(String(format: "%.1f", self.bytesToKB(bytes: bytes))) KB"
        }
        
        else if(bytes < Int64(DEFAULT_UNITS * DEFAULT_UNITS * DEFAULT_UNITS)) {
            return "\(String(format: "%.1f", self.bytesToMB(bytes: bytes))) MB"
        }
        
        return "\(String(format: "%.1f", self.bytesToGB(bytes: bytes))) GB"
    }
    

    public func gbToBytes(gb : Double) -> Int64 {
        return Int64(gb * DEFAULT_UNITS * DEFAULT_UNITS * DEFAULT_UNITS)
    }
    
}
