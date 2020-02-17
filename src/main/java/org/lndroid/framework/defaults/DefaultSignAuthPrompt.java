package org.lndroid.framework.defaults;

import android.util.Log;

import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import org.lndroid.framework.WalletData;
import org.lndroid.framework.common.Errors;
import org.lndroid.framework.common.IResponseCallback;
import org.lndroid.framework.common.ISigner;
import org.lndroid.framework.engine.ISignAuthPrompt;

public class DefaultSignAuthPrompt implements ISignAuthPrompt {

    private static final String TAG = "DefaultSignAuthPrompt";

    private void authDevice(
            final ISigner signer,
            FragmentActivity activity,
            final WalletData.User u,
            final IResponseCallback cb)
    {

        BiometricPrompt.PromptInfo.Builder b = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Please authorize")
                .setSubtitle("Confirm your identity before accessing the wallet");

        if (!WalletData.AUTH_TYPE_BIO.equals(u.authType()))
            b.setDeviceCredentialAllowed(true);
        else
            b.setNegativeButtonText("Cancel");

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                activity,
                activity.getMainExecutor(),
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        // retry
                        signer.setAuthObject(result.getCryptoObject());
                        cb.onResponse(null);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.e(TAG, "device auth failed: " + errorCode + " error " + errString);
                        cb.onError(Errors.FORBIDDEN, "" + errString);
                    }


                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Log.e(TAG, "device auth failed");
                        cb.onError(Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
                    }

                }
        );

        if (WalletData.AUTH_TYPE_BIO.equals(u.authType())) {
            biometricPrompt.authenticate(b.build(), (BiometricPrompt.CryptoObject) signer.getAuthObject());
        } else {
            biometricPrompt.authenticate(b.build());
        }
    }

    private void authPassword(
            final ISigner signer,
            FragmentActivity activity,
            final WalletData.User u,
            final IResponseCallback cb)
    {
        // FIXME show password dialog, when user is done do
        String password = null;
        signer.setAuthObject(password);
        cb.onResponse(null);
    }

    @Override
    public void auth(final ISigner signer,
                     FragmentActivity activity,
                     final WalletData.User u,
                     final IResponseCallback cb) {
        switch (u.authType()) {
            case WalletData.AUTH_TYPE_NONE:
                cb.onError(Errors.FORBIDDEN, Errors.errorMessage(Errors.FORBIDDEN));
                return;

            case WalletData.AUTH_TYPE_PASSWORD:
                authPassword(signer, activity, u, cb);
                return;

            case WalletData.AUTH_TYPE_DEVICE_SECURITY:
            case WalletData.AUTH_TYPE_SCREEN_LOCK:
            case WalletData.AUTH_TYPE_BIO:
                authDevice(signer, activity, u, cb);
                break;

            default:
                throw new RuntimeException("Unknown auth type");

        }
    }
}
