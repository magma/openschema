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
import io.openschema.mma.data.entity.NetworkConnectionsEntity;

//TODO: javadoc
@Dao
public interface NetworkConnectionsDAO {

    @Query("SELECT * FROM network_connections")
    LiveData<List<NetworkConnectionsEntity>> getAll();

    @Insert
    void insert(NetworkConnectionsEntity newEntity);
}
