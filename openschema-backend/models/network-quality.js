const mongoose = require(`mongoose`)
const Schema = mongoose.Schema
const timestampSchema = require('./timestamp')
const identifierSchema = require('./identifier')

//OpenSchema baseline metrics use a standard metric name & schema
const openschemaMetricName = "openschemaNetworkQuality"

let metricsSchema = new Schema({
    transportType: {
        type: String,
        enum: ['wifi', 'cellular']
    },
    qualityScore: Number,
    latency: Number,
    rssi: Number
}, {
    _id: false
})

let networkQualitySchema = new Schema({
    metrics: metricsSchema,
    timestamp: timestampSchema,
    identifier: identifierSchema
}, {
    collection: openschemaMetricName
})

let model = mongoose.model(`NetworkQuality`, networkQualitySchema)

async function handleRequestBody(body) {
    let newEntry = {
        metrics: body.metrics,
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