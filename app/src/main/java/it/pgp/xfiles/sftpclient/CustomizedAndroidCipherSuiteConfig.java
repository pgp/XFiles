package it.pgp.xfiles.sftpclient;

import android.util.Log;

import com.hierynomus.sshj.signature.SignatureEdDSA;
import com.hierynomus.sshj.transport.cipher.BlockCiphers;

import net.schmizz.sshj.DefaultConfig;
import net.schmizz.sshj.signature.SignatureDSA;
import net.schmizz.sshj.signature.SignatureRSA;
import net.schmizz.sshj.transport.random.JCERandom;
import net.schmizz.sshj.transport.random.SingletonRandomFactory;

import java.security.Security;

public class CustomizedAndroidCipherSuiteConfig
        extends DefaultConfig {

    static {
        Security.removeProvider("BC"); // disable Android's internal Bouncycastle provider (which has higher priority)
        int k = Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(),1);
        Log.d(CustomizedAndroidCipherSuiteConfig.class.getName(),"BouncyCastle registered: "+k);
    }

    @Override
    protected void initSignatureFactories() {
        setSignatureFactories(
                new SignatureEdDSA.Factory(), // also valid for ed25519
                new SignatureRSA.Factory(),
                new SignatureDSA.Factory()); // original
//        setSignatureFactories(new SignatureECDSA.Factory()); // ECDSA not working - invalid key spec
    }

    @Override
    protected void initCipherFactories() {
        setCipherFactories(
                BlockCiphers.AES128CTR(),
                BlockCiphers.AES192CTR(),
                BlockCiphers.AES256CTR(),
                BlockCiphers.AES128CBC(),
                BlockCiphers.AES192CBC(),
                BlockCiphers.AES256CBC()
                );
    }

    @Override
    protected void initRandomFactory(boolean ignored) {
        setRandomFactory(new SingletonRandomFactory(new JCERandom.Factory()));
    }

}
