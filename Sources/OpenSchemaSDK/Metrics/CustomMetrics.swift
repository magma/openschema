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
    
    public init() {
        
    }
    
    public func CreateLabelPair(labelName : String, labelValue : String) -> Magma_Orc8r_LabelPair {
        let label : Magma_Orc8r_LabelPair = Magma_Orc8r_LabelPair.with {
            $0.name = labelName
            $0.value = labelValue
        }
        
        return label
    }
    
    public func CreateSimpleMetric(familyName : String, LabelContainer : [(labelName: String, labelValue: String)], metricValue: Double) -> Bool {
        
        let customMetric = NSEntityDescription.insertNewObject(forEntityName: "CustomMetric", into: coreDataController.managedObjectContext) as! CustomMetric
        customMetric.familyName = familyName
        customMetric.timestamp = String(Date().millisecondsSince1970)
        customMetric.value = metricValue
        
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
    
    public func CreateSimpleMetric(simpleMetricType : SimpleMetricType, labelContainer : MagmaLabelContainer, value : Double) -> Magma_Orc8r_Metric {
        
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
    
    public func CreateFamilyForSimpleMetric(simpleMetricType: SimpleMetricType, metrics : MagmaMetricContainer, familyName: String) -> Magma_Orc8r_MetricFamily {
        
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
    
    public func CreateMetricsContainer(metricFamilyContainer : MagmaMetricFamilyContainer, gatewayID : String) -> Magma_Orc8r_MetricsContainer {
        let customMetricsContainer = Magma_Orc8r_MetricsContainer.with {
            $0.gatewayID = gatewayID
            $0.family = metricFamilyContainer
        }
        
        return customMetricsContainer
    }
    
}
