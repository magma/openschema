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

import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

import io.openschema.mma.bootstrapper.Challenge;
import io.openschema.mma.bootstrapper.Response;
import io.openschema.mma.certifier.CSR;
import io.openschema.mma.identity.AccessGatewayID;
import io.openschema.mma.identity.Identity;

/**
 * Response to challenge including CSR include:
 * - UUID
 * - Challenge
 * - CSR: Type, Id, Duration, CSR DER bytes
 * - r and s bytes
 */
public class ChallengeResponse {

    private Response mResponse;

    public ChallengeResponse(String uuid,
                    Challenge challenge,
                    int certType,
                    int duration,
                    ByteString csr,
                    ByteString r,
                    ByteString s) {

        mResponse = Response.newBuilder()
                .setHwId(AccessGatewayID.newBuilder()
                        .setId(uuid)
                        .build())
                .setChallenge(challenge.getChallenge())
                .setCsr(CSR.newBuilder()
                        .setCertTypeValue(certType)
                        .setId(Identity.newBuilder()
                                .setGateway(Identity.Gateway.newBuilder()
                                        .setHardwareId(uuid)
                                        .build())
                                .build())
                        .setValidTime(Duration.newBuilder()
                                .setSeconds(duration)
                                .setNanos(duration)
                                .build())
                        .setCsrDer(csr)
                        .build())
                .setEcdsaResponse(Response.ECDSA.newBuilder()
                        .setR(r)
                        .setS(s)
                        .build())
                .build();

    }

    public Response getResponse(){
        return mResponse;
    }

}
