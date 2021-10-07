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

import Runtime
import CoreData

///Class that maps the entities from domain to DB and vice versa
class Mapper {

    class func copy<DomainEntity: Mappable>(from domainEntity: DomainEntity,
                                            target dbEntity: inout DomainEntity) {
        let info = try? typeInfo(of: DomainEntity.self)
        for property in info!.properties {
            try? property.set(value: property.get(from: domainEntity), on: &dbEntity)
        }
    }

    class func mapToDomain<DBEntity: Storable, DomainEntity: Mappable>(from dbEntity: DBEntity, target domainEntity: inout DomainEntity) {

        let domainEntityInfo = try? typeInfo(of: DomainEntity.self)
        let managedObject: NSManagedObject? = dbEntity as? NSManagedObject
        let keys = managedObject?.entity.attributesByName.keys

        for dbEntityKey in keys! {
            let value = managedObject?.value(forKey: dbEntityKey)
            do {
                let domainProperty = try domainEntityInfo?.property(named: dbEntityKey)
                try domainProperty?.set(value: value as Any, on: &domainEntity)
            } catch {
                print(error.localizedDescription)
            }
        }
        domainEntity.objectID = managedObject?.objectID
    }

    class func mapToDB<DomainEntity: Mappable, DBEntity: Storable>(from domainEntity: DomainEntity, target dbEntity: DBEntity) {

        let managedObject: NSManagedObject? = dbEntity as? NSManagedObject
        let keys = managedObject?.entity.attributesByName.keys

        let domainEntityMirror = Mirror(reflecting: domainEntity)
        for dbEntityKey in keys! {
            for property in domainEntityMirror.children.enumerated() where
                property.element.label == dbEntityKey {
                    let value = property.element.value as AnyObject
                    if !value.isKind(of: NSNull.self) {
                        managedObject?.setValue(value, forKey: dbEntityKey)
                    }
            }
        }
    }
}
