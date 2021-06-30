const mongoose = require(`mongoose`)
const Schema = mongoose.Schema
const timestampSchema = require('./timestamp')
const identifierSchema = require('./identifier')
const locationSchema = require('./location')

//OpenSchema baseline metrics use a standard metric name & schema
const openschemaMetricName = "openschemaConnectionReport"

//A challenge of the report is that we don't have a way to find 
// the source network session which generated it, 
// thus we need to store duplicate information
let wifiSchema = new Schema({
    ssid: String,
    bssid: String,
}, {
    _id: false
})

let cellularSchema = new Schema({
    networkType: String,
    cellId: Number,
}, {
    _id: false
})

let metricsSchema = new Schema({
    reportDescription: String,
    sessionStartTime: Date,
    transportType: {
        type: String,
        enum: ['wifi', 'cellular']
    },
    //TODO: need to validate the transport data to the above schemas
    transportData: Schema.Types.Mixed, //May either be wifiSchema or cellularSchema depending on transportType
    location: locationSchema
}, {
    _id: false
})

let connectionReportSchema = new Schema({
    metrics: metricsSchema,
    timestamp: timestampSchema,
    identifier: identifierSchema
}, {
    collection: openschemaMetricName
})

//Convert flat list of metrics into required nested structure
function preProcessMetrics(metrics) {
    if (metrics.longitude && metrics.latitude) {
        metrics.location = {
            longitude: metrics.longitude,
            latitude: metrics.latitude
        }
    }
    delete metrics.longitude
    delete metrics.latitude

    //TODO: move static values to const field
    if (metrics.transportType === 'wifi') {
        metrics.transportData = {
            ssid: metrics.ssid,
            bssid: metrics.bssid
        }
        delete metrics.ssid
        delete metrics.bssid
    } else if (metrics.transportType === 'cellular') {
        metrics.transportData = {
            networkType: metrics.networkType,
            cellId: metrics.cellId
        }
        delete metrics.networkType
        delete metrics.cellId
    }

    return metrics
}

exports.model = mongoose.model(`ConnectionReport`, connectionReportSchema)
exports.preProcessMetrics = preProcessMetrics
exports.metricName = openschemaMetricName