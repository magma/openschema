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

package io.openschema.mma.networking.response;

import com.google.gson.annotations.SerializedName;

import org.json.JSONObject;

import okhttp3.ResponseBody;

public class BaseResponse {
    @SerializedName("message") private String mMessage;
    public String getMessage() { return mMessage; }

    private final static String ERROR_MESSAGE_FIELD = "message";
    public static String getErrorMessage(ResponseBody res) {
        String errorMessage = "";
        try {
            JSONObject jsonObject = new JSONObject(res.string());
            errorMessage = (String) jsonObject.get(ERROR_MESSAGE_FIELD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return errorMessage;
    }
}
