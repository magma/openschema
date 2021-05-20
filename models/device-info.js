const mongoose = require(`mongoose`)
const Schema = mongoose.Schema
const timestampSchema = require('./timestamp')
const identifierSchema = require('./identifier')

//OpenSchema baseline metrics use a standard metric name & schema
const openschemaMetricName = "openschemaDeviceInfo"

let metricsSchema = new Schema({
    osVersion: Number,
    model: String,
    manufacturer: String,
    brand: String,
    androidId: String
}, {
    _id: false
})

let deviceInfoSchema = new Schema({
    metrics: metricsSchema,
    timestamp: timestampSchema,
    identifier: identifierSchema
}, {
    collection: openschemaMetricName
})

exports.model = mongoose.model(`DeviceInfo`, deviceInfoSchema)
exports.metricName = openschemaMetricName