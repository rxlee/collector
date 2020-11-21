package com.kkwl.collector.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PasswordFinder;



public class SslUtil
{
  public static SSLSocketFactory getSocketFactory(String caCrtFile, String crtFile, String keyFile, String password) throws Exception {
    Security.addProvider(new BouncyCastleProvider());

    
    PEMReader reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(caCrtFile, new String[0])))));
    X509Certificate caCert = (X509Certificate)reader.readObject();
    reader.close();

    
    reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(crtFile, new String[0])))));
    X509Certificate cert = (X509Certificate)reader.readObject();
    reader.close();


    
    reader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(Files.readAllBytes(Paths.get(keyFile, new String[0])))), new PasswordFinder()
        {
          public char[] getPassword()
          {
            return password.toCharArray();
          }
        });
    
    KeyPair key = (KeyPair)reader.readObject();
    reader.close();

    
    KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
    caKs.load(null, null);
    caKs.setCertificateEntry("ca-certificate", caCert);
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(caKs);

    
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    ks.load(null, null);
    ks.setCertificateEntry("certificate", cert);
    ks.setKeyEntry("private-key", key.getPrivate(), password.toCharArray(), new Certificate[] { cert });
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, password.toCharArray());

    
    SSLContext context = SSLContext.getInstance("TLSv1");
    context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    
    return context.getSocketFactory();
  }
}
