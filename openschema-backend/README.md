# OpenSchema Backend

### 1) Create an .env file

You can copy the `.env.template` file and fill the values required by each field.

* **MONGODB_URI** - URI linking to the MongoDB instance that will hold the data lake
* **AUTH_USERNAME** - username to be used for Basic Auth for access to this server's APIs
* **AUTH_PASSWORD** - password to be used for Basic Auth for access to this server's APIs


### 2) Create a X509 certificate and key to be used for HTTPS

Create a `keys/` folder and then add your files `server.crt` and `server.key` to it.