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

public class DeviceInfo: DomainBaseEntity, Codable {
    public var iOSVersion: String?
    public var deviceManufacturer: String?
    public var deviceModel: String?
    public var deviceBrand: String?
    public var timestamp: Int64?
    public var offsetMinutes: Int32?
  
}
