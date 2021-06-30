package io.openschema.mma.utils;

import android.content.Context;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

/**
 * Utility class to check whether Location Services are turned on in the device.
 */
public class LocationServicesChecker {

    /**
     * @return True if Location Services are enabled on the device. False if they have been disabled.
     */
    public static boolean isLocationEnabled(Context context) {
        boolean isLocationEnabled;
        //Checking whether location services is enabled was moved from the Settings APIs to LocationManager in SDK 28
        if (Build.VERSION.SDK_INT >= 28) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            isLocationEnabled = locationManager.isLocationEnabled();
        } else {
            try {
                isLocationEnabled = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF;
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                isLocationEnabled = false;
            }
        }
        return isLocationEnabled;
    }
}
