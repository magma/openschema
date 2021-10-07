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

class DeviceInfo: DomainBaseEntity, Codable {
    var iOSVersion: String?
    var deviceManufacturer: String?
    var deviceModel: String?
    var deviceBrand: String?
    var offsetminutes: String?
    var timestamp: String?
}
