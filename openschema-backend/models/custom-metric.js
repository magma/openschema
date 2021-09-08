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

let model = mongoose.model(`CustomMetric`, customMetricSchema)

async function handleRequestBody(body) {
    //TODO: implement custom metric handling
    console.log(`Error: Custom metric handling hasn't been implemented yet.`)
}

exports.model = model
exports.handleRequestBody = handleRequestBody