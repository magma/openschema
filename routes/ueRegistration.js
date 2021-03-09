const express = require('express')
const axios = require("axios");
const fs = require('fs')
const https = require("https")
const _ = require('lodash')
const am = require('../utils/asyncMiddleware').asyncMiddleware
var router = express.Router()

router.post('/register', am(async (req, res) => {
    //Trim request body to only expected parameters
    req.body = _.pick(req.body, ['uuid', 'publicKey'])

    let registrationResult = await sendRegister(req.body)
    if (registrationResult === REGISTRATION_SUCCESS) {
        res.status(200).json({
            message: 'Registration was successful'
        })
    } else if (registrationResult === REGISTRATION_DUPLICATE) {
        res.status(409).json({
            message: 'UUID is already registered'
        })
    } else {
        res.status(400).json({
            message: 'Registration failed'
        })
    }
}))

module.exports = router

const MAGMA_BASE_URL = process.env.MAGMA_BASE_URL
const MAGMA_NETWORK = process.env.MAGMA_NETWORK
const UE_BASE_ID = process.env.UE_BASE_ID

const customAxios = axios.create({
    baseURL: MAGMA_BASE_URL,
    httpsAgent: new https.Agent({
        pfx: fs.readFileSync(process.env.MAGMA_CERTIFICATE),
        passphrase: process.env.MAGMA_PASSPHRASE,
        rejectUnauthorized: false
    })
})

const REGISTRATION_SUCCESS = 0
const REGISTRATION_DUPLICATE = 1
const REGISTRATION_FAILED = 2

const sendRegister = async (ueData) => {
    let result = REGISTRATION_FAILED

    console.log(`Sending UE data to Magma...`)

    //Create request body
    let body = {
        description: "OpenSchema UE",
        device: {
            hardware_id: ueData.uuid,
            key: {
                key: ueData.publicKey,
                key_type: "SOFTWARE_ECDSA_SHA256"
            }
        },
        id: UE_BASE_ID + ueData.uuid.replace(/-/g, ""),
        magmad: {
            autoupgrade_enabled: true,
            autoupgrade_poll_interval: 300,
            checkin_interval: 60,
            checkin_timeout: 10,
        },
        name: `OpenSchema UE`,
        tier: "default"
    }

    //Send request
    try {
        const response = await customAxios.post(`networks/${MAGMA_NETWORK}/gateways`, body)
        console.log(`UE registration was successful`)
        result = REGISTRATION_SUCCESS
    } catch (error) {
        if (error.response) {
            console.log(error.response.data);

            //Currently the server responds with 400 BAD REQUEST. Comparing this string is the only way to determine if UE has already been registered.
            let duplicateMessage = `device ${body.device.hardware_id} is already mapped to gateway ${body.id}`
            if (duplicateMessage === error.response.data.message) {
                result = REGISTRATION_DUPLICATE
            }
        } else {
            console.log(error);
        }
    }

    return result
}