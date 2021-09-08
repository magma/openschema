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

let model = mongoose.model(`UsageHourly`, usageHourlySchema)

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