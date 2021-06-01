const mongoose = require(`mongoose`)
const Schema = mongoose.Schema

let locationSchema = new Schema({
    longitude: Number,
    latitude: Number
}, {
    _id: false
})

module.exports = locationSchema