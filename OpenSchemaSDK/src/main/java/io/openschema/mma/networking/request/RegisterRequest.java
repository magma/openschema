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

package io.openschema.mma.networking.request;

import com.google.gson.annotations.SerializedName;

/**
 * Class with the structure expected to be received in the OpenSchema's middle box registration API.
 */
public class RegisterRequest {
    @SerializedName("uuid") private String mUUID;
    @SerializedName("publicKey") private String mPublicKey;

    public RegisterRequest(String uuid, String publicKey) {
        mUUID = uuid;
        mPublicKey = publicKey;
    }
}