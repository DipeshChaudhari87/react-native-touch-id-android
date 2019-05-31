package co.eleken.react_native_touch_id_android;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

@TargetApi(Build.VERSION_CODES.M)
class FingerprintHandler {

    private static final String KEY_NAME = UUID.randomUUID().toString();

    private Cipher cipher;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private FingerprintManagerCompat.CryptoObject cryptoObject;


    protected Context context;

    private CancellationSignal cancellationSignal;

    protected void authenticateFingerprint(final BiometricCallback biometricCallback) {

        //This will generate key for cipher use
        generateKey();

        if(initCipher()) {
            cryptoObject = new FingerprintManagerCompat.CryptoObject(cipher);
            authenticateFingerprint(cryptoObject,biometricCallback);
        }
    }

    private void authenticateFingerprint(final FingerprintManagerCompat.CryptoObject cryptoObject,final BiometricCallback biometricCallback){
        FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(context);
        cancellationSignal = new CancellationSignal();
        fingerprintManagerCompat.authenticate(cryptoObject, 0, cancellationSignal,
                new FingerprintManagerCompat.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errMsgId, CharSequence errString) {
                        super.onAuthenticationError(errMsgId, errString);
                        biometricCallback.onAuthenticationError(errMsgId,errString);
                        //callback called when user already failed authentication
                    }

                    @Override
                    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                        super.onAuthenticationHelp(helpMsgId, helpString);
                        biometricCallback.onAuthenticationHelp(helpMsgId,helpString);
                    }

                    @Override
                    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        biometricCallback.onAuthenticationSuccessful();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        biometricCallback.onAuthenticationFailed();
                    }
                }, null);
    }

    void disableFingerprintSensor(){
        if(cancellationSignal!=null && !cancellationSignal.isCanceled()){
             cancellationSignal.cancel();
             cancellationSignal = null;
        }
    }

    private void generateKey() {
        try {

            keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            keyGenerator.generateKey();

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();

        }
    }

    private boolean initCipher() {
        try {
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;


        } catch (KeyPermanentlyInvalidatedException e) {
            return false;

        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {

            throw new RuntimeException("Failed to init Cipher", e);
        }
    }
}
