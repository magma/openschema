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

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

/**
 * Data access object used to interact with the Metrics' table in the database.
 */
@Dao
public interface MetricsDAO {
    /**
     * Returns a list of all metrics saved in the database.
     */
    @Query("SELECT * FROM metrics_entity")
    List<MetricsEntity> getAll();

    /**
     * Inserts a metric in the database.
     */
    @Insert
    void insert(MetricsEntity newMetric);

    /**
     * Deletes a list of metrics from the database.
     */
    @Delete
    void delete(MetricsEntity... metrics);
}
