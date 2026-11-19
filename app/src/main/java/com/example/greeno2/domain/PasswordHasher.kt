package com.example.greeno2.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 password hashing. Passwords are never stored in plain text. */
object PasswordHasher {
    private const val ITERATIONS = 60_000
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    data class Hashed(val hash: String, val salt: String)

    fun hash(password: String, salt: ByteArray = newSalt()): Hashed {
        val key = derive(password, salt)
        val enc = Base64.getEncoder()
        return Hashed(enc.encodeToString(key), enc.encodeToString(salt))
    }

    fun verify(password: String, hash: String, salt: String): Boolean {
        val dec = Base64.getDecoder()
        val expected = dec.decode(hash)
        val actual = derive(password, dec.decode(salt))
        return MessageDigest.isEqual(expected, actual)
    }

    private fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
