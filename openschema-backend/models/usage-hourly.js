const mongoose = require(`mongoose`)
const Schema = mongoose.Schema
const timestampSchema = require('./timestamp')
const identifierSchema = require('./identifier')

//OpenSchema baseline metrics use a standard metric name & schema
const openschemaMetricName = "openschemaUsageHourly"

let metricsSchema = new Schema({
    transportType: {
        type: String,
        enum: ['wifi', 'cellular']
    },
    rxBytes: Number,
    txBytes: Number,
    segmentStartTime: Date
}, {
    _id: false
})

let usageHourlySchema = new Schema({
    metrics: metricsSchema,
    timestamp: timestampSchema,
    identifier: identifierSchema
}, {
    collection: openschemaMetricName
})

exports.model = mongoose.model(`UsageHourly`, usageHourlySchema)
exports.metricName = openschemaMetricName