const mongoose = require('mongoose')
const faker = require('faker')
const WifiSession = require('../models/wifi-session')

//Use a separate test DB to avoid polluting the data
const MONGODB_URI = `mongodb://localhost:27017/openschema_datalake_test`

//Define Arrays for users
let uuids = []
let carriers = ["Claro", "Claro", "Claro", "T-Mobile", "T-Mobile", "T-Mobile"]
let isoCountryCodes = ["pe", "pe", "pe", "us", "us", "us"]
let mobileNetworkCodes = ["17", "17", "17", "260", "260", "260"]
let mobileCountryCodes = ["716", "716", "716", "310", "310", "310"]
let networkTypes = ["4G", "4G", "4G", "4G", "5G", "5G"]
let latitudes = [-16.390332, -12.105356, -12.091066, 33.772516, 33.932614, 28.511954]
let longitudes = [-71.549965, -76.964025, -77.066721, -118.193372, -118.379157, -81.381235]
let countries = ["PE", "PE", "PE", "US", "US", "US"]
let cities = ["Arequipa", "Lima", "Lima", "Long Beach", "Los Angeles", "Orlando"]

//Create DB handler
mongoose.Promise = global.Promise
mongoose.connect(MONGODB_URI, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
    useCreateIndex: true
})

//Open DB connection
let db = mongoose.connection
db.on('error', console.error.bind(console, 'connection error:'))
db.once(`open`, async () => {
    console.log('MongoDB connected!')
    try {
        //Run any code needed for creating mockup data
        console.log(`Creating mock data...`)
        await createMockWiFiSessionData()
        console.log(`Finished writing mock data...`)
    } catch (e) {
        console.log(e.message)
    } finally {
        mongoose.connection.close()
    }
})

//Other functions to run

function createUsers(nUsers) {

    for (let i = 0; i < nUsers; i++) {
        let uuid = "uuid-user-" + i
        uuids.push(uuid)
    }

}

function getRandomCoordinates(radius, uniform) {
    // Generate two random numbers
    var a = Math.random(), b = Math.random()

    // Flip for more uniformity.
    if (uniform) {
        if (b < a) {
            var c = b
            b = a
            a = c
        }
    }

    // It's all triangles.
    return [
        b * radius * Math.cos(2 * Math.PI * a / b),
        b * radius * Math.sin(2 * Math.PI * a / b)
    ]
}

function getRandomLocation(latitude, longitude, radiusInMeters) {

    var randomCoordinates = getRandomCoordinates(radiusInMeters, true);

    // Earths radius in meters via WGS 84 model.
    var earth = 6378137;

    // Offsets in meters.
    var northOffset = randomCoordinates[0], eastOffset = randomCoordinates[1]

    // Offset coordinates in radians.
    var offsetLatitude = northOffset / earth,
        offsetLongitude = eastOffset / (earth * Math.cos(Math.PI * (latitude / 180)));

    // Offset position in decimal degrees.
    return {
        latitude: latitude + (offsetLatitude * (180 / Math.PI)),
        longitude: longitude + (offsetLongitude * (180 / Math.PI))
    }
}

async function createMockWiFiSessionData() {
    let nUsers = 6
    createUsers(nUsers)
    let dataCount = Math.floor(Math.random() * 11); 
    let newItems = []

    console.log(`Creating ${nUsers * dataCount} mock entries...`)

    for (let i = 0; i < nUsers; i++) {
        for (let j = 0; j < dataCount; j++) {

            let coordinates = getRandomLocation(latitudes[i], longitudes[i], 500)

            //Create body to populate the mongoose schema
            let newItem = {
                metrics: {
                    carrierName: carriers[i],
                    isoCountryCode: isoCountryCodes[i],
                    networkType: networkTypes[i],
                    mobileNetworkCode: mobileNetworkCodes[i],
                    mobileCountryCode: mobileCountryCodes[i],
                    sessionStartTime: faker.time.recent(), //TODO: Should manipulate timestamps to model data better
                    sessionDurationMillis: faker.datatype.number({ min: 5000, max: 3600000 }), //5s to 1h
                    rxBytes: faker.datatype.number({ min: 3072, max: 1048576 }), //3KB to 1GB
                    txBytes: faker.datatype.number({ min: 3072, max: 1048576 }), //3KB to 1GB
                    location: {
                        latitude: coordinates.latitude,
                        longitude: coordinates.longitude,
                        country: countries[i],
                        city: cities[i],
                    }
                },
                identifier: {
                    clientType: 'android',
                    uuid: uuids[i]
                },
                timestamp: {
                    offsetMinutes: -300,
                    timestamp: faker.time.recent(), //TODO: Should manipulate timestamps to model data better
                }
            }

            console.log(newItem)
            newItems.push(newItem)
        }
    }

    console.log(`Pushing entries to DB...`)
    await WifiSession.model.create(newItems)
}