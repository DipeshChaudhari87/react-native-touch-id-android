package co.eleken.react_native_touch_id_android;

import android.content.Context;

class BiometricManager extends FingerprintHandler {


    protected BiometricManager(final Context context) {
        this.context = context;
    }

    public void authenticate(final BiometricCallback biometricCallback) {

        if(!isSensorAvailable(biometricCallback)){
            return;
        }

        requestTouch(biometricCallback);
    }

    public void disableSensor(){
        disableFingerprintSensor();
    }

    public boolean isSensorAvailable(BiometricCallback biometricCallback){
        if(!BiometricUtils.isSdkVersionSupported()) {
            if(biometricCallback!=null)
                biometricCallback.onSdkVersionNotSupported();
            return false;
        }

        if(!BiometricUtils.isPermissionGranted(context)) {
            if(biometricCallback!=null)
                biometricCallback.onBiometricAuthenticationPermissionNotGranted();
            return false;
        }

        if(!BiometricUtils.isHardwareSupported(context)) {
            if(biometricCallback!=null)
                biometricCallback.onBiometricAuthenticationNotSupported();
            return false;
        }

        if(!BiometricUtils.isFingerprintAvailable(context)) {
            if(biometricCallback!=null)
                biometricCallback.onBiometricAuthenticationNotAvailable();
            return false;
        }

        return true;
    }

    private void requestTouch(BiometricCallback biometricCallback) {

        try{
            authenticateFingerprint(biometricCallback);
        }catch (Exception e){
            e.printStackTrace();
            biometricCallback.onBiometricAuthenticationInternalError("Internal error occurred."+ (e.getMessage() != null ? e.getMessage() : ""));
        }
    }
}
