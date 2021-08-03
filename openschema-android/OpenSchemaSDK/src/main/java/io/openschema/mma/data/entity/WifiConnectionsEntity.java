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
import androidx.room.Ignore;
import io.openschema.mma.data.pojo.Timestamp;

/**
 * Entity class used by Room to store network connections locally
 */
@Entity(tableName = "wifi_connections")
public class WifiConnectionsEntity extends NetworkConnectionsEntity {

    @ColumnInfo(name = "ssid")
    private String SSID;

    @ColumnInfo(name = "bssid")
    private String BSSID;

    @Ignore
    public WifiConnectionsEntity(int transportType, String SSID, String BSSID, double longitude, double latitude, long timestamp) {
        this(0, transportType, SSID, BSSID, longitude, latitude, timestamp, false);
    }

    public WifiConnectionsEntity(int id, int transportType, String SSID, String BSSID, double longitude, double latitude, long timestamp, boolean reported) {
        super(id, transportType, 0, 0, longitude, latitude, timestamp, reported);
        this.SSID = SSID;
        this.BSSID = BSSID;
    }

    public String getSSID() { return SSID;}
    public String getBSSID() {return BSSID;}
}