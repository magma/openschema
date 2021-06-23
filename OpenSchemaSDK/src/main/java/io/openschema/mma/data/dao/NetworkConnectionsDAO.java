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
import io.openschema.mma.data.entity.CellularConnectionsEntity;
import io.openschema.mma.data.entity.WifiConnectionsEntity;

/**
 * Data access object used to interact with the optional Network connections' table in the database.
 */
@Dao
public interface NetworkConnectionsDAO {

    //Wi-Fi calls
    @Insert
    void insert(WifiConnectionsEntity newEntity);

    @Query("SELECT * FROM wifi_connections")
    LiveData<List<WifiConnectionsEntity>> getAllWifiConnections();

    @Query("UPDATE wifi_connections SET is_reported = 1 WHERE id=:id")
    void setWifiReported(int id);

    //Cellular calls
    @Query("SELECT * FROM cellular_connections")
    LiveData<List<CellularConnectionsEntity>> getAllCellularConnections();

    @Insert
    void insert(CellularConnectionsEntity newEntity);

    @Query("UPDATE cellular_connections SET is_reported = 1 WHERE id=:id")
    void setCellularReported(int id);
}
