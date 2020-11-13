const express = require(`express`)
const morgan = require(`morgan`);
const helmet = require(`helmet`);
const fs = require('fs')
const https = require('https')
require('dotenv').config() //Pull process.env values declared in .env

const routes = require("./routes");

const app = express()
const port = process.env.PORT

//Middleware
app.use(helmet()) //General HTTP security module
app.use(morgan('dev')) //Logger to trace requests received
app.use(express.json()) //Populate req.body from JSON body

//Our app routes
app.use(routes)

//Start listening using a self-signed certificate for HTTPS
https.createServer({
    key: fs.readFileSync('keys/server.key'),
    cert: fs.readFileSync('keys/server.crt')
  }, app)
  .listen(port, () => console.log(`App listening at https://localhost:${port}`))