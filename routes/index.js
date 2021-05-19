const express = require('express')
const basicAuth = require('express-basic-auth')
var router = express.Router()

router.use(basicAuth({
    users: {
        [process.env.AUTH_USERNAME]: process.env.AUTH_PASSWORD
    },
    unauthorizedResponse: {
        message: `You are not authorized`
    }
}))

router.use('/', require('./ueRegistration'))
// router.use('/', require('./qosScoring'))
router.use('/', require('./pushMetric'))

//Error handling middleware
router.use(function (err, req, res, next) {
    if (process.env.NODE_ENV === 'development') console.error(`ERROR: ${err.stack}`)

    if (err.status === 401) {
        res.status(err.status)
            .json({
                message: `Not authorized`
            })
    } else {
        res.status(err.status || 500)
            .json({
                message: `An error occurred`
            })
    }
})

module.exports = router