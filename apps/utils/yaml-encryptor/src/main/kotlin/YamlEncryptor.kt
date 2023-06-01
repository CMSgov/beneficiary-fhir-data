package gov.cms.yamlEncryptor

import arrow.core.Option
import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.CryptoResult
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKey
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKeyProvider
import com.google.common.io.*
import gov.cms.bfd.sharedutils.config.LayeredConfiguration
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.toPersistentList
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import java.io.File
import java.io.Writer
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
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
    fun load(file: CharSource): Dictionary

    /**
     * Write this instance to the file.
     */
    fun store(file: CharSink)

    fun keys(): ImmutableCollection<String>

    fun get(key: String): Option<String>

    fun map(f: (String) -> String): Dictionary

    fun encrypt(cipher: Cipher): Dictionary =
        map { v: String ->
            if (v.startsWith("PLAIN:")) {
                "CIPHER:" + cipher.encrypt(v.substring(6))
            } else {
                v
            }
        }

    fun decrypt(cipher: Cipher): Dictionary =
        map { v: String ->
            if (v.startsWith("CIPHER:")) {
                cipher.decrypt(v.substring(7))
            } else if (v.startsWith("PLAIN:")) {
                v.substring(6)
            } else {
                v
            }
        }

    fun rewind(cipher: Cipher): Dictionary =
        map { v: String ->
            if (v.startsWith("CIPHER:")) {
                "PLAIN:" + cipher.decrypt(v.substring(7))
            } else {
                v
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

    override fun load(file: CharSource): Dictionary {
        val props = Properties()
        file.openBufferedStream().use { props.load(it) }
        return PropertiesDictionary(props)
    }

    override fun store(file: CharSink) =
        file.openBufferedStream().use { properties.store(it, "") }

    override fun keys(): ImmutableCollection<String> = properties.stringPropertyNames().toPersistentList()

    override fun get(key: String): Option<String> = Option.fromNullable(properties.getProperty(key))

    override fun map(f: (String) -> String): Dictionary {
        val newProperties = Properties()
        properties.stringPropertyNames().stream().forEach { key ->
            get(key).onSome { value -> newProperties.setProperty(key, f(value)) }
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
    val editor = config.stringValue("EDITOR", "vi");
    val mode = args[0]
    val inputFile = args[1]
    val outputFile = if (args.size == 2) inputFile else args[2]
    when (mode) {
        "cat" -> {
            val original = empty.load(Files.asCharSource(File(inputFile), Charsets.UTF_8))
            val decrypted = original.decrypt(cipher)
            val printBuffer = PrintBuffer()
            decrypted.store(printBuffer)
            print(printBuffer.toString())
        }

        "encrypt" -> {
            val original = empty.load(Files.asCharSource(File(inputFile), Charsets.UTF_8))
            val encrypted = original.encrypt(cipher)
            encrypted.store(Files.asCharSink(File(outputFile), Charsets.UTF_8))
        }

        "edit" -> {
            val cwd = Paths.get(".").toFile()
            val tempFile = File.createTempFile("edit", ".txt", cwd)
            val original = empty.load(Files.asCharSource(File(inputFile), Charsets.UTF_8))
            val rewound = original.rewind(cipher)
            rewound.store(Files.asCharSink(tempFile, Charsets.UTF_8))
            ProcessBuilder(listOf(editor, tempFile.path))
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor()
            val modified = empty.load(Files.asCharSource(tempFile, Charsets.UTF_8))
            val encrypted = modified.encrypt(cipher)
            encrypted.store(Files.asCharSink(File(outputFile), Charsets.UTF_8))
        }

        else -> {
            println("usage: YamlEncryptor cat|encrypt|edit source [dest]")
        }
    }
}
