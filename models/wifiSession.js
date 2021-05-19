const mongoose = require(`mongoose`)
const Schema = mongoose.Schema
const timestampSchema = require('./timestamp')
const identifierSchema = require('./identifier')

//OpenSchema baseline metrics use a standard metric name & schema
const openschemaMetricName = "openschemaWifiSession"

let metricsSchema = new Schema({
    rxBytes: Number,
    txBytes: Number,
    sessionStartTime: Date,
    sessionDurationMillis: Number,
    ssid: String,
    bssid: String,
}, {
    _id: false
})

let wifiSessionSchema = new Schema({
    metrics: metricsSchema,
    timestamp: timestampSchema,
    identifier: identifierSchema
}, {
    collection: openschemaMetricName
})


exports.model = mongoose.model(`WifiSession`, wifiSessionSchema)
exports.metricName = openschemaMetricName