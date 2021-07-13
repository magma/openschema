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

package io.openschema.mma.utils;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.os.Build;
import android.telephony.TelephonyManager;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

/**
 * Utility class to extract network usage information using Android APIs.
 */
public class UsageRetriever {
    private static final String TAG = "UsageRetriever";
    private NetworkStatsManager mNetworkStatsManager = null;
    private String mSubscriberId = null;

    public UsageRetriever(Context ctx) {
        initNetworkManager(ctx);
    }

    @SuppressLint({"MissingPermission", "HardwareIds"})
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

    public long getDeviceTonnage(int transportType, long startTime, long endTime) {
        switch (transportType) {
            case NetworkCapabilities.TRANSPORT_WIFI:
                return getDeviceWifiTonnage(startTime, endTime);
            case NetworkCapabilities.TRANSPORT_CELLULAR:
                return getDeviceCellularTonnage(startTime, endTime);
            default:
                //Error
                return 0;
        }
    }

    public NetworkStats.Bucket getDeviceNetworkBucket(int transportType, long startTime, long endTime) {
        switch (transportType) {
            case NetworkCapabilities.TRANSPORT_WIFI:
                return getDeviceWifiBucket(startTime, endTime);
            case NetworkCapabilities.TRANSPORT_CELLULAR:
                return getDeviceCellularBucket(startTime, endTime);
            default:
                //Error
                return null;
        }
    }

    private long getDeviceWifiTonnage(long startTime, long endTime) {
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

    //TODO: need to be called from worker thread instead?
    private NetworkStats.Bucket getDeviceWifiBucket(long startTime, long endTime) {
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

    private long getDeviceCellularTonnage(long startTime, long endTime) {
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

    private NetworkStats.Bucket getDeviceCellularBucket(long startTime, long endTime) {
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

    public long getRxBytes(int transportType) {
        switch (transportType) {
            case NetworkCapabilities.TRANSPORT_WIFI:
                //TODO: test if data retrieved is correct. Seems to be gathering data even when wifi was OFF.
                return TrafficStats.getTotalRxBytes() - TrafficStats.getMobileRxBytes();
            case NetworkCapabilities.TRANSPORT_CELLULAR:
                return TrafficStats.getMobileRxBytes();
            default:
                //Error
                return 0;
        }
    }

    public long getTxBytes(int transportType) {
        switch (transportType) {
            case NetworkCapabilities.TRANSPORT_WIFI:
                return TrafficStats.getTotalTxBytes() - TrafficStats.getMobileTxBytes();
            case NetworkCapabilities.TRANSPORT_CELLULAR:
                return TrafficStats.getMobileTxBytes();
            default:
                //Error
                return 0;
        }
    }
}
