package gov.cms.kms

import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.CryptoResult
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKey
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKeyProvider
import gov.cms.bfd.sharedutils.config.LayeredConfiguration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Predicate

fun createKmsClient(region: Region): KmsClient {
    val builder = KmsClient.builder().region(region)
    val endpoint: String? = System.getenv(LayeredConfiguration.ENV_VAR_KEY_SSM_ENDPOINT)
    if (endpoint != null) {
        builder.endpointOverride(URI.create(endpoint))
    }
    return builder.build()
}

private fun createKeyProvider(keyArn: String): KmsMasterKeyProvider =
        KmsMasterKeyProvider.builder()
                .customRegionalClientSupplier(::createKmsClient)
                .buildStrict(keyArn)

fun main(args: Array<String>) {
    val keyArn = args[0]
    val plaintext = args[1]

    // Instantiate the SDK
    val crypto: AwsCrypto = AwsCrypto.standard()

    // Set up the master key provider
    val prov: KmsMasterKeyProvider = createKeyProvider(keyArn)

    // Set up the encryption context
    // NOTE: Encrypted data should have associated encryption context
    // to protect its integrity. This example uses placeholder values.
    // For more information about the encryption context, see
    // https://docs.aws.amazon.com/encryption-sdk/latest/developer-guide/concepts.html#encryption-context
    val context: Map<String, String> = Collections.singletonMap("ExampleContextKey", "ExampleContextValue")

    // Encrypt the data
    //
    val encryptResult: CryptoResult<ByteArray, KmsMasterKey> = crypto.encryptData(prov, plaintext.toByteArray(StandardCharsets.UTF_8), context)
    val ciphertext: ByteArray = encryptResult.result
    println("Ciphertext: " + ciphertext.contentToString())

    // Decrypt the data
    val decryptResult: CryptoResult<ByteArray, KmsMasterKey> = crypto.decryptData(prov, ciphertext)
    // Your application should verify the encryption context and the KMS key to
    // ensure this is the expected ciphertext before returning the plaintext
    if (decryptResult.masterKeyIds[0] != keyArn) {
        throw IllegalStateException("Wrong key id!")
    }

    // The AWS Encryption SDK may add information to the encryption context, so check to
    // ensure all of the values that you specified when encrypting are *included* in the returned encryption context.
    if (!context.entries.stream()
                    .allMatch(Predicate { e: Map.Entry<String, String> -> (e.value == decryptResult.encryptionContext[e.key]) })) {
        throw IllegalStateException("Wrong Encryption Context!")
    }

    assert(Arrays.equals(decryptResult.result, plaintext.toByteArray(StandardCharsets.UTF_8)))

    // The data is correct, so return it.
    println("Decrypted: " + String(decryptResult.result, StandardCharsets.UTF_8))
}
