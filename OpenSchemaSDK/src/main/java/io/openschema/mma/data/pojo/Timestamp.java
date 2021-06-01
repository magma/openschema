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

package io.openschema.mma.data.pojo;

import android.util.Log;

import com.google.gson.annotations.SerializedName;

import java.util.TimeZone;

//TODO: javadocs
public class Timestamp {
    private static final String TAG = "Timestamp";

    @SerializedName("timestamp") private long mTimestamp;
    @SerializedName("offsetMinutes") private int mOffsetMinutes;

    public Timestamp(long timestampMillis, int offsetMinutes) {
        mTimestamp = timestampMillis;
        mOffsetMinutes = offsetMinutes;
    }

    public long getTimestampMillis() {return mTimestamp;}

    public static Timestamp getTimestampInstance() {
        Log.d(TAG, "MMA: Generating time metrics...");

        long timestamp = System.currentTimeMillis();
        TimeZone currentTimeZone = TimeZone.getDefault();

        return new Timestamp(timestamp,
                currentTimeZone.getRawOffset() / (60 * 1000)
        );
    }
}