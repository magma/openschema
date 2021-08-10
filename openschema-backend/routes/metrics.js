const express = require('express')
const _ = require('lodash')
const am = require('../utils/async-middleware').asyncMiddleware
const WifiSession = require('../models/wifi-session')
const CellularSession = require('../models/cellular-session')
const DeviceInfo = require('../models/device-info')
const ConnectionReport = require('../models/connection-report')
const UsageHourly = require('../models/usage-hourly')
const CustomMetric = require('../models/custom-metric')
var router = express.Router()


//TODO: add middleware to handle identifier information and make sure that the UE has been registered
router.use(function (req, res, next) {
    //Trim request body to expected parameters
    req.body = _.pick(req.body, ['metricName', 'metricsList', 'identifier', 'timestamp'])

    if (req.body.identifier.clientType === 'android') {
        //Processing request from ANDROID clients
        req.body.metrics = {}
        for (let i = 0; i < req.body.metricsList.length; i++) {
            const metricPair = req.body.metricsList[i];
            //Convert numerical strings into number values
            if (!isNaN(metricPair.second)) metricPair.second = Number(metricPair.second)
            //Convert Android Pair<String,String> into javascript object
            req.body.metrics[metricPair.first] = metricPair.second
        }
        delete req.body.metricsList
    }

    //TODO: implement other client types

    next()
})


router.post('/metrics/push', am(async (req, res) => {

    let metricHandler = checkKnownMetrics(req.body.metricName)

    if (await metricHandler(req.body)) {
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

//TODO: Abstract handlers back into each schema's module?
function checkKnownMetrics(metricName) {
    switch (metricName) {
        case WifiSession.metricName:
            return handleWifiSession
        case CellularSession.metricName:
            return handleCellularSession
        case DeviceInfo.metricName:
            return handleDeviceInfo
        case ConnectionReport.metricName:
            return handleConnectionReport
        case UsageHourly.metricName:
            return handleUsageHourly
        default:
            return handleCustomMetric
    }
}

async function handleWifiSession(body) {
    let newEntry = {
        metrics: WifiSession.preProcessMetrics(body.metrics),
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
        metrics: CellularSession.preProcessMetrics(body.metrics),
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
        metrics: body.metrics,
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

async function handleConnectionReport(body) {
    let newEntry = {
        metrics: ConnectionReport.preProcessMetrics(body.metrics),
        identifier: body.identifier,
        timestamp: body.timestamp
    }

    //TODO: remove
    console.log(newEntry)
    console.log(`Saving entry...`)

    let storedEntry = await new ConnectionReport.model(newEntry)
        .save()
        .catch(e => console.log('Error: ', e.message));

    return storedEntry != null
}

async function handleUsageHourly(body) {
    let newEntry = {
        metrics: body.metrics,
        identifier: body.identifier,
        timestamp: body.timestamp
    }

    //TODO: remove
    console.log(newEntry)
    console.log(`Saving entry...`)

    let storedEntry = await new UsageHourly.model(newEntry)
        .save()
        .catch(e => console.log('Error: ', e.message));

    return storedEntry != null
}

async function handleCustomMetric(body) {
    //TODO: implement custom metric handling
    console.log(`Error: Custom metric handling hasn't been implemented yet.`)
}