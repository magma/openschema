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

package io.openschema.mma.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Room database to handle data used by the library.
 */
@Database(
        entities = {MetricsEntity.class},
        version = 1
)
@TypeConverters({
                        MetricsTypeConverter.class,
                        TimestampTypeConverter.class
                })
public abstract class MMADatabase extends RoomDatabase {
    public abstract MetricsDAO metricsDAO();

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
