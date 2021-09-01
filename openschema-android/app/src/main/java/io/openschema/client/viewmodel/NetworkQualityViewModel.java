package io.openschema.client.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import io.openschema.mma.MobileMetricsService;
import io.openschema.mma.data.MetricsRepository;
import io.openschema.mma.data.entity.NetworkQualityEntity;

public class NetworkQualityViewModel extends AndroidViewModel {
    private static final String TAG = "NetworkQualityViewModel";

    private static final int NO_ACTIVE_NETWORK = -1;

    private final ConnectivityManager mConnectivityManager;
    private final MetricsRepository mMetricsRepository;

    private final ConnectivityManager.NetworkCallback mNetworkCallBack = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            Log.d(TAG, "UI: Detected new active network connection");
            NetworkCapabilities networkCapabilities = mConnectivityManager.getNetworkCapabilities(network);
            if (networkCapabilities != null) {
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    mActiveNetworkExists.postValue(NetworkCapabilities.TRANSPORT_WIFI);
                } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    mActiveNetworkExists.postValue(NetworkCapabilities.TRANSPORT_CELLULAR);
                }
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            Log.d(TAG, "UI: Active network connection disconnected");
            mActiveNetworkExists.postValue(NO_ACTIVE_NETWORK);
        }
    };

    private final MutableLiveData<Integer> mActiveNetworkExists = new MutableLiveData<>(NO_ACTIVE_NETWORK);
    private final ActiveNetworkQuality mActiveNetworkQuality;

    public NetworkQualityViewModel(@NonNull Application application) {
        super(application);

        mMetricsRepository = MetricsRepository.getRepository(application);
        mConnectivityManager = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest activeNetworkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        mConnectivityManager.requestNetwork(activeNetworkRequest, mNetworkCallBack);

        mActiveNetworkQuality = new ActiveNetworkQuality(mMetricsRepository.getLastNetworkQualityMeasurement(), mActiveNetworkExists);
    }

    public void remeasureNetworkQuality() {
        //Calling startService with specified action to call method within active Service instance.
        Intent i = new Intent(getApplication(), MobileMetricsService.class);
        i.setAction(MobileMetricsService.ACTION_MEASURE_NETWORK_QUALITY);
        getApplication().startService(i);
    }

    public LiveData<NetworkQualityEntity> getActiveNetworkQuality() {
        return mActiveNetworkQuality;
    }

    @Override
    protected void onCleared() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallBack);
    }

    private static class ActiveNetworkQuality extends MediatorLiveData<NetworkQualityEntity> {

        private NetworkQualityEntity mCurrentQualityEntity = null;
        private int mActiveNetworkType = NO_ACTIVE_NETWORK;

        public ActiveNetworkQuality(LiveData<NetworkQualityEntity> latestMeasurement, LiveData<Integer> activeNetworkExists) {
            addSource(latestMeasurement, networkQualityEntity -> {
                mCurrentQualityEntity = networkQualityEntity;
                update();
            });

            addSource(activeNetworkExists, transportType -> {
                mActiveNetworkType = transportType;
                update();
            });

            setValue(null);
        }

        private void update() {
            if (mActiveNetworkType != NO_ACTIVE_NETWORK &&
                    mCurrentQualityEntity != null &&
                    mCurrentQualityEntity.getTransportType() == mActiveNetworkType) {
                //TODO: Might be pulling the previous measurement value in case the device connected to the same network type twice in a row
                setValue(mCurrentQualityEntity);
            } else {
                setValue(null);
            }
        }
    }
}