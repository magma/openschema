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

import java.util.List;

import androidx.core.util.Pair;
import io.openschema.mma.data.pojo.Timestamp;

/**
 * Class with the structure expected to be received in the OpenSchema's middle box registration API.
 */
public class MetricsPushRequest {

    @SerializedName("metricName") private String mMetricName;
    @SerializedName("metricsList") public List<Pair<String, String>> mMetricsList;
    @SerializedName("identifier") private Identifier mIdentifier;
    @SerializedName("timestamp") private Timestamp mTimestamp;

    public MetricsPushRequest(String metricName, List<Pair<String, String>> metricsList, String uuid, Timestamp timestamp) {
        mMetricName = metricName;
        mMetricsList = metricsList;
        mIdentifier = new Identifier(uuid);
        mTimestamp = timestamp;
    }

    private class Identifier {
        @SerializedName("uuid") private String mUUID;
        @SerializedName("clientType") private String mClientType;

        Identifier(String uuid) {
            mUUID = uuid;
            mClientType = "android";
        }
    }
}