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

    console.log(req.body)

    let ueRegistered = await sendRegister(req.body)
    if (ueRegistered) {
        res.status(200).json({
            message: 'Registered Successfully'
        })
    } else {
        //TODO: Return different responses depending on error received?
        res.status(400).json({
            message: 'Registration Failed'
        })
    }
}))

module.exports = router

const MAGMA_BASE_URL = process.env.MAGMA_BASE_URL
const MAGMA_NETWORK = process.env.MAGMA_NETWORK
const UE_BASE_ID = process.env.UE_BASE_ID

// const UE_ID_ERROR_NOT_INITIALIZED = -1
// const UE_ID_ERROR_NOT_FOUND = -2
// let ueCount = UE_ID_ERROR_NOT_INITIALIZED

const customAxios = axios.create({
    baseURL: MAGMA_BASE_URL,
    httpsAgent: new https.Agent({
        pfx: fs.readFileSync(process.env.MAGMA_CERTIFICATE),
        passphrase: process.env.MAGMA_PASSPHRASE,
        rejectUnauthorized: false
    })
})

const sendRegister = async (ueData) => {
    let success = false

    //Check if the count for IDs has already been initialized
    // if (ueCount == UE_ID_ERROR_NOT_INITIALIZED) {
    //     console.log(`UE count for ID has not been initialized`)
    //     ueCount = await getIdNumber()
    //     if (ueCount == UE_ID_ERROR_NOT_FOUND) {
    //         //Start counting IDs from 1 if none has been created
    //         ueCount = 0
    //     }
    // }

    console.log(`Sending UE data to Magma...`)

    //Create request body
    // let currentCount = ueCount + 1
    let body = {
        description: "OpenSchema UE",
        device: {
            hardware_id: ueData.uuid,
            key: {
                key: ueData.publicKey,
                key_type: "SOFTWARE_ECDSA_SHA256"
            }
        },
        // id: UE_BASE_ID + currentCount,
        id: UE_BASE_ID + ueData.uuid.replace(/-/g, ""),
        magmad: {
            autoupgrade_enabled: true,
            autoupgrade_poll_interval: 300,
            checkin_interval: 60,
            checkin_timeout: 10,
        },
        // name: `OpenSchema UE ${currentCount}`,
        name: `OpenSchema UE`,
        tier: "default"
    }

    //Send request
    try {
        const response = await customAxios.post(`networks/${MAGMA_NETWORK}/gateways`, body)
        // const data = response.data
        // console.log(data)
        console.log(`UE registration was successful`)
        success = true
        // ueCount++
    } catch (error) {
        if (error.response) {
            console.log(error.response.data);
        } else {
            console.log(error);
        }
    }

    return success
}

// const getIdNumber = async () => {
//     console.log(`Fetching gateway list from Magma...`)

//     let idNumber = UE_ID_ERROR_NOT_INITIALIZED
//     try {
//         //Get all the gateways in the network
//         const response = await customAxios.get(`networks/${MAGMA_NETWORK}/gateways`)
//         const data = response.data

//         //Find the last entry using our base ID
//         console.log(`Looking for the latest UE entry...`)
//         const lastKey = _.findLastKey(data, function (o) {
//             return o.id.startsWith(UE_BASE_ID)
//         })
//         if (lastKey) {
//             //Retrieve only the entry's number
//             console.log(`Extracting the latest id count...`)
//             idNumber = Number(lastKey.substring(UE_BASE_ID.length, lastKey.length))
//         } else {
//             //No gateway has been created with our base ID
//             console.log(`No entry found`)
//             idNumber = UE_ID_ERROR_NOT_FOUND
//         }
//     } catch (error) {
//         console.log(error.response.data);
//     }

//     return idNumber
// }