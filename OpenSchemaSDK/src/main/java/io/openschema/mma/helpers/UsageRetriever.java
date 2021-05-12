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

package io.openschema.mma.helpers;

import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.telephony.TelephonyManager;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

//TODO: javadocs
public class UsageRetriever {
    private static final String TAG = "UsageRetriever";
    private NetworkStatsManager mNetworkStatsManager = null;
    private String mSubscriberId = null;

    public UsageRetriever(Context ctx) {
        initNetworkManager(ctx);
    }

    private void initNetworkManager(Context ctx) {
        try {
            AppOpsManager appOps = (AppOpsManager) ctx.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS, ctx.getApplicationInfo().uid, ctx.getPackageName());
            if (mode == MODE_ALLOWED) {
                mNetworkStatsManager = (NetworkStatsManager) ctx.getSystemService(ctx.NETWORK_STATS_SERVICE);

                //On Android 9 and onwards, querying TRANSPORT_CELLULAR with a null SubscriberID will fetch ALL cellular traffic
                //This means it's no longer possible to relate traffic to each SIM card on a dual SIM phone
                if (Build.VERSION.SDK_INT < 28) {
                    TelephonyManager manager = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
                    mSubscriberId = manager.getSubscriberId();
                }
            } else {
                mNetworkStatsManager = null;
            }
        } catch (SecurityException e) {
            android.util.Log.e(TAG, "MMA: Missing required Usage Access permissions");
            e.printStackTrace();
            mNetworkStatsManager = null;
            mSubscriberId = null;
        }
    }

    public long getDeviceWifiTonnage(long startTime, long endTime) {
        if (mNetworkStatsManager != null) {
            NetworkStats.Bucket wifiBucket = null;

            try {
                wifiBucket = mNetworkStatsManager.querySummaryForDevice(NetworkCapabilities.TRANSPORT_WIFI, null, startTime, endTime);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (wifiBucket != null) {
                return (wifiBucket.getRxBytes() + wifiBucket.getTxBytes());
            }
        } else {
            android.util.Log.e(TAG, "MMA: Missing required Usage Access permissions");
        }

        //Error
        return 0;
    }

    public NetworkStats.Bucket getDeviceWifiBucket(long startTime, long endTime) {
        if (mNetworkStatsManager != null) {
            NetworkStats.Bucket wifiBucket = null;

            try {
                wifiBucket = mNetworkStatsManager.querySummaryForDevice(NetworkCapabilities.TRANSPORT_WIFI, null, startTime, endTime);
            } catch (Exception e) {
                e.printStackTrace();
            }


            return wifiBucket;
        } else {
            android.util.Log.e(TAG, "MMA: Missing required Usage Access permissions");
        }

        //Error
        return null;
    }

    public long getDeviceCellularTonnage(long startTime, long endTime) {
        if (mNetworkStatsManager != null &&
                !(Build.VERSION.SDK_INT < 28 && (mSubscriberId == null || mSubscriberId.equals("")))) {
            NetworkStats.Bucket cellBucket = null;

            try {
                cellBucket = mNetworkStatsManager.querySummaryForDevice(NetworkCapabilities.TRANSPORT_CELLULAR, mSubscriberId, startTime, endTime);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (cellBucket != null) {
                return (cellBucket.getRxBytes() + cellBucket.getTxBytes());
            }
        } else {
            android.util.Log.e(TAG, "MMA: Missing required Usage Access permissions");
        }

        //Error
        return 0;
    }

    public NetworkStats.Bucket getDeviceCellularBucket(long startTime, long endTime) {
        if (mNetworkStatsManager != null &&
                !(Build.VERSION.SDK_INT < 28 && (mSubscriberId == null || mSubscriberId.equals("")))) {
            NetworkStats.Bucket cellBucket = null;

            try {
                cellBucket = mNetworkStatsManager.querySummaryForDevice(NetworkCapabilities.TRANSPORT_CELLULAR, mSubscriberId, startTime, endTime);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return cellBucket;
        } else {
            android.util.Log.e(TAG, "MMA: Missing required Usage Access permissions");
        }

        //Error
        return null;
    }
}
