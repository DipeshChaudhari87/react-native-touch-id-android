package co.eleken.react_native_touch_id_android;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

public class FingerprintModule extends ReactContextBaseJavaModule {
    
    
    final String STATUS_FAILED = "failed";
    final String STATUS_OK = "ok";
    final BiometricManager biometricManager;


    private final ReactApplicationContext mReactContext;
    
    FingerprintModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mReactContext = reactContext;
        biometricManager = new BiometricManager(reactContext);
    }

    String TAG = "FingerprintModule";
    
    @Override
    public String getName() {
        return "Fingerprint";
    }
    
    @ReactMethod
    public void requestTouch(final Promise promise) {

        if (!biometricManager.isSensorAvailable(null)) {
            sendResponse(STATUS_FAILED, "Finger sensor is not available", promise);
            return;
        }
        
        Activity currentActivity = getCurrentActivity();
        
        if (currentActivity == null) {
            sendResponse(STATUS_FAILED, "Can't find current Activity", promise);
            return;
        }

        final String msg = "Error occured while authenticating fingeprint";

        biometricManager.authenticate(new BiometricCallback() {
            @Override
            public void onSdkVersionNotSupported() {
                Log.e(TAG,"onSdkVersionNotSupported");
                sendResponse(STATUS_FAILED, Constants.MSG_API_LEVEL_NOT_SUPPORTED,promise);
            }

            @Override
            public void onBiometricAuthenticationNotSupported() {
                Log.e(TAG,"onBiometricAuthenticationNotSupported");
                sendResponse(STATUS_FAILED,Constants.MSG_HARDWARE_NOT_PRESENT,promise);
            }

            @Override
            public void onBiometricAuthenticationNotAvailable() {
                Log.e(TAG,"onBiometricAuthenticationNotAvailable");
                sendResponse(STATUS_FAILED,Constants.MSG_FINGERPRINT_NOT_ENROLLED,promise);
            }

            @Override
            public void onBiometricAuthenticationPermissionNotGranted() {
                Log.e(TAG,"onBiometricAuthenticationPermissionNotGranted");
                sendResponse(STATUS_FAILED,Constants.MSG_PERMISSION_NOT_GRANTED,promise);
            }

            @Override
            public void onBiometricAuthenticationInternalError(String error) {
                Log.e(TAG,"onBiometricAuthenticationInternalError"+"-"+error);
                sendResponse(STATUS_FAILED,error.isEmpty()?msg:error.toString(),promise);
            }

            @Override
            public void onAuthenticationFailed() {
                Log.e(TAG,"onAuthenticationFailed");
                sendResponse(STATUS_FAILED,Constants.MSG_FINGERPRINT_NOT_RECOGNIZED,promise);
            }

            @Override
            public void onAuthenticationCancelled() {
                Log.e(TAG,"onAuthenticationCancelled");
               // sendResponse(STATUS_FAILED,Constants.LDAError.LDA_BIOMETRIC_AUTHENTICATION_CANCELLED);
            }

            @Override
            public void onAuthenticationSuccessful() {
                Log.e(TAG,"onAuthenticationSuccessful");
                sendResponse(STATUS_OK,"",promise);
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                if(!helpString.toString().isEmpty()) {
                    Log.e(TAG, "onAuthenticationHelp" + "-" + helpCode + "-" + helpString);
                    sendResponse(STATUS_FAILED, helpString.toString(), promise);
                }else {
                    Log.e(TAG, "onAuthenticationHelp message is empty");
                }
            }

            @Override
            public void onAuthenticationError(int errorCode, final CharSequence errString) {
                Log.e(TAG,"onAuthenticationError"+"-"+errorCode+"-"+errString);
                if(errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                    if (errorCode == BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    sendResponse(STATUS_FAILED, "LOCKED_OUT", promise);
                                } catch (Exception e) {
                                    Log.d("exceptionLog", errString.toString().isEmpty() ? msg : errString.toString());
                                }
                            }
                        });
                        t.start();
                    } else {
                        sendResponse(STATUS_FAILED, errString.toString().isEmpty() ? msg.toString() : errString.toString(), promise);
                    }
                }
            }
        });


       /*
        Reprint.authenticate(new AuthenticationListener() {
            @Override
            public void onSuccess(int moduleTag) {
                sendResponse("ok", null, promise);
            }
            @Override
            public void onFailure(final AuthenticationFailureReason failureReason, final boolean fatal,
                                  final CharSequence errorMessage, int moduleTag, int errorCode) {

                final CharSequence msg = "Authentication Failed";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    if(failureReason == AuthenticationFailureReason.LOCKED_OUT) {
                        final Thread t = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    sendResponse("failed", "LOCKED_OUT", promise);
                                } catch (Exception e) {
                                    Log.d("exceptionLog", errorMessage.toString().isEmpty()?msg.toString():errorMessage.toString());
                                }
                            }
                        });
                        t.start();
                    } else {
                        sendResponse("failed",errorMessage.toString().isEmpty()?msg.toString():errorMessage.toString() , promise);
                    }
                }
            }
        }); */
    }
    
    @ReactMethod
    public void dismiss() {
        biometricManager.disableSensor();
    }
    
    
    @ReactMethod
    public void isSensorAvailable(final Promise promise) {
        /*response = Arguments.createMap();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(mReactContext, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                sendResponse("failed", Constants.MSG_PERMISSION_NOT_GRANTED, promise);
                return;
            }
            
            if (mReactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) || (mReactContext.getSystemService(Context.FINGERPRINT_SERVICE)!=null &&
                ((FingerprintManager) mReactContext.getSystemService(Context.FINGERPRINT_SERVICE)).isHardwareDetected())) {
                if (((FingerprintManager) mReactContext.getSystemService(Context.FINGERPRINT_SERVICE)).hasEnrolledFingerprints()) {
                    sendResponse("ok", null, promise);
                } else {
                    sendResponse("failed", Constants.MSG_FINGERPRINT_NOT_ENROLLED, promise);
                }
            } else {
                sendResponse("failed", Constants.MSG_HARDWARE_NOT_PRESENT, promise);
            }
        } else {
            sendResponse("failed", Constants.MSG_API_LEVEL_NOT_SUPPORTED, promise);
        }*/

        boolean status = biometricManager.isSensorAvailable(new BiometricCallback() {
            @Override
            public void onSdkVersionNotSupported() {
                Log.e(TAG,"onSdkVersionNotSupported");
                sendResponse(STATUS_FAILED, Constants.MSG_API_LEVEL_NOT_SUPPORTED,promise);
            }

            @Override
            public void onBiometricAuthenticationNotSupported() {
                Log.e(TAG,"onBiometricAuthenticationNotSupported");
                sendResponse(STATUS_FAILED,Constants.MSG_HARDWARE_NOT_PRESENT,promise);
            }

            @Override
            public void onBiometricAuthenticationNotAvailable() {
                Log.e(TAG,"onBiometricAuthenticationNotAvailable");
                sendResponse(STATUS_FAILED,Constants.MSG_FINGERPRINT_NOT_ENROLLED,promise);
            }

            @Override
            public void onBiometricAuthenticationPermissionNotGranted() {
                Log.e(TAG,"onBiometricAuthenticationPermissionNotGranted");
                sendResponse(STATUS_FAILED,Constants.MSG_PERMISSION_NOT_GRANTED,promise);
            }

            @Override
            public void onBiometricAuthenticationInternalError(String error) {
                Log.e(TAG,"onBiometricAuthenticationInternalError"+"-"+error);
                sendResponse(STATUS_FAILED,error,promise);
            }

            @Override
            public void onAuthenticationFailed() {

            }

            @Override
            public void onAuthenticationCancelled() {

            }

            @Override
            public void onAuthenticationSuccessful() {

            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {

            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {

            }
        });

        if(status)
            sendResponse(STATUS_OK,null,promise);
    }
    
    private boolean isSensorAvailable() {
        if (ActivityCompat.checkSelfPermission(mReactContext, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (mReactContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) || ((FingerprintManager) mReactContext.getSystemService(Context.FINGERPRINT_SERVICE)).isHardwareDetected());
    }
    
    private void sendResponse(String status, String message, Promise promise) {
       /* Reprint.cancelAuthentication();*/
        biometricManager.disableSensor();
        WritableMap response = Arguments.createMap();
        response.putString("status", status);
        response.putString("error", message);
        promise.resolve(response);
    }
}
