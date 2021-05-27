const mongoose = require(`mongoose`)
const Schema = mongoose.Schema
const timestampSchema = require('./timestamp')
const identifierSchema = require('./identifier')
const locationSchema = require('./location')

//OpenSchema baseline metrics use a standard metric name & schema
const openschemaMetricName = "openschemaCellularSession"

let metricsSchema = new Schema({
    rxBytes: Number,
    txBytes: Number,
    sessionStartTime: Date,
    sessionDurationMillis: Number,
    carrierName: String,
    mobileNetworkCode: String,
    mobileCountryCode: String,
    isoCountryCode: String,
    networkType: String,
    location: locationSchema
}, {
    _id: false
})

let cellularSessionSchema = new Schema({
    metrics: metricsSchema,
    timestamp: timestampSchema,
    identifier: identifierSchema
}, {
    collection: openschemaMetricName
})

//Convert flat list of metrics into required nested structure
function preProcessMetrics(metrics){
    if (metrics.longitude && metrics.latitude) {
        metrics.location = {
            longitude: metrics.longitude,
            latitude: metrics.latitude
        }
    }
    delete metrics.longitude
    delete metrics.latitude
    return metrics
}

exports.model = mongoose.model(`CellularSession`, cellularSessionSchema)
exports.preProcessMetrics = preProcessMetrics
exports.metricName = openschemaMetricName