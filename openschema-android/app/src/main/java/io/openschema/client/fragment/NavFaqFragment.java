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

//Empty fragment used for integration with Navigation component and Navigation Drawer
public class NavFaqFragment extends Fragment {
    private static final String TAG = "NavFaqFragment";

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

        Log.d(TAG, "UI: Opening FAQ in browser");
        //Check if there is any app capable of handling the intent first
        if (browserIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(browserIntent);
        }
    }
}
