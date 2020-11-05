const express = require(`express`)
const morgan = require(`morgan`);
const helmet = require(`helmet`);
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

//TODO: Create a self-signed certificate and use HTTPS instead
app.listen(port, () => {
  console.log(`App listening at http://localhost:${port}`)
})