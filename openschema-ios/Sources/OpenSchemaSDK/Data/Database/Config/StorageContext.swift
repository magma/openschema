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

///StorageContext consists of generic DB operations that are required with almost any DB implementation
public protocol StorageContext {

    func create<DBEntity: Storable>(_ model: DBEntity.Type) -> DBEntity?

    func save(object: Storable) throws

    func saveAll(objects: [Storable]) throws

    func update(object: Storable) throws

    func delete(object: Storable) throws

    func deleteAll(_ model: Storable.Type) throws

    func fetch(_ model: Storable.Type, predicate: NSPredicate?, sorted: Sorted?, entityName : String) -> [Storable]
    
    //Coredata Only should be moved to StorageContext extension but currently running into issue like this one https://stackoverflow.com/questions/44703205/swift-protocol-extension-method-is-called-instead-of-method-implemented-in-subcl
    func objectWithObjectId<DBEntity: Storable>(objectId: NSManagedObjectID) -> DBEntity?
    
    func countObjects(entityName: String) -> Int
    
    func deleteAllByEntity(entityName : String) throws
    
    func fetchAllByEntity(entityName : String) -> [Storable]
    
    func fetchLastItem(entityName : String, dateItemName : String) -> Storable?
    
    func fetchFirstItem(entityName : String) -> Storable?

}
