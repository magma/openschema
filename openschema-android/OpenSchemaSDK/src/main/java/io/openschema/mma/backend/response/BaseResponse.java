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

package io.openschema.mma.backend.response;

import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import okhttp3.ResponseBody;

/**
 * Base class to be extended by other response types received from the OpenSchema middle box. Every response from the OpenSchema middle box contains at least a "message" field.
 */
public class BaseResponse {
    private final static String MESSAGE_FIELD = "message";

    @SerializedName(MESSAGE_FIELD) private String mMessage;

    /**
     * Retrieve the message from a response received from the OpenSchema middle box.
     */
    public String getMessage() { return mMessage; }

    /**
     * Helper method used to retrieve the error message from a response received from the OpenSchema middle box.
     */
    public static String getErrorMessage(ResponseBody res) {
        String errorMessage = "";
        try {
            JSONObject jsonObject = new JSONObject(res.string());
            errorMessage = (String) jsonObject.get(MESSAGE_FIELD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return errorMessage;
    }
}
