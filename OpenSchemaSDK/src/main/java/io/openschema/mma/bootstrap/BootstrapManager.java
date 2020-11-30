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

package io.openschema.mma.bootstrap;

import android.util.Log;

import org.spongycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import androidx.annotation.WorkerThread;
import io.grpc.Channel;
import io.openschema.mma.bootstrapper.BootstrapperGrpc;
import io.openschema.mma.bootstrapper.Challenge;
import io.openschema.mma.certifier.Certificate;
import io.openschema.mma.helpers.ChannelHelper;
import io.openschema.mma.helpers.KeyHelper;
import io.openschema.mma.helpers.RandSByteString;
import io.openschema.mma.id.Identity;
import io.openschema.mma.identity.AccessGatewayID;
import io.openschema.mma.networking.CertificateManager;

/**
 * Class in charge of the Bootstrapping flow. This is required to start pushing metrics.
 */
public class BootstrapManager {

    private static final String TAG = "BootstrapManager";

    private Identity mIdentity;

    private BootstrapperGrpc.BootstrapperBlockingStub mBlockingStub;

    private boolean mBootstrapSuccess;

    public BootstrapManager(String controllerAddress, int controllerPort, SSLContext sslContext, Identity identity) {
        mIdentity = identity;

        Channel channel = ChannelHelper.getSecureManagedChannel(
                controllerAddress,
                controllerPort,
                sslContext.getSocketFactory());

        mBlockingStub = BootstrapperGrpc.newBlockingStub(channel);
    }

    /**
     * Execute the bootstrapping process in blocking mode. This will allow the client to collect metrics. This
     * operation can't be called from the main thread.
     */
    @WorkerThread
    public Certificate bootstrapSync()
            throws NoSuchAlgorithmException, IOException, OperatorCreationException, UnrecoverableKeyException, CertificateException, SignatureException, KeyStoreException, InvalidKeyException {

        Log.d(TAG, "MMA: Starting bootstrap process");

        AccessGatewayID hw_id = AccessGatewayID.newBuilder()
                .setId(mIdentity.getUUID())
                .build();

        // 1) get challenge
        Log.d(TAG, "MMA: Requesting challenge...");
        Challenge challenge = mBlockingStub.getChallenge(hw_id);
        RandSByteString rands = KeyHelper.getRandS(challenge);
        CertSignRequest csr = new CertSignRequest(KeyHelper.generateRSAKeyPairForAlias(CertificateManager.GATEWAY_KEY_ALIAS), mIdentity.getUUID());

        ChallengeResponse response = new ChallengeResponse(
                mIdentity.getUUID(),
                challenge,
                0,
                10000,
                csr.getCSRByteString(),
                rands.getR(),
                rands.getS());

        // 2) send CSR to sign
        Log.d(TAG, "MMA: Sending csr...");
        Certificate certificate = mBlockingStub.requestSign(response.getResponse());

        Log.d(TAG, "MMA: Bootstrapping was successful");
        mBootstrapSuccess = true;

        // 3) Add cert to keystore for mutual TLS and use for calling Collect() and Push()
//        storeSignedCertificate(certificate);
        return certificate;
    }

    public boolean wasBootstrapSuccessful() {
        return mBootstrapSuccess;
    }
}
