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
import CoreData

public typealias MagmaLabelContainer = [Magma_Orc8r_LabelPair]
public typealias MagmaMetricContainer = [Magma_Orc8r_Metric]
public typealias MagmaMetricFamilyContainer = [Magma_Orc8r_MetricFamily]

public class CustomMetrics {
    
    public enum SimpleMetricType {
        case untyped
        case gauge
        case counter
    }
    
    private let coreDataController = PackageDataStackController.shared
    private let uuidManager = UUIDManager.shared
    
    public init() {
        
    }
    
    private func CreateLabelPair(labelName : String, labelValue : String) -> Magma_Orc8r_LabelPair {
        let label : Magma_Orc8r_LabelPair = Magma_Orc8r_LabelPair.with {
            $0.name = labelName
            $0.value = labelValue
        }
        
        return label
    }
    
    private func FetchFamilyOrCreate(familyName: String) -> MetricFamily {
        
        //TODO add safe measeure to prevent returning empty family object
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "MetricFamily")
        fetchRequest.predicate = NSPredicate(format: "familyName == %@", familyName)
        
        var family : MetricFamily = MetricFamily()
        
        do {
            //TODO: Find a way to ensure only one object is saved efficiently
            let families = try coreDataController.managedObjectContext.fetch(fetchRequest) as! [MetricFamily]
            
            if (families.count == 0) {
                family = NSEntityDescription.insertNewObject(forEntityName: "MetricFamily", into: coreDataController.managedObjectContext) as! MetricFamily
                family.familyName = familyName
            } else if (families.count == 1) {
                family = families[0]
            } else {
                print("Multiple of the same family are stored ERROR?, This should not happen")
            }

        } catch {
            print(error)
        }
        
        return family
        
    }
    
    private func clearAllCoreData() {
        let entities = coreDataController.persistentContainer?.managedObjectModel.entities
        
        if entities != nil {
            for entity in entities! {
                clearDeepObjectEntity(entity: entity.name!)
            }
        }
    }

    private func clearDeepObjectEntity(entity: String) {

        let deleteFetch = NSFetchRequest<NSFetchRequestResult>(entityName: entity)
        let deleteRequest = NSBatchDeleteRequest(fetchRequest: deleteFetch)

        do {
            try coreDataController.managedObjectContext.execute(deleteRequest)
            try coreDataController.managedObjectContext.save()
        } catch {
            print ("There was an error")
        }
    }
    
    public func CreateSimpleMetric(familyName : String, LabelContainer : [(labelName: String, labelValue: String)], metricValue: Double) -> Bool {
        
        let customMetric = NSEntityDescription.insertNewObject(forEntityName: "CustomMetric", into: coreDataController.managedObjectContext) as! CustomMetric
        customMetric.timestamp = String(Date().millisecondsSince1970)
        customMetric.value = metricValue
        customMetric.family = FetchFamilyOrCreate(familyName: familyName)
        
        for labelPair in LabelContainer {
            let labelContainer = NSEntityDescription.insertNewObject(forEntityName: "LabelContainer", into: coreDataController.managedObjectContext) as! LabelContainer
            labelContainer.customMetric = customMetric
            labelContainer.labelName = labelPair.labelName
            labelContainer.labelValue = labelPair.labelValue
        }
        
        do {
            try coreDataController.managedObjectContext.save()
            
        } catch {
            fatalError("Failure to save context: \(error)")
        }
        
        print("Data Saved")
        return true
    }
    
    //TODO: Do not delete Coredata if metrics for some reason fail to be pushed
    public func FetchMetricsFromCoreData() -> Magma_Orc8r_MetricsContainer {
        let fetchRequest = NSFetchRequest<NSFetchRequestResult>(entityName: "MetricFamily")
        var metricFamilyContainer : MagmaMetricFamilyContainer = MagmaMetricFamilyContainer()
        
        do {
            
            let families = try coreDataController.managedObjectContext.fetch(fetchRequest) as! [MetricFamily]
            
            for family in families {
                
                var metricContainer : MagmaMetricContainer = MagmaMetricContainer()
                let metrics = family.metrics!.allObjects as! [CustomMetric]
                
                for metric in metrics {

                    var labelContainer : MagmaLabelContainer = MagmaLabelContainer()
                    let labels = metric.labels!.allObjects as! [LabelContainer]
                    
                    for label in labels {
                        labelContainer.append(CreateLabelPair(labelName: label.labelName!, labelValue: label.labelValue!))
                    }
                    
                    metricContainer.append(CreateMagmaMetric(simpleMetricType: .untyped, labelContainer: labelContainer, value: metric.value))
                }
                
                metricFamilyContainer.append(CreateMagmaFamilyForSimpleMetric(simpleMetricType: .untyped, metrics : metricContainer, familyName: family.familyName!))
                
            }
   
        }
        catch  {
            
            print(error)
            
        }
  
        clearAllCoreData()
        
        return CreateMetricsContainer(metricFamilyContainer: metricFamilyContainer, gatewayID: uuidManager.getUUID())
    }
    
    private func CreateMagmaMetric(simpleMetricType : SimpleMetricType, labelContainer : MagmaLabelContainer, value : Double) -> Magma_Orc8r_Metric {
        
        switch simpleMetricType {
        case .counter:
            let counter : Magma_Orc8r_Counter = Magma_Orc8r_Counter.with {
                $0.value = value
            }
            
            let metric = Magma_Orc8r_Metric.with {
                $0.label = labelContainer
                $0.counter = counter
            }
            
            return metric
        case .gauge:
            let gauge : Magma_Orc8r_Gauge = Magma_Orc8r_Gauge.with {
                $0.value = value
            }

            let metric = Magma_Orc8r_Metric.with {
                $0.label = labelContainer
                $0.gauge = gauge
            }
            
            return metric
        
        case .untyped:
            let untyped : Magma_Orc8r_Untyped = Magma_Orc8r_Untyped.with {
                $0.value = value
            }

            let metric = Magma_Orc8r_Metric.with {
                $0.label = labelContainer
                $0.untyped = untyped
            }
            
            return metric
        }
        
    }
    
    private func CreateMagmaFamilyForSimpleMetric(simpleMetricType: SimpleMetricType, metrics : MagmaMetricContainer, familyName: String) -> Magma_Orc8r_MetricFamily {
        
        switch simpleMetricType {
        case .counter:
            let family = Magma_Orc8r_MetricFamily.with {
                $0.name = familyName
                $0.metric = metrics
                $0.type = .counter
            }
            
            return family
            
        case .gauge:
            let family = Magma_Orc8r_MetricFamily.with {
                $0.name = familyName
                $0.metric = metrics
                $0.type = .gauge
            }
            
            return family
            
        case .untyped:
            let family = Magma_Orc8r_MetricFamily.with {
                $0.name = familyName
                $0.metric = metrics
                $0.type = .untyped
            }
            
            return family
        }
        
        
    }
    
    private func CreateMetricsContainer(metricFamilyContainer : MagmaMetricFamilyContainer, gatewayID : String) -> Magma_Orc8r_MetricsContainer {
        let customMetricsContainer = Magma_Orc8r_MetricsContainer.with {
            $0.gatewayID = gatewayID
            $0.family = metricFamilyContainer
        }
        
        return customMetricsContainer
    }
    
}
