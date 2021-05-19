const mongoose = require(`mongoose`)
const Schema = mongoose.Schema
const timestampSchema = require('./timestamp')
const identifierSchema = require('./identifier')

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
    networkType: String
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

exports.model = mongoose.model(`CellularSession`, cellularSessionSchema)
exports.metricName = openschemaMetricName