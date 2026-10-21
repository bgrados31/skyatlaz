package org.tnozamas.skyatlaz.api.service

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.ProfileKeyPair
import java.security.PrivateKey
import java.security.Signature

/**
 * The profile key pair of the player, the way the vanilla client sees it.
 *
 * The private key signs the challenge of the service, the public key and Mojang's signature over it prove to the
 * service that the caller owns the key and that Mojang issued it for this account. Nothing secret leaves the game.
 */
class ProfileKeyMaterial(
    /** The uuid of the player. */
    val uuid: String,
    /** The DER encoded public key of the key pair. */
    val publicKey: ByteArray,
    /** The signature Mojang produced over the public key. */
    val keySignature: ByteArray,
    /** When the key pair expires, in milliseconds since the epoch. */
    val expiresAt: Long,
    private val privateKey: PrivateKey,
) {
    /** Signs [payload] the way the vanilla client signs profile data. */
    fun sign(payload: ByteArray): ByteArray {
        val signer = Signature.getInstance(ALGORITHM)
        signer.initSign(privateKey)
        signer.update(payload)
        return signer.sign()
    }

    companion object {
        private const val ALGORITHM = "SHA256withRSA"

        /**
         * Reads the key pair of the player. Blocks until the client has it, so it may only run on a worker thread.
         * Returns null when the account has no profile keys (an offline account, for example).
         */
        fun current(): ProfileKeyMaterial? {
            val minecraft = Minecraft.getInstance()
            val keyPair =
                minecraft.profileKeyPairManager
                    .prepareKeyPair()
                    .join()
                    .orElse(null) ?: return null
            return from(keyPair, minecraft.gameProfile.id.toString())
        }

        private fun from(
            keyPair: ProfileKeyPair,
            uuid: String,
        ): ProfileKeyMaterial {
            val data = keyPair.publicKey().data()
            return ProfileKeyMaterial(
                uuid = uuid,
                publicKey = data.key().encoded,
                keySignature = data.keySignature(),
                expiresAt = data.expiresAt().toEpochMilli(),
                privateKey = keyPair.privateKey(),
            )
        }
    }
}
