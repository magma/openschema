# OpenSchema Backend

### 1) Create an .env file

You can copy the `.env.template` file and fill the values required by each field.

* **MAGMA_CERTIFICATE** - PFX file used to authenticate into the orc8r's APIs
* **MAGMA_PASSPHRASE** - passphrase used to authenticate into the orc8r's APIs
* **MAGMA_BASE_URL** - URL of the orc8r's APIs
* **MAGMA_NETWORK** - network name to be used internally in orc8r
* **UE_BASE_ID** - desired prefix to be used in UE's registered ID
* **AUTH_USERNAME** - username to be used for Basic Auth for access to this server's APIs
* **AUTH_PASSWORD** - password to be used for Basic Auth for access to this server's APIs


### 2) Create a X509 certificate and key to be used for HTTPS

Create a `keys/` folder and then add your files `server.crt` and `server.key` to it.