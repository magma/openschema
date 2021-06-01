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
import androidx.room.PrimaryKey;
import io.openschema.mma.data.pojo.Timestamp;

/**
 * Entity class used by Room to store network connections locally
 */
//TODO: javadocs
@Entity(tableName = "network_connections")
public class NetworkConnectionsEntity {
    /**
     * Autogenerated ID for Room database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int mId = 0;

    @ColumnInfo(name = "transport_type")
    public int mTransportType;

    @ColumnInfo(name = "longitude")
    public double mLongitude;

    @ColumnInfo(name = "latitude")
    public double mLatitude;

    @ColumnInfo(name = "timestamp")
    public Timestamp mTimeStamp;

    public NetworkConnectionsEntity(int transportType, double longitude, double latitude, Timestamp timeStamp) {
        mTransportType = transportType;
        mLongitude = longitude;
        mLatitude = latitude;
        mTimeStamp = timeStamp;
    }
}