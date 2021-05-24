const mongoose = require('mongoose')
const faker = require('faker')
const WifiSession = require('../models/wifi-session')

//Use a separate test DB to avoid polluting the data
const MONGODB_URI = `mongodb://localhost:27017/openschema_datalake_test`

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
async function createMockWiFiSessionData() {
    let dataCount = 10
    let newItems = []

    //Could hardcode to use a static UUID instead
    let uuid = faker.datatype.uuid()

    console.log(`Creating ${dataCount} mock entries...`)
    for (let i = 0; i < dataCount; i++) {

        //Create body to populate the mongoose schema
        let newItem = {
            metrics: {
                ssid: faker.animal.dog().replace(/\s+/g, ''), //Using dog breeds as SSID
                bssid: faker.internet.mac(),
                sessionStartTime: faker.time.recent(), //TODO: Should manipulate timestamps to model data better
                sessionDurationMillis: faker.datatype.number({ min: 5000, max: 3600000 }), //5s to 1h
                rxBytes: faker.datatype.number({ min: 3072, max: 1048576 }), //3KB to 1GB
                txBytes: faker.datatype.number({ min: 3072, max: 1048576 }), //3KB to 1GB
            },
            identifier: {
                clientType: 'android',
                uuid: uuid //Using a static UUID for all the mocks in this session
            },
            timestamp: {
                offsetMinutes: -300,
                timestamp: faker.time.recent(), //TODO: Should manipulate timestamps to model data better
            }
        }

        // console.log(newItem)
        newItems.push(newItem)
    }

    console.log(`Pushing entries to DB...`)
    await WifiSession.model.create(newItems)
}