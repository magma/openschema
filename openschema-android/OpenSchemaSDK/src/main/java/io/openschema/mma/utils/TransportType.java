package io.openschema.mma.utils;

import android.net.NetworkCapabilities;

public class TransportType {

    //Metric name to be used on collected metrics which require specifying network type
    public static final String METRIC_TRANSPORT_TYPE = "transportType";

    //Internal static enums used in OpenSchema ETL
    private static final String TRANSPORT_WIFI = "wifi";
    private static final String TRANSPORT_CELLULAR = "cellular";

    public static String getTransportString(int transportType) {
        switch (transportType) {
            case NetworkCapabilities.TRANSPORT_WIFI:
                return TRANSPORT_WIFI;
            case NetworkCapabilities.TRANSPORT_CELLULAR:
                return TRANSPORT_CELLULAR;
            default:
                return "unknown";
        }
    }
}
