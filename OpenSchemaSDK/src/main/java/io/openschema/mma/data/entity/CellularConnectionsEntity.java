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

/**
 * Entity class used by Room to store network connections locally
 */
//TODO: javadocs
@Entity(tableName = "cellular_connections")
public class CellularConnectionsEntity extends NetworkConnectionsEntity {

    @ColumnInfo(name = "networkType")
    private String networkType;

    @ColumnInfo(name = "cellIdentity")
    private long cellIdentity;

    @Ignore
    public CellularConnectionsEntity(int transportType, String networkType, long cellIdentity, long duration, long usage, double longitude, double latitude, long timestamp) {
        this(0, transportType, networkType, cellIdentity, duration, usage, longitude, latitude, timestamp, false);
    }

    public CellularConnectionsEntity(int id, int transportType, String networkType, long cellIdentity, long duration, long usage, double longitude, double latitude, long timestamp, boolean reported) {
        super(id, transportType, duration, usage, longitude, latitude, timestamp, reported);
        this.networkType = networkType;
        this.cellIdentity = cellIdentity;
    }

    public String getNetworkType() { return networkType;}
    public long getCellIdentity() {return cellIdentity;}
}