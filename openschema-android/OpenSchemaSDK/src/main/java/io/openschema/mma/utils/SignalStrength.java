package io.openschema.mma.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.TelephonyManager;

public class SignalStrength {

    private final WifiManager wifiManager;
    private final WifiInfo wifiInfo;
    private final TelephonyManager telephonyManager;

    public SignalStrength(Context context){
        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        wifiInfo =  wifiManager.getConnectionInfo();
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public int getWifiRSSI() {
       return wifiInfo.getRssi();
    }

    public int getCellularRSSI() {

        CellInfoGsm cellInfoGsm = (CellInfoGsm)telephonyManager.getAllCellInfo().get(0);
        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfoGsm.getCellSignalStrength();
        return cellSignalStrengthGsm.getDbm();
    }

}
