const express = require('express')
const _ = require('lodash')
const am = require('../utils/async-middleware').asyncMiddleware
const WifiSession = require('../models/wifi-session')
const CellularSession = require('../models/cellular-session')
const DeviceInfo = require('../models/device-info')
const ConnectionReport = require('../models/connection-report')
const UsageHourly = require('../models/usage-hourly')
const NetworkQuality = require('../models/network-quality')
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

//TODO: Change from switch/case to a map using metricName:Handler as key:value?
function checkKnownMetrics(metricName) {
    switch (metricName) {
        case WifiSession.metricName:
            return WifiSession.handleRequestBody
        case CellularSession.metricName:
            return CellularSession.handleRequestBody
        case DeviceInfo.metricName:
            return DeviceInfo.handleRequestBody
        case ConnectionReport.metricName:
            return ConnectionReport.handleRequestBody
        case UsageHourly.metricName:
            return UsageHourly.handleRequestBody
        case NetworkQuality.metricName:
            return NetworkQuality.handleRequestBody
        default:
            return CustomMetric.handleRequestBody
    }
}