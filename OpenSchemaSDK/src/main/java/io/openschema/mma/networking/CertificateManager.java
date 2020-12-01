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

import android.content.Context;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import io.openschema.mma.bootstrap.BootstrapManager;

/**
 * Class in charge of handling all the custom self-signed certificates used in the metrics agent flow.
 */
//TODO: Fix all the KeyStore exceptions that are being generated
public class CertificateManager {

    private static final String CERT_TYPE = "X.509";
    private static final String KEY_STORE_TYPE = "AndroidKeyStore";

    public static final String CONTROLLER_CERTIFICATE_ALIAS = "controller";
    public static final String BACKEND_CERTIFICATE_ALIAS = "backend";
    public static final String GATEWAY_KEY_ALIAS = "gateway";

    private KeyStore mKeyStore;

    public CertificateManager() {
        try {
            mKeyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            mKeyStore.load(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add the custom certificate used by the Magma controller to the KeyStore.
     *
     * @param certificateResId Resource ID of the raw certificate file.
     */
    public void addControllerCertificate(Context context, int certificateResId) {
        addCertificate(context, CONTROLLER_CERTIFICATE_ALIAS, certificateResId);
    }

    /**
     * Add the custom certificate used by the middle box to the KeyStore.
     *
     * @param certificateResId Resource ID of the raw certificate file.
     */
    public void addBackendCertificate(Context context, int certificateResId) {
        addCertificate(context, BACKEND_CERTIFICATE_ALIAS, certificateResId);
    }

    /**
     * Add the custom certificate received after completing the bootstrapping process to the KeyStore.
     *
     * @param certificate Certificate received from calling {@link BootstrapManager#bootstrapSync()}
     */
    public void addBootstrapCertificate(io.openschema.mma.certifier.Certificate certificate) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance(CERT_TYPE);
            final Certificate cert = cf.generateCertificate(certificate.getCertDer().newInput());

            Certificate[] certChain = new Certificate[1];
            certChain[0] = cert;
            PrivateKey privateKey = (PrivateKey) mKeyStore.getKey(GATEWAY_KEY_ALIAS, null);
            mKeyStore.setKeyEntry(GATEWAY_KEY_ALIAS, privateKey, null, certChain);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addCertificate(Context context, String certificateAlias, int certificateResId) {
        //Load our self-signed certificate
        try {
            CertificateFactory cf = CertificateFactory.getInstance(CERT_TYPE);
            InputStream certificate = context.getResources().openRawResource(certificateResId);
            Certificate cert = cf.generateCertificate(certificate);
            certificate.close();

            mKeyStore.setCertificateEntry(certificateAlias, cert);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate an SSLContext that contains all the previously added certificates.
     */
    public SSLContext generateSSLContext() {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(mKeyStore);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(mKeyStore, "".toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            return sslContext;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
