package io.openschema.client.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.openschema.mma.utils.DnsPing;
import io.openschema.mma.utils.SignalStrength;

//Empty fragment used for integration with Navigation component and Navigation Drawer
public class NavFaqFragment extends Fragment {
    private static final String TAG = "NavFaqFragment";
    ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        openFAQ();
        NavHostFragment.findNavController(this).popBackStack();
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    private void openFAQ() {
        //Create intent for opening a url in a browser
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.shoelacewireless.com/os/faq"));

        //Testing DNS Ping
        Log.d(TAG, "UI: Opening FAQ in browser");
        DnsPing dnsPing = new DnsPing(executorService);
        dnsPing.dnsTest("1.0.0.1");

        SignalStrength signalStrength = new SignalStrength(getContext());
        Log.d("RSSI", "Wifi RSSI: " + signalStrength.getWifiRSSI());
        Log.d("RSSI", "Cell RSSI" + signalStrength.getCellularRSSI());

        //Check if there is any app capable of handling the intent first
        if (browserIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(browserIntent);
        }
    }
}
