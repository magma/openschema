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

import com.google.protobuf.ByteString;

import org.spongycastle.openssl.PEMWriter;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.pkcs.PKCS10CertificationRequest;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyPair;

import io.openschema.mma.helpers.CsrHelper;

public class CertSignRequest {

    private PKCS10CertificationRequest mCsr;

    public CertSignRequest(KeyPair kp, String uuid)
            throws IOException, OperatorCreationException {
        mCsr = CsrHelper.generateCSR(kp, uuid);
    }

    public PKCS10CertificationRequest getCSR(){
        return mCsr;
    }

    public ByteString getCSRByteString() throws IOException {
        Log.d("TestCSR", mCsr.getEncoded().toString());
        return ByteString.copyFrom(mCsr.getEncoded());
    }
}
