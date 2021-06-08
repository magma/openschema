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

package io.openschema.mma.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import io.openschema.mma.data.pojo.Timestamp;

/**
 * Entity class used by Room to store network connections locally
 */
//TODO: javadocs
@Entity(tableName = "wifi_connections")
public class WifiConnectionsEntity extends NetworkConnectionsEntity {

    @ColumnInfo(name = "ssid")
    public String mSSID;

    @ColumnInfo(name = "bssid")
    public String mBSSID;

    public WifiConnectionsEntity(int transportType, String mSSID, String mBSSID, long duration, long usage, double longitude, double latitude, long timeStamp) {
        super(transportType, duration, usage, longitude, latitude, timeStamp);

        //Adding m to parameters due to Room not matching them correctly to the attributes
        this.mSSID = mSSID;
        this.mBSSID = mBSSID;
    }
}