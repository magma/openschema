const express = require('express')
const _ = require('lodash')
const am = require('../utils/asyncMiddleware').asyncMiddleware
const WifiSession = require('../models/wifiSession')
const CellularSession = require('../models/cellularSession')
const DeviceInfo = require('../models/deviceInfo')
const CustomMetric = require('../models/customMetric')
var router = express.Router()


//TODO: add middleware to handle identifier and compare with registered devices
router.post('/metrics/push', am(async (req, res) => {

    //Trim request body to expected parameters
    req.body = _.pick(req.body, ['metricName', 'metricsList', 'identifier', 'timestamp'])

    let metricHandler = checkKnownMetrics(req.body.metricName)
    let pushResult = await metricHandler(req.body)

    if (pushResult) {

        res.status(200).json({
            message: `Metric was stored successfully`
        })
    } else {
        res.status(400).json({
            message: `Metric failed to be stored`
        })

    }
}))

module.exports = router

//TODO: Abstract handlers into a new layer?
function checkKnownMetrics(metricName) {
    switch (metricName) {
        case WifiSession.metricName:
            return handleWifiSession
        case CellularSession.metricName:
            return handleCellularSession
        case DeviceInfo.metricName:
            return handleDeviceInfo
        default:
            return handleCustomMetric
    }
}

//TODO: convert to middleware
function processMetricsList(metricsList) {
    let metricsBody = {}
    for (let i = 0; i < metricsList.length; i++) {
        const metricPair = metricsList[i];
        if (!isNaN(metricPair.second)) metricPair.second = Number(metricPair.second)
        metricsBody[metricPair.first] = metricPair.second
    }
    return metricsBody
}

async function handleWifiSession(body) {
    let newEntry = {
        metrics: processMetricsList(body.metricsList),
        identifier: body.identifier,
        timestamp: body.timestamp
    }

    //TODO: remove
    console.log(newEntry)
    console.log(`Saving entry...`)

    let storedEntry = await new WifiSession.model(newEntry)
        .save()
        .catch(e => console.log('Error: ', e.message));

    return storedEntry != null
}

async function handleCellularSession(body) {
    let newEntry = {
        metrics: processMetricsList(body.metricsList),
        identifier: body.identifier,
        timestamp: body.timestamp
    }

    //TODO: remove
    console.log(newEntry)
    console.log(`Saving entry...`)

    let storedEntry = await new CellularSession.model(newEntry)
        .save()
        .catch(e => console.log('Error: ', e.message));

    return storedEntry != null
}

async function handleDeviceInfo(body) {
    let newEntry = {
        metrics: processMetricsList(body.metricsList),
        identifier: body.identifier,
        timestamp: body.timestamp
    }

    //TODO: remove
    console.log(newEntry)
    console.log(`Saving entry...`)

    let storedEntry = await new DeviceInfo.model(newEntry)
        .save()
        .catch(e => console.log('Error: ', e.message));

    return storedEntry != null
}

async function handleCustomMetric(body) {
    //TODO: implement custom metric handling
}