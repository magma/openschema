const express = require('express')
const _ = require('lodash')
const am = require('../utils/async-middleware').asyncMiddleware
var router = express.Router()

router.post('/register', am(async (req, res) => {
    // //Trim request body to only expected parameters
    // req.body = _.pick(req.body, ['uuid'])

    res.status(200).json({
        message: 'Registration was successful'
    })

    //TODO: Implement device registration with the new data lake
}))

module.exports = router