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

public class CoreDataController {

    // MARK: - Private properties
    private var storageContext: StorageContext?

    // MARK: - Public properties
    public static var shared = CoreDataController()
    
    //Here your xdata classes
    public lazy var deviceInfoDao = DeviceInfoDAO(storageContext: storageContextImpl())
    public lazy var hourlyDataDao = HourlyDataDAO(storageContext: storageContextImpl())
    public lazy var totalDataUsageDao = TotalDataUsageDAO(storageContext: storageContextImpl())
    public lazy var totalHourlyDataUsageDao = TotalHourlyDataUsageDAO(storageContext: storageContextImpl())

    private init() {
    }

    public static func setup(storageContext: StorageContext) {
        shared.storageContext = storageContext
    }

    private func storageContextImpl() -> StorageContext {
        if self.storageContext != nil {
            return self.storageContext!
        }
        fatalError("You must call setup to configure the StoreContext before accessing any dao")
    }

}
