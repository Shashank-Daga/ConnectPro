package com.connectpro.data.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.connectpro.data.auth.AuthTokenResponse
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionManager(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun saveTokens(tokens: AuthTokenResponse) {
        val saved = preferences.edit()
            .putString(KEY_ACCESS_TOKEN, encrypt(tokens.accessToken))
            .putString(KEY_REFRESH_TOKEN, encrypt(tokens.refreshToken))
            .putString(KEY_TOKEN_TYPE, tokens.tokenType)
            .putLong(
                KEY_ACCESS_EXPIRY,
                System.currentTimeMillis() + tokens.expiresIn * 1000L
            )
            .commit()

        check(saved) { "Unable to persist the authentication session" }
    }

    @Synchronized
    fun accessToken(): String? = decryptStored(KEY_ACCESS_TOKEN)

    @Synchronized
    fun refreshToken(): String? = decryptStored(KEY_REFRESH_TOKEN)

    @Synchronized
    fun tokenType(): String =
        preferences.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer"

    @Synchronized
    fun accessTokenExpiresAtMillis(): Long =
        preferences.getLong(KEY_ACCESS_EXPIRY, 0L)

    @Synchronized
    fun hasSession(): Boolean =
        accessToken() != null && refreshToken() != null

    @Synchronized
    fun clear() {
        preferences.edit().clear().commit()
    }

    private fun decryptStored(key: String): String? {
        val stored = preferences.getString(key, null) ?: return null

        return try {
            decrypt(stored)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encryptedBytes

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)

        require(combined.size > GCM_IV_LENGTH) {
            "Invalid encrypted session value"
        }

        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        )

        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES_NAME = "connectpro_secure_session"
        const val KEY_ALIAS = "connectpro_session_encryption_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"

        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_LENGTH_BITS = 128

        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_TOKEN_TYPE = "token_type"
        const val KEY_ACCESS_EXPIRY = "access_token_expiry"
    }
}
