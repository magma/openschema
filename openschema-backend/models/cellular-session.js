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
    cellId: Number,
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

let model = mongoose.model(`CellularSession`, cellularSessionSchema)

async function handleRequestBody(body) {
    let newEntry = {
        metrics: preProcessMetrics(body.metrics),
        identifier: body.identifier,
        timestamp: body.timestamp
    }

    //TODO: remove
    console.log(newEntry)
    console.log(`Saving entry...`)

    let storedEntry = await new model(newEntry)
        .save()
        .catch(e => console.log('Error: ', e.message));

    return storedEntry != null
}

exports.model = model
exports.handleRequestBody = handleRequestBody
exports.metricName = openschemaMetricName

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