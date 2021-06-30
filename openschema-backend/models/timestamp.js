const mongoose = require(`mongoose`)
const Schema = mongoose.Schema

//Timestamp representing the moment when the metric was collected by the client

let timestampSchema = new Schema({
    timestamp: Date,
    offsetMinutes: Number
}, {
    _id: false
})

module.exports = timestampSchema

//Date & time can be reconstructed to client's local time by using the offsetMinutes
//E.g.: dayjs(timestamp).utcOffset(offsetMinutes).format()

//TODO: remove
// let timestampMillis = 1621439106916
// let timeOffset = -300

// const dayjs = require('dayjs')
// dayjs.extend(require('dayjs/plugin/utc'))

// console.log(dayjs(timestampMillis).utc().format())
// console.log(dayjs(timestampMillis).utcOffset(timeOffset).format())