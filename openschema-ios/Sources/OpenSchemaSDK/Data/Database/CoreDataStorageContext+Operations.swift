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

///Implements all the required methods from the StorageContext protocol.
public extension CoreDataStorageContext {

    func create<DBEntity: Storable>(_ model: DBEntity.Type) -> DBEntity? {
        let entityDescription =  NSEntityDescription.entity(forEntityName: String.init(describing: model.self),
                                                            in: managedContext!)
        let entity = NSManagedObject(entity: entityDescription!,
                                     insertInto: managedContext)
        return entity as? DBEntity
    }

    func save(object: Storable) throws {
    }

    func saveAll(objects: [Storable]) throws {
    }

    func update(object: Storable) throws {
    }

    func delete(object: Storable) throws {
    }

    func deleteAll(_ model: Storable.Type) throws {
    }

    func fetch(_ model: Storable.Type, predicate: NSPredicate?, sorted: Sorted?) -> [Storable] {
        return []
    }
    
    func objectWithObjectId<DBEntity: Storable>(objectId: NSManagedObjectID) -> DBEntity? {
        do {
            let result = try managedContext!.existingObject(with: objectId)
            return result as? DBEntity
        } catch {
            print("Failure")
        }

        return nil
    }
    
    func countObjects(entityName : String) -> Int {
        
        let request = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)
        
        do {
            let count = try managedContext!.count(for: request)
            print("CoreDataStorageContext there are " + String(count) + " " + entityName + " Objects")
            return count
        } catch {
            print("CoreDataStorageContext can't count object")
        }
        
        return 0
    }
    
    func fetchAllByEntity(entityName : String) -> [Storable] {
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)

        do {
            let entities = try managedContext!.fetch(fetchRequest) as! [Storable]
            print("fetched something ")
            print(entities)
            return entities
            
        } catch {
            print(error)
        }
        
        return []
        
    }
    
}
