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
        do {
            try self.managedContext?.save()
        } catch {
            print("Failed to Save data")
        }
    }

    func saveAll(objects: [Storable]) throws {
        do {
            for object in objects {
                try save(object: object)
            }
        } catch {
            print("Failed to save objects")
        }
    }

    func update(object: Storable) throws {
    }

    func delete(object: Storable) throws {
    }

    func deleteAll(_ model: Storable.Type) throws {
        /*guard let url = persistentContainer.persistentStoreDescriptions.first?.url else { return }
        
        self.managedContext?.persis

         do {
             try self.managedContext?.persistentStoreCoordinator.destroyPersistentStore(at:url, ofType: model, options: nil)
             try self.managedContext?.persistentStoreCoordinator.addPersistentStore(ofType: model, configurationName: nil, at: url, options: nil)
         } catch {
             print("Attempted to clear persistent store: " + error.localizedDescription)
         }*/
    }

    func fetch(_ model: Storable.Type, predicate: NSPredicate?, sorted: Sorted?, entityName : String) -> [Storable] {
        
        let request = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)
        request.predicate = predicate

        do {
            let entities = try managedContext!.fetch(request) as! [Storable]
            print("Fetched items filtered by predicate with EntityName: \(entityName)")
            return entities
        } catch {
            print(error)
        }
        
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
    
    func deleteAllByEntity(entityName : String) throws {
        let fetchRequest : NSFetchRequest<NSFetchRequestResult> = NSFetchRequest(entityName: entityName)
        let deleteRequest = NSBatchDeleteRequest(fetchRequest: fetchRequest)

        do {
            try managedContext?.execute(deleteRequest)
            print("Deleted elements with entity name \(entityName)")
        } catch let error as NSError {
            // TODO: handle the error
            print("Failed to delete elements with entity name \(entityName) with \(error)")
        }
    }
    
    func fetchAllByEntity(entityName : String) -> [Storable] {
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)

        do {
            let entities = try managedContext!.fetch(fetchRequest) as! [Storable]
            print("fetched something ")
            return entities
            
        } catch {
            print(error)
        }
        
        return []
        
    }
    
    func fetchLastItem(entityName : String, dateItemName : String) -> Storable? {
        
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: dateItemName, ascending: false)]
        fetchRequest.fetchLimit = 1
        
        do {
            let entities = try managedContext!.fetch(fetchRequest) as! [Storable]
            return entities.last
            
        } catch {
            print(error)
        }
        
        return nil

    }
    
    func fetchFirstItem(entityName : String) -> Storable? {
        
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: entityName)
        fetchRequest.fetchLimit = 1
        
        do {
            let entities = try managedContext!.fetch(fetchRequest) as! [Storable]
            return entities.first
            
        } catch {
            print(error)
        }
        
        return nil

    }
    
}
