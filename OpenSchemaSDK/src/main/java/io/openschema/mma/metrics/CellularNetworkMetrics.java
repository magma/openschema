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

package io.openschema.mma.metrics;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityNr;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

/**
 * Collects metrics related to cellular networks.
 */
public class CellularNetworkMetrics {

    private static final String TAG = "CellularNetworkMetrics";

    /**
     * Metric family name to be used for the collected cellular information.
     */
    public static final String METRIC_FAMILY_NAME = "openschema_android_cellular_network_info";

    private static final String METRIC_CARRIER_NAME = "carrier_name";
    private static final String METRIC_MOBILE_NETWORK_CODE = "mobile_network_code";
    private static final String METRIC_MOBILE_COUNTRY_CODE = "mobile_country_code";
    private static final String METRIC_ISO_COUNTRY_CODE = "iso_country_code";
    private static final String METRIC_RADIO_TECHNOLOGY_CODE = "radio_technology_code";

    private final Context mContext;
    private final TelephonyManager mTelephonyManager;

    public CellularNetworkMetrics(Context context) {
        mContext = context;
        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * Collects information about available cellular networks and generates a list of pairs to
     * be used in {@link MetricsManager#collect(String, List)}.
     */
    public List<Pair<String, String>> retrieveNetworkMetrics() {
        Log.d(TAG, "MMA: Generating cellular network metrics...");

        //TODO: check if SIM is available?
        //TODO: check if airplane mode?
        List<Pair<String, String>> metricsList = new ArrayList<>();

        metricsList.add(new Pair<>(METRIC_CARRIER_NAME, mTelephonyManager.getNetworkOperatorName()));
        metricsList.add(new Pair<>(METRIC_ISO_COUNTRY_CODE, mTelephonyManager.getNetworkCountryIso()));
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            metricsList.add(new Pair<>(METRIC_RADIO_TECHNOLOGY_CODE, getRadioTechnologyString(mTelephonyManager.getDataNetworkType())));
        }

        //TODO: Also check if location services are enabled?
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Requires location services to be enabled, otherwise returned list will be empty
            List<CellInfo> allCellInfo = mTelephonyManager.getAllCellInfo();

            if (allCellInfo != null) {
                for (int i = 0; i < allCellInfo.size(); i++) {
                    CellInfo cellInfo = allCellInfo.get(i);

                    if (cellInfo == null || !cellInfo.isRegistered()) continue;
                    //TODO: Can there be more than 1 registered network?

                    if (cellInfo instanceof CellInfoCdma) {
                        getInfoCDMA(metricsList);
                    } else if (cellInfo instanceof CellInfoGsm) {
                        getInfoGSM(metricsList, cellInfo);
                    } else if (cellInfo instanceof CellInfoLte) {
                        getInfoLTE(metricsList, cellInfo);
                    } else if (cellInfo instanceof CellInfoWcdma) {
                        getInfoWCDMA(metricsList, cellInfo);
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            cellInfo instanceof CellInfoNr) {
                        getInfoNR(metricsList, cellInfo);
                    }
                }
            }
        }

//        Log.d(TAG, "MMA: Collected metrics:\n"+metricsList.toString());
        return metricsList;
    }

    /**
     * Collects the mobile network code & mobile country code from a CDMA cell.
     */
    private void getInfoCDMA(List<Pair<String, String>> metricsList) {
        String networkOperator = mTelephonyManager.getNetworkOperator();
        if (networkOperator != null) {
            metricsList.add(new Pair<>(METRIC_MOBILE_NETWORK_CODE, networkOperator.substring(3)));
            metricsList.add(new Pair<>(METRIC_MOBILE_COUNTRY_CODE, networkOperator.substring(0, 3)));
        }
    }

    /**
     * Collects the mobile network code & mobile country code from a GSM cell.
     */
    private void getInfoGSM(List<Pair<String, String>> metricsList, CellInfo cellInfo) {
        CellInfoGsm info = (CellInfoGsm) cellInfo;
        CellIdentityGsm cellIdentity = info.getCellIdentity();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            metricsList.add(new Pair<>(METRIC_MOBILE_NETWORK_CODE, cellIdentity.getMncString()));
            metricsList.add(new Pair<>(METRIC_MOBILE_COUNTRY_CODE, cellIdentity.getMccString()));
        } else {
            metricsList.add(new Pair<>(METRIC_MOBILE_NETWORK_CODE, Integer.toString(cellIdentity.getMnc())));
            metricsList.add(new Pair<>(METRIC_MOBILE_COUNTRY_CODE, Integer.toString(cellIdentity.getMcc())));
        }
    }

    /**
     * Collects the mobile network code & mobile country code from an LTE cell.
     */
    private void getInfoLTE(List<Pair<String, String>> metricsList, CellInfo cellInfo) {
        CellInfoLte info = (CellInfoLte) cellInfo;
        CellIdentityLte cellIdentity = info.getCellIdentity();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            metricsList.add(new Pair<>(METRIC_MOBILE_NETWORK_CODE, cellIdentity.getMncString()));
            metricsList.add(new Pair<>(METRIC_MOBILE_COUNTRY_CODE, cellIdentity.getMccString()));
        } else {
            metricsList.add(new Pair<>(METRIC_MOBILE_NETWORK_CODE, Integer.toString(cellIdentity.getMnc())));
            metricsList.add(new Pair<>(METRIC_MOBILE_COUNTRY_CODE, Integer.toString(cellIdentity.getMcc())));
        }
    }

    /**
     * Collects the mobile network code & mobile country code from a WCDMA cell.
     */
    private void getInfoWCDMA(List<Pair<String, String>> metricsList, CellInfo cellInfo) {
        CellInfoWcdma info = (CellInfoWcdma) cellInfo;
        CellIdentityWcdma cellIdentity = info.getCellIdentity();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            metricsList.add(new Pair<>(METRIC_MOBILE_NETWORK_CODE, cellIdentity.getMncString()));
            metricsList.add(new Pair<>(METRIC_MOBILE_COUNTRY_CODE, cellIdentity.getMccString()));
        } else {
            metricsList.add(new Pair<>(METRIC_MOBILE_NETWORK_CODE, Integer.toString(cellIdentity.getMnc())));
            metricsList.add(new Pair<>(METRIC_MOBILE_COUNTRY_CODE, Integer.toString(cellIdentity.getMcc())));
        }
    }

    /**
     * Collects the mobile network code & mobile country code from a 5G NR cell.
     */
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void getInfoNR(List<Pair<String, String>> metricsList, CellInfo cellInfo) {
        CellInfoNr info = (CellInfoNr) cellInfo;
        CellIdentityNr cellIdentity = (CellIdentityNr) info.getCellIdentity();

        metricsList.add(new Pair<>(METRIC_MOBILE_NETWORK_CODE, cellIdentity.getMncString()));
        metricsList.add(new Pair<>(METRIC_MOBILE_COUNTRY_CODE, cellIdentity.getMccString()));
    }

    /**
     * Converts the Data Network Type int value to a Radio Technology string
     */
    private String getRadioTechnologyString(int dataNetworkType) {
        switch (dataNetworkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_NR:
                return "5G";
            default:
                return "Unknown";
        }
    }
}