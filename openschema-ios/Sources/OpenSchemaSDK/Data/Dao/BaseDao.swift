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

public class BaseDao<DomainEntity: Mappable, DBEntity: Storable> {

    private var storageContext: StorageContext?

    required init(storageContext: StorageContext) {
        self.storageContext = storageContext
    }

    public func create() -> Mappable? {
        let dbEntity: DBEntity? = storageContext?.create(DBEntity.self)
        return mapToDomain(dbEntity: dbEntity!)
    }

    public func save<DomainEntity: Mappable>(object: DomainEntity) throws {
        var dbEntity: DBEntity?
        if object.objectID != nil {
            dbEntity = storageContext?.objectWithObjectId(objectId: object.objectID!)
        } else {
            dbEntity = storageContext?.create(DBEntity.self)
        }

        Mapper.mapToDB(from: object, target: dbEntity!)
        try storageContext?.save(object: dbEntity!)
    }

    public func saveAll<DomainEntity: Mappable>(objects: [DomainEntity]) throws {
        for domainEntity in objects {
            try self.save(object: domainEntity)
        }
    }

    public func update<DomainEntity: Mappable>(object: DomainEntity) throws {
        if object.objectID != nil {
            let dbEntity: DBEntity? = storageContext?.objectWithObjectId(objectId: object.objectID!)
            Mapper.mapToDB(from: object, target: dbEntity!)
            try storageContext?.update(object: dbEntity!)
        }
    }

    public func delete<DomainEntity: Mappable>(object: DomainEntity) throws {
        if object.objectID != nil {
            let dbEntity: DBEntity? = storageContext?.objectWithObjectId(objectId: object.objectID!)
            try storageContext?.delete(object: dbEntity!)
        }
    }

    public func deleteAll() throws {
        try storageContext?.deleteAll(DBEntity.self)
    }

    public func fetch(predicate: NSPredicate?, sorted: Sorted? = nil, entityName : String) -> [DomainEntity] {
        let dbEntities = storageContext?.fetch(DBEntity.self, predicate: predicate, sorted: sorted, entityName : entityName) as? [DBEntity]
        return mapToDomain(dbEntities: dbEntities)
    }
    
    public func countObjects(entityName : String) -> Int {
        let count = storageContext?.countObjects(entityName: entityName)
        return count ?? 0
    }
    
    public func deleteAllByEntity(entityName : String) throws {
        do {
            try storageContext?.deleteAllByEntity(entityName: entityName)
        }
    }
    
    public func fetchAllByEntity(entityName : String) -> [DomainEntity] {
        let entities = storageContext?.fetchAllByEntity(entityName: entityName) as? [DBEntity]
        return mapToDomain(dbEntities: entities)
    }
    
    public func fetchLastItem(entityName : String, dateItemName : String) -> DBEntity? {
        let entity = storageContext?.fetchLastItem(entityName: entityName, dateItemName: dateItemName) as? DBEntity
        return entity
    }
    
    public func fetchFirstItem(entityName : String) -> DBEntity? {
        let entity = storageContext?.fetchFirstItem(entityName: entityName) as? DBEntity
        return entity
    }
    
    private func mapToDomain<DBEntity: Storable>(dbEntity: DBEntity) -> DomainEntity {
        var domainEntity = DomainEntity.init()
        Mapper.mapToDomain(from: dbEntity, target: &domainEntity)
        return domainEntity
    }

    private func mapToDomain<DBEntity: Storable>(dbEntities: [DBEntity]?) -> [DomainEntity] {
        var domainEntities = [DomainEntity]()
        for dbEntity in dbEntities! {
            domainEntities.append(mapToDomain(dbEntity: dbEntity))
        }
        return domainEntities
    }
}
