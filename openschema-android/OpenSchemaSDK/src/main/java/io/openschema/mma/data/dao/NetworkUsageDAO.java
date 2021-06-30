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

package io.openschema.mma.data.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import io.openschema.mma.data.entity.NetworkUsageEntity;

/**
 * Data access object used to interact with the optional Network usage table in the database.
 */
@Dao
public interface NetworkUsageDAO {

    @Insert
    void insert(NetworkUsageEntity newEntity);

    @Query("SELECT * from network_usage " +
                   "WHERE timestamp >= :startTime " +
                   "AND timestamp < :endTime")
    LiveData<List<NetworkUsageEntity>> getUsageEntities(long startTime, long endTime);

    @Query("SELECT * from network_usage " +
                   "WHERE timestamp >= :startTime " +
                   "AND timestamp < :endTime " +
                   "AND transport_type ==:transportType")
    LiveData<List<NetworkUsageEntity>> getUsageEntities(long startTime, long endTime, int transportType);
}
