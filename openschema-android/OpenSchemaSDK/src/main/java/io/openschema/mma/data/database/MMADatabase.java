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

package io.openschema.mma.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import io.openschema.mma.data.MetricsTypeConverter;
import io.openschema.mma.data.TimestampTypeConverter;
import io.openschema.mma.data.dao.HourlyUsageDAO;
import io.openschema.mma.data.dao.MetricsDAO;
import io.openschema.mma.data.dao.NetworkConnectionsDAO;
import io.openschema.mma.data.dao.NetworkQualityDAO;
import io.openschema.mma.data.dao.NetworkUsageDAO;
import io.openschema.mma.data.entity.CellularConnectionsEntity;
import io.openschema.mma.data.entity.HourlyUsageEntity;
import io.openschema.mma.data.entity.MetricsEntity;
import io.openschema.mma.data.entity.NetworkQualityEntity;
import io.openschema.mma.data.entity.NetworkUsageEntity;
import io.openschema.mma.data.entity.WifiConnectionsEntity;

/**
 * Room database to handle data used by the library.
 */
@Database(
        entities = {
                MetricsEntity.class,
                WifiConnectionsEntity.class,
                CellularConnectionsEntity.class,
                NetworkUsageEntity.class,
                HourlyUsageEntity.class,
                NetworkQualityEntity.class
        },
        version = 1
)
@TypeConverters({
                        MetricsTypeConverter.class,
                        TimestampTypeConverter.class
                })
public abstract class MMADatabase extends RoomDatabase {
    public abstract MetricsDAO metricsDAO();
    public abstract NetworkConnectionsDAO networkConnectionsDAO();
    public abstract NetworkUsageDAO networkUsageDAO();
    public abstract HourlyUsageDAO hourlyUsageDAO();
    public abstract NetworkQualityDAO networkQualityDAO();

    private static volatile MMADatabase _instance;

    /**
     * Call to retrieve a {@link MMADatabase} object.
     */
    public static MMADatabase getDatabase(final Context context) {
        if (_instance == null) {
            synchronized (MMADatabase.class) {
                if (_instance == null) {
                    _instance = Room.databaseBuilder(context.getApplicationContext(), MMADatabase.class, "mma_database")
                            .build();
                }
            }
        }
        return _instance;
    }
}
