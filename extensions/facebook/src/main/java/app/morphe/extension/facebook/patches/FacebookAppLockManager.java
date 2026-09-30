/*
 * Copyright (C) 2026 RHpatch <https://github.com/Rhdevs71/dududu>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.morphe.extension.facebook.patches;

import android.app.Activity;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

import app.morphe.extension.crimera.PikoUtils;
import app.morphe.extension.facebook.settings.FacebookPref;

public class FacebookAppLockManager {
    private static final String TAG = "FacebookAppLockManager";
    private static volatile boolean isUnlocked = false;
    private static volatile boolean isAuthenticating = false;
    private static int activeActivitiesCount = 0;
    private static CancellationSignal cancellationSignal;
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Executor mainExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            mainHandler.post(command);
        }
    };

    public static boolean isUnlocked() {
        return isUnlocked;
    }

    public static void setUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public static void onActivityResumed(final Activity activity) {
        try {
            activeActivitiesCount++;
            if (!FacebookPref.isAppLock()) {
                isUnlocked = true;
                return;
            }

            if (isUnlocked) {
                return;
            }

            if (activity == null || activity.isFinishing()) {
                return;
            }

            authenticate(activity);
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "onActivityResumed error", t);
        }
    }

    public static void onActivityStopped(Activity activity) {
        try {
            activeActivitiesCount--;
            if (activeActivitiesCount <= 0) {
                activeActivitiesCount = 0;
                if (FacebookPref.isAppLock()) {
                    isUnlocked = false;
                }
            }
        } catch (Throwable t) {
            PikoUtils.logger(TAG, "onActivityStopped error", t);
        }
    }

    public static void authenticate(final Activity activity) {
        if (isAuthenticating || isUnlocked) return;
        isAuthenticating = true;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (activity.isFinishing() || isUnlocked) {
                        isAuthenticating = false;
                        return;
                    }

                    cancellationSignal = new CancellationSignal();

                    BiometricPrompt.Builder builder = new BiometricPrompt.Builder(activity)
                            .setTitle("RHpatch Facebook")
                            .setSubtitle("Konfirmasi identitas Anda untuk membuka Facebook");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        builder.setAllowedAuthenticators(
                                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        );
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        builder.setDeviceCredentialAllowed(true);
                    } else {
                        builder.setNegativeButton("Keluar", mainExecutor, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isAuthenticating = false;
                                activity.moveTaskToBack(true);
                            }
                        });
                    }

                    BiometricPrompt prompt = builder.build();
                    prompt.authenticate(
                            cancellationSignal,
                            mainExecutor,
                            new BiometricPrompt.AuthenticationCallback() {
                                @Override
                                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                                    super.onAuthenticationSucceeded(result);
                                    isUnlocked = true;
                                    isAuthenticating = false;
                                }

                                @Override
                                public void onAuthenticationError(int errorCode, CharSequence errString) {
                                    super.onAuthenticationError(errorCode, errString);
                                    isAuthenticating = false;
                                    if (!isUnlocked) {
                                        activity.moveTaskToBack(true);
                                    }
                                }

                                @Override
                                public void onAuthenticationFailed() {
                                    super.onAuthenticationFailed();
                                }
                            }
                    );
                } catch (Throwable t) {
                    isAuthenticating = false;
                    PikoUtils.logger(TAG, "authenticate prompt failed", t);
                }
            }
        });
    }
}
