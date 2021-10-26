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

import CoreData

///Core Data has some functions that are required in most cases, so let's add these methods as an extension of the StorageContext. CoreData entities are identified by NSManagedObjectID; we’ll need this method when fetching existing objects by ID from the database. You can also add other similar methods as needed.
public extension StorageContext {

    func objectWithObjectId<DBEntity: Storable>(objectId: NSManagedObjectID) -> DBEntity? {
        return nil
    }
    
    func countObjects(request : NSFetchRequest<NSFetchRequestResult>) -> Int? {
        return nil
    }
}
