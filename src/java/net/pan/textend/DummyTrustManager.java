package net.pan.textend;

import java.util.Arrays;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.security.KeyStore;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;

import java.net.Socket;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 * Always lets invalid certificates succeeed.  Failed certificates are logged.
 */
class DummyTrustManager
extends X509ExtendedTrustManager
{
    private static final Logger logger =
        Logger.getLogger(DummyTrustManager.class.getName());

    private final X509ExtendedTrustManager[] realTrustManagers;

    private interface Checker
    {
        void checkWith(X509ExtendedTrustManager realTrustManager)
        throws CertificateException;
    }

    DummyTrustManager()
    {
        TrustManagerFactory factory;
        try
        {
            factory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
            factory.init((KeyStore) null);
        }
        catch (GeneralSecurityException e)
        {
            // We should not be able to get here.
            throw new RuntimeException(e);
        }

        TrustManager[] allTrustManagers = factory.getTrustManagers();
        realTrustManagers = Arrays.stream(allTrustManagers)
            .filter(tm -> tm instanceof X509ExtendedTrustManager)
            .map(tm -> (X509ExtendedTrustManager) tm)
            .toArray(X509ExtendedTrustManager[]::new);
        
    }

    private void check(Checker checker)
    {
        try
        {
            for (X509ExtendedTrustManager realTrustManager : realTrustManagers)
            {
                checker.checkWith(realTrustManager);
            }
        }
        catch (CertificateException e)
        {
            logger.log(Level.WARNING, "Invalid certificate", e);
        }
    }

    private void check(Checker checker,
                       Socket socket)
    {
        try
        {
            for (X509ExtendedTrustManager realTrustManager : realTrustManagers)
            {
                checker.checkWith(realTrustManager);
            }
        }
        catch (CertificateException e)
        {
            logger.log(Level.WARNING,
                socket.getRemoteSocketAddress() + " uses invalid certificate",
                e);
        }
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certChain,
                                   String authType,
                                   Socket socket)
    {
        check(tm -> tm.checkClientTrusted(certChain, authType, socket), socket);
/*
        try
        {
            realTrustManager.checkClientTrusted(certChain, authType, socket);
        }
        catch (CertificateException e)
        {
            logger.log(Level.WARNING,
                socket.getRemoteSocketAddress() + " uses invalid certificate",
                e);
        }
*/
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certChain,
                                   String authType,
                                   SSLEngine engine)
    {
        check(tm -> tm.checkClientTrusted(certChain, authType, engine));
/*
        try
        {
            realTrustManager.checkClientTrusted(certChain, authType, engine);
        }
        catch (CertificateException e)
        {
            logger.log(Level.WARNING,
                socket.getRemoteSocketAddress() + " uses invalid certificate",
                e);
        }
*/
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certChain,
                                   String authType,
                                   Socket socket)
    {
        check(tm -> tm.checkServerTrusted(certChain, authType, socket), socket);
/*
        try
        {
            realTrustManager.checkServerTrusted(certChain, authType, socket);
        }
        catch (CertificateException e)
        {
            logger.log(Level.WARNING,
                socket.getRemoteSocketAddress() + " uses invalid certificate",
                e);
        }
*/
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certChain,
                                   String authType,
                                   SSLEngine engine)
    {
        check(tm -> tm.checkServerTrusted(certChain, authType, engine));
/*
        try
        {
            realTrustManager.checkServerTrusted(certChain, authType, engine);
        }
        catch (CertificateException e)
        {
            logger.log(Level.WARNING,
                socket.getRemoteSocketAddress() + " uses invalid certificate",
                e);
        }
*/
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certChain,
                                   String authType)
    {
        check(tm -> tm.checkServerTrusted(certChain, authType));
/*
        try
        {
            realTrustManager.checkServerTrusted(certChain, authType, socket);
        }
        catch (CertificateException e)
        {
            logger.log(Level.WARNING,
                socket.getRemoteSocketAddress() + " uses invalid certificate",
                e);
        }
*/
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certChain,
                                   String authType)
    {
        check(tm -> tm.checkServerTrusted(certChain, authType));
/*
        try
        {
            realTrustManager.checkServerTrusted(certChain, authType, engine);
        }
        catch (CertificateException e)
        {
            logger.log(Level.WARNING,
                socket.getRemoteSocketAddress() + " uses invalid certificate",
                e);
        }
*/
    }

    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
        // TODO: Do we need anything here, considering all CAs are acceptable?
        return new X509Certificate[0];
    }
}
