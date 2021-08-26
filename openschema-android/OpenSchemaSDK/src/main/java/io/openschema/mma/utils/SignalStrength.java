package io.openschema.mma.utils;

import android.content.Context;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoNr;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.TelephonyManager;

import java.util.List;

public class SignalStrength {

    private final WifiManager mWifiManager;
    private final TelephonyManager mTelephonyManager;

    public SignalStrength(Context context) {
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    public int getRSSI(int transportType) {
        switch (transportType) {
            case NetworkCapabilities.TRANSPORT_WIFI:
                return getWifiRSSI();
            case NetworkCapabilities.TRANSPORT_CELLULAR:
                return getCellularRSSI();
            default:
                throw new RuntimeException("Invalid tranport type");
        }
    }

    public int getWifiRSSI() {
        return mWifiManager.getConnectionInfo().getRssi();
    }

    public int getCellularRSSI() throws SecurityException {
        int strength = 0;
        List<CellInfo> cellInfos = mTelephonyManager.getAllCellInfo();   //This will give info of all sims present inside your mobile
        if (cellInfos != null) {
            for (int i = 0; i < cellInfos.size(); i++) {
                if (cellInfos.get(i).isRegistered()) {
                    if (cellInfos.get(i) instanceof CellInfoWcdma) {
                        CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) cellInfos.get(i);
                        CellSignalStrengthWcdma cellSignalStrengthWcdma = cellInfoWcdma.getCellSignalStrength();
                        strength = cellSignalStrengthWcdma.getDbm();
                    } else if (cellInfos.get(i) instanceof CellInfoGsm) {
                        CellInfoGsm cellInfogsm = (CellInfoGsm) cellInfos.get(i);
                        CellSignalStrengthGsm cellSignalStrengthGsm = cellInfogsm.getCellSignalStrength();
                        strength = cellSignalStrengthGsm.getDbm();
                    } else if (cellInfos.get(i) instanceof CellInfoLte) {
                        CellInfoLte cellInfoLte = (CellInfoLte) cellInfos.get(i);
                        CellSignalStrengthLte cellSignalStrengthLte = cellInfoLte.getCellSignalStrength();
                        strength = cellSignalStrengthLte.getDbm();
                    } else if (cellInfos.get(i) instanceof CellInfoCdma) {
                        CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfos.get(i);
                        CellSignalStrengthCdma cellSignalStrengthCdma = cellInfoCdma.getCellSignalStrength();
                        strength = cellSignalStrengthCdma.getDbm();
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            cellInfos.get(i) instanceof CellInfoNr) {
                        CellInfoNr cellInfoNr = (CellInfoNr) cellInfos.get(i);
                        CellSignalStrength cellSignalStrengthNr = cellInfoNr.getCellSignalStrength();
                        strength = cellSignalStrengthNr.getDbm();
                    }
                }
            }
        }
        return strength;
    }

}
