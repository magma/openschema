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

package io.openschema.mma.id;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

/**
 *  Handles the unique ID generation and storage. Unique ID is a UUID and will be used as part of the
 *  registration and bootstrapping.
 */
public class Identity {

    private String mUUID;
    private String mPublicKey;

    public Identity(Context context)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidAlgorithmParameterException, NoSuchProviderException {
        mUUID = new UUID(context).getUUID();
        mPublicKey = new HardwareKey().getHwPublicKey();
        Log.d("MMA: TestIdentity", mUUID + "\n" + mPublicKey);
    }

    public String getUUID() {
        return mUUID;
    }

    public String getPublicKey() {
        return mPublicKey;
    }
}
