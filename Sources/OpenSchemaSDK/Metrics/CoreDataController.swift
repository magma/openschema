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

import UIKit
import CoreData

class PackageDataStackController : NSObject {
    
    public static let shared = PackageDataStackController()

    private override init() {}
    
    // Create a subclass of NSPersistentStore Coordinator
    open class PersistentContainer: NSPersistentContainer {
    }
    
    lazy public var persistentContainer: PersistentContainer? = {
            guard let modelURL = Bundle.module.url(forResource:"AnalyticsModel", withExtension: "momd") else { return  nil }
            guard let model = NSManagedObjectModel(contentsOf: modelURL) else { return nil }
            let container = PersistentContainer(name:"AnalyticsModel",managedObjectModel:model)
            container.loadPersistentStores(completionHandler: { (storeDescription, error) in
                if let error = error as NSError? {
                    print("Unresolved error \(error), \(error.userInfo)")
                }
            })
            return container
        }()

    /// The managed object context associated with the main queue. (read-only)
        public var managedObjectContext : NSManagedObjectContext {
            return self.persistentContainer!.viewContext
        }
    
        public func saveContext () {
            if managedObjectContext.hasChanges {
                do {
                    try managedObjectContext.save()
                } catch {
                    let nserror = error as NSError
                    fatalError("Unresolved error \(nserror), \(nserror.userInfo)")
                }
            }
        }

}
