const mongoose = require(`mongoose`)
const Schema = mongoose.Schema
const timestampSchema = require('./timestamp')
const identifierSchema = require('./identifier')

let customMetricSchema = new Schema({
    metricName: String,
    metrics: Schema.Types.Mixed,
    timestamp: timestampSchema,
    identifier: identifierSchema
})

module.exports = mongoose.model(`CustomMetric`, customMetricSchema)