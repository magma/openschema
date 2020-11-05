const express = require('express')
var router = express.Router()

router.use('/', require('./ueRegistration'))
router.use('/', require('./qosScoring'))

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