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

public class MillisecondsUnitConverter {
    
    public init() { }
    
    public func millisecondsToSeconds(ms : Int64) -> Double {
        return Double(ms) / 1000.0
    }
    
    public func millisecondsToMinutes(ms : Int64) -> Double {
        return Double(ms) / 60000.0
    }
    
    public func millisecondsToHours(ms : Int64) -> Double {
        return Double(ms) / 3600000.0
    }
    
    public func millisecondsToString(ms : Int64) -> String {
        
    //TODO: Improve function to separate values to each type HH:mm:ss format
        if(ms < 1000) {
            return "\(ms) ms"
        }
        else if(ms < 60000) {
            return "\(self.millisecondsToSeconds(ms: ms)) s"
        }
        else if(ms < 3600000) {
            return "\(self.millisecondsToMinutes(ms: ms)) m"
        }
        
        return "\(self.millisecondsToHours(ms: ms)) H"
    }
}
