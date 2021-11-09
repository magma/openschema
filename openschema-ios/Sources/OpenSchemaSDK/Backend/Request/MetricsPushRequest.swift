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

/**
* Class with the structure expected to be received in the OpenSchema's middle box registration API.
*/

public class MetricsPushRequest {
    private var metricName : String
    private var metricList : [[String : Any]]
    private var identifier : [String : Any]
    private var timestamp : [String : Any]
    
    public init(metricName : String, metricList : [[String : Any]], identifier : [String : Any], timestamp : [String : Any]) {
        self.metricName = metricName
        self.metricList = metricList
        self.identifier = identifier
        self.timestamp = timestamp
        
    }
    
    ///This function build the expected JSON request to push to metrics server. Edit as required.
    public func buildPushRequest() -> [String: Any] {
        let request : [String: Any] = [
            "metricName" : self.metricName,
            "metricsList" : self.metricList,
            "timestamp": self.timestamp,
            "identifier": self.identifier
            ]
        
        print(request)
        return request
    }
    
}
