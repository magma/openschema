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
import androidx.room.PrimaryKey;

/**
 * Entity class used by Room to store network usage locally. Entries are logged based on each network connection.
 */
@Entity(tableName = "network_usage")
public class NetworkUsageEntity {
    /**
     * Autogenerated ID for Room database
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id = 0;

    @ColumnInfo(name = "transport_type")
    private int transportType;

    //Stored in milliseconds
    @ColumnInfo(name = "duration")
    private long duration;

    //Stored in bytes
    @ColumnInfo(name = "usage")
    private long usage;

    @ColumnInfo(name = "timestamp")
    private long timestamp;

    @Ignore
    public NetworkUsageEntity(int transportType, long duration, long usage, long timestamp) {
        this(0, transportType, duration, usage, timestamp);
    }

    /**
     * Constructor with all fields. Required for Android Room.
     */
    public NetworkUsageEntity(int id, int transportType, long duration, long usage, long timestamp) {
        this.id = id;
        this.transportType = transportType;
        this.duration = duration;
        this.usage = usage;
        this.timestamp = timestamp;
    }

    public int getId() { return id;}
    public int getTransportType() {return transportType;}
    public long getDuration() {return duration;}
    public long getUsage() {return usage;}
    public long getTimestamp() {return timestamp;}
}