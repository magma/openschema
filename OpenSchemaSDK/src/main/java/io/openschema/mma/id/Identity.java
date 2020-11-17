package io.openschema.mma.id;


import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

/**
 *  Handles the unique ID generation and storage. Unique ID is a UUID and will be used as part of the
 *  registration and bootstrapping.
 */
public class Identity {

    private String mUUID;
    private String mPublicKey;

    public Identity(Context context)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidAlgorithmParameterException, NoSuchProviderException {
        mUUID = new UUID(context).getUUID();
        mPublicKey = new HardwareKey().getHwPublicKey();
        Log.d("MMA: TestIdentity", mUUID + "\n" + mPublicKey);
    }

    public String getUUID() {
        return mUUID;
    }

    public String getPublicKey() {
        return mPublicKey;
    }
}
