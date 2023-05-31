package gov.cms.yamlEncryptor

import arrow.core.Option
import arrow.core.flatMap
import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.CryptoResult
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKey
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKeyProvider
import com.google.common.io.CharSink
import com.google.common.io.CharSource
import com.google.common.io.CharStreams
import com.google.common.io.Files
import gov.cms.bfd.sharedutils.config.LayeredConfiguration
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.PersistentCollection
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.Writer
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.*

interface Cipher {
    /**
     * Convert the given plain text into a base64 encoded string of cipher text.
     */
    fun encrypt(plainText: String): String

    /**
     * Convert the given base 64 encoded cipher text into plain text.
     */
    fun decrypt(cipherText: String): String
}

interface Dictionary {
    fun empty(): Dictionary

    /**
     * Get a new instance containing values from the file.
     */
    fun load(file: CharSource): Result<Dictionary>

    /**
     * Write this instance to the file.
     */
    fun store(file: CharSink): Result<Unit>

    fun keys(): ImmutableCollection<String>

    fun get(key: String): Option<String>

    fun map(f: (String) -> String): Dictionary

    fun encrypt(cipher: Cipher): Result<Dictionary> = runCatching {
        map { v: String ->
            if (v.startsWith("PLAIN:")) {
                "CIPHER:" + cipher.encrypt(v.substring(6))
            } else {
                v
            }
        }
    }

    fun decrypt(cipher: Cipher): Result<Dictionary> = runCatching {
        map { v: String ->
            if (v.startsWith("CIPHER:")) {
                cipher.decrypt(v.substring(7))
            } else if (v.startsWith("PLAIN:")) {
                v.substring(6)
            } else {
                v
            }
        }
    }
}

class KmsCipher(private val endpoint: String?, keyArn: String) : Cipher {
    private val crypto = AwsCrypto.standard()

    private val keyProvider = KmsMasterKeyProvider.builder()
        .customRegionalClientSupplier(::createKmsClient)
        .buildStrict(keyArn)

    private val context = Collections.singletonMap("Application", "YamlEncryptor")

    private fun createKmsClient(region: Region): KmsClient {
        val builder = KmsClient.builder().region(region)
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
        }
        return builder.build()
    }

    override fun encrypt(plainText: String): String {
        val encryptResult: CryptoResult<ByteArray, KmsMasterKey> = crypto.encryptData(
            keyProvider, plainText.toByteArray(
                StandardCharsets.UTF_8
            ), context
        )
        return Base64.getEncoder().encodeToString(encryptResult.result)
    }

    override fun decrypt(cipherText: String): String {
        val decryptResult = crypto.decryptData(keyProvider, Base64.getDecoder().decode(cipherText))
        return String(decryptResult.result, StandardCharsets.UTF_8)
    }
}

class PropertiesDictionary(private val properties: Properties) : Dictionary {
    override fun empty(): Dictionary = PropertiesDictionary(Properties())

    override fun load(file: CharSource): Result<Dictionary> =
        runCatching {
            val props = Properties()
            file.openBufferedStream().use { props.load(it) }
            PropertiesDictionary(props)
        }

    override fun store(file: CharSink): Result<Unit> =
        runCatching {
            file.openBufferedStream().use { properties.store(it, "") }
        }

    override fun keys(): ImmutableCollection<String> = properties.stringPropertyNames().toPersistentList()

    override fun get(key: String): Option<String> = Option.fromNullable(properties.getProperty(key))

    override fun map(f: (String) -> String): Dictionary {
        val newProperties = Properties()
        properties.stringPropertyNames().stream().forEach { key ->
            get(key).onSome { value -> newProperties.setProperty(key, value) }
        }
        return PropertiesDictionary(newProperties)
    }

}

class PrintBuffer : CharSink() {
    private val buffer = StringBuilder()

    override fun openStream(): Writer = CharStreams.asWriter(buffer)
    override fun toString(): String = buffer.toString()
}

fun main(args: Array<String>) {
    val config = LayeredConfiguration.createConfigLoader(Collections.emptyMap(), System::getenv)
    val endpoint = config.stringValue(LayeredConfiguration.ENV_VAR_KEY_SSM_ENDPOINT, null)
    val keyArn = config.stringValue("keyArn")
    val cipher = KmsCipher(endpoint, keyArn)
    val empty = PropertiesDictionary(Properties())
    val mode = args[0]
    val inputFile = args[1]
    val outputFile = if (args.size == 2) inputFile else args[2]
    val original = empty.load(Files.asCharSource(File(inputFile), Charsets.UTF_8)).getOrThrow()
    if (mode == "cat") {
        val decrypted = original.decrypt(cipher).getOrThrow()
        val printBuffer = PrintBuffer()
        decrypted.store(printBuffer).getOrThrow()
        print(printBuffer.toString())
    } else if (mode == "encrypt") {

    }
}
