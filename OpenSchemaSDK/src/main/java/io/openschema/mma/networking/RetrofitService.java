/*
 * Copyright (c) 2020, The Magma Authors
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openschema.mma.networking;

import android.app.Application;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Helper class to setup Retrofit, either with a regular HTTP client or an unsafe client using
 * a self-signed certificate, and to retrieve the API declared in {@link BackendApi}. This class
 * works as a singleton and must be retrieved using {@link #getService(Context)}.
 */
public class RetrofitService {
    private static String TAG = "RetrofitService";

    //Singleton
    private static RetrofitService _instance = null;

    /**
     * Call to retrieve a {@link RetrofitService} object.
     */
    public static RetrofitService getService(Context appContext) {
        Log.d(TAG, "UI: Fetching RetrofitService");
        if (_instance == null) {
            synchronized (BackendApi.class) {
                if (_instance == null) {
                    if (appContext == null) {
                        Log.e(TAG, "UI: RetrofitService can't be instantiated with a null context");
                        return _instance;
                    }

                    if (!(appContext instanceof Application)) {
                        appContext = appContext.getApplicationContext();
                    }

                    _instance = new RetrofitService(appContext);
                }
            }
        }
        return _instance;
    }

    private BackendApi mApi = null;

    private RetrofitService(Context context) {
    }

    /**
     * Returns the instantiated interface with the previously declared HTTP calls. Requires {@link #initApi(Context, String, int, String, String) initApi()}
     * to have been called first.
     */
    public BackendApi getApi() { return mApi;}

    /**
     * Initialize the HTTP interface to be used with Retrofit. Can either use a safe HTTP client or an unsafe client
     * using a self-signed certificate.
     *
     * @param baseURL          Base URL to be used to make the HTTP calls.
     * @param certificateResId Resource ID for self-signed certificate. Use -1 to get a safe HTTP client instead.
     * @param username         Secret username used in the server's Basic Auth.
     * @param password         Secret password used in the server's Basic Auth.
     */
    public void initApi(Context context, String baseURL, SSLContext sslContext, String username, String password) {

        //Build credentials string for Basic Auth
        String basicCredentials = "Basic " + Base64.encodeToString((username + ":" + password).getBytes(), Base64.NO_WRAP);

        OkHttpClient httpClient = sslContext == null ?
                getSafeHttpClient(context, basicCredentials) :
                getUnsafeHttpClient(sslContext, basicCredentials);

        mApi = new Retrofit.Builder()
                .baseUrl(baseURL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BackendApi.class);
    }

    private OkHttpClient getSafeHttpClient(Context context, String credentials) {
        //Interceptor for including Basic Auth header in every request
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request req = chain.request().newBuilder().addHeader("Authorization", credentials).build();
                    return chain.proceed(req);
                })
                .build();
    }

    //Unsafe httpclient that accepts a server using a self-signed certificate
    private OkHttpClient getUnsafeHttpClient(SSLContext sslContext, String credentials) {
        //Interceptor for including Basic Auth header in every request
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request req = chain.request().newBuilder().addHeader("Authorization", credentials).build();
                    return chain.proceed(req);
                })
                .sslSocketFactory(sslContext.getSocketFactory()) //Overriding certificate verification for self-signed certificate
                .hostnameVerifier((hostname, session) -> true) //Overriding hostname verification
                .build();
    }
}