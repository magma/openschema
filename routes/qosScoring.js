const express = require('express')
var router = express.Router()

router.get('/score', (req, res) => {
    res.status(200).json({
        message: 'QoS Score API'
    })
})

module.exports = router