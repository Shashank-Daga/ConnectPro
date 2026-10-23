package com.connectpro.config;

import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.Base64;

@Component
public class JwtKeyProvider {

    private static final int MIN_RSA_KEY_SIZE = 2048;

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JwtKeyProvider(JwtProperties properties) {
        this.privateKey = loadPrivateKey(properties.privateKey());
        this.publicKey = loadPublicKey(properties.publicKey());

        validateKeyPair();
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    private RSAPrivateKey loadPrivateKey(String encodedKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PrivateKey key = keyFactory.generatePrivate(
                    new PKCS8EncodedKeySpec(keyBytes)
            );

            if (!(key instanceof RSAPrivateKey rsaPrivateKey)) {
                throw new IllegalStateException(
                        "Configured private key is not an RSA private key"
                );
            }

            return rsaPrivateKey;

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load JWT private key",
                    exception
            );
        }
    }

    private RSAPublicKey loadPublicKey(String encodedKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(encodedKey);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PublicKey key = keyFactory.generatePublic(
                    new X509EncodedKeySpec(keyBytes)
            );

            if (!(key instanceof RSAPublicKey rsaPublicKey)) {
                throw new IllegalStateException(
                        "Configured public key is not an RSA public key"
                );
            }

            return rsaPublicKey;

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to load JWT public key",
                    exception
            );
        }
    }

    private void validateKeyPair() {
        if (privateKey.getModulus().bitLength() < MIN_RSA_KEY_SIZE) {
            throw new IllegalStateException(
                    "Configured JWT private key is too small"
            );
        }

        if (publicKey.getModulus().bitLength() < MIN_RSA_KEY_SIZE) {
            throw new IllegalStateException(
                    "Configured JWT public key is too small"
            );
        }

        if (!privateKey.getModulus().equals(publicKey.getModulus())) {
            throw new IllegalStateException(
                    "Configured JWT public and private keys do not match"
            );
        }
    }
}
