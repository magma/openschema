const mongoose = require(`mongoose`)
const Schema = mongoose.Schema

let identifierSchema = new Schema({
    uuid: String,
    clientType: String
})

module.exports = identifierSchema