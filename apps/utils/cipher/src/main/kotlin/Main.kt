package gov.cms.cipher

import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.CryptoResult
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKey
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKeyProvider
import gov.cms.bfd.sharedutils.config.LayeredConfiguration
import okio.*
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.util.*
import kotlin.io.use
import kotlin.system.exitProcess

val AppName = "cipher"

val PlainPrefix = "<<SECURE>>".encodeUtf8()
val PlainSuffix = "<</SECURE>>".encodeUtf8()

val CipherPrefix = "<<CIPHER>>".encodeUtf8()
val CipherSuffix = "<</CIPHER>>".encodeUtf8()

val EmptyString = ByteString.of()

fun ByteString.toBuffer(): BufferedSource =
    Buffer().readFrom(ByteArrayInputStream(toByteArray()))

class KmsCipher(private val endpoint: String?, keyArn: String) {
    private val crypto = AwsCrypto.standard()

    private val keyProvider = KmsMasterKeyProvider.builder()
        .customRegionalClientSupplier(::createKmsClient)
        .buildStrict(keyArn)

    private val context = Collections.singletonMap("Application", AppName)

    private fun createKmsClient(region: Region): KmsClient {
        val builder = KmsClient.builder().region(region)
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
        }
        return builder.build()
    }

    fun encrypt(plainText: ByteString): ByteString {
        val encryptResult: CryptoResult<ByteArray, KmsMasterKey> =
            crypto.encryptData(keyProvider, plainText.toByteArray(), context)
        return Base64.getUrlEncoder().encodeToString(encryptResult.result).encodeUtf8()
    }

    fun decrypt(cipherText: ByteString): ByteString {
        val decryptResult = crypto.decryptData(keyProvider, Base64.getUrlDecoder().decode(cipherText.utf8()))
        return decryptResult.result.toByteString()
    }
}

class Text(private val bytes: ByteString) {
    fun store(sink: Sink) = sink.buffer().use { it.write(bytes) }

    fun encrypt(cipher: KmsCipher): Text =
        transform(PlainPrefix, PlainSuffix, CipherPrefix, CipherSuffix, cipher::encrypt)

    fun decrypt(cipher: KmsCipher): Text =
        transform(CipherPrefix, CipherSuffix, EmptyString, EmptyString, cipher::decrypt)

    fun rewind(cipher: KmsCipher): Text =
        transform(CipherPrefix, CipherSuffix, PlainPrefix, PlainSuffix, cipher::decrypt)

    private fun transform(
        fromPrefix: ByteString,
        fromSuffix: ByteString,
        toPrefix: ByteString,
        toSuffix: ByteString,
        op: (ByteString) -> ByteString
    ): Text {
        val buffer = Buffer()
        bytes.toBuffer().use { source ->
            while (!source.exhausted()) {
                val start = source.indexOf(fromPrefix)
                if (start >= 0) {
                    buffer.write(source.readByteArray(start))
                    source.skip(fromPrefix.size.toLong())
                    val finish = source.indexOf(fromSuffix)
                    if (finish < 0) {
                        throw IOException("missing " + fromSuffix.utf8())
                    }
                    val plainText = source.readUtf8(finish)
                    source.skip(fromSuffix.size.toLong())
                    val cipherText = op(plainText.encodeUtf8())
                    buffer.write(toPrefix)
                    buffer.write(cipherText)
                    buffer.write(toSuffix)
                } else {
                    buffer.write(source.readByteArray())
                }
            }
        }
        return Text(buffer.readByteString())
    }

    companion object {
        fun load(file: File): Text = load(file.source())
        fun load(source: Source): Text = source.buffer().use { Text(it.readByteArray().toByteString()) }
    }
}

fun usage() {
    println(
        """
        usage: $AppName cat|encrypt|decrypt|edit source [dest]
        
        If dest is omitted the source file will be overwritten.
        
        cat writes a completely decrypted version of source to stdout.
        
        decrypt writes a completely decrypted version of source to dest.
        
        encrypt writes a version of source with all SECURE blocks replaced by CIPHER blocks to dest.
          
        edit creates a temporary file with CIPHER blocks converted to SECURE
          blocks and opens it using the editor defined in EDITOR environment variable.
          If the temp file is modified the SECURE blocks are converted back to CIPHER
          blocks the result is written to the dest file.
          Default editor is vi.

        Note: Both cat and decrypt remove the secure text delimiters so they are not reversible.
    """.trimIndent()
    )
}

fun main(args: Array<String>) {
    if (args.size < 2 || args.size > 3) {
        usage()
        exitProcess(1)
    }

    val config = LayeredConfiguration.createConfigLoader(Collections.emptyMap(), System::getenv)
    val endpoint = config.stringValue(LayeredConfiguration.ENV_VAR_KEY_SSM_ENDPOINT, null)
    val keyArn = config.stringValue("keyArn")
    val cipher = KmsCipher(endpoint, keyArn)
    val editor = config.stringValue("EDITOR", "vi");

    val mode = args[0]
    val inputFile = args[1]
    val outputFile = if (args.size == 2) inputFile else args[2]
    when (mode) {
        "cat" -> {
            val original = Text.load(File(inputFile))
            val decrypted = original.decrypt(cipher)
            val printBuffer = Buffer()
            decrypted.store(printBuffer)
            print(printBuffer.readUtf8())
        }

        "encrypt" -> {
            val original = Text.load(File(inputFile))
            val encrypted = original.encrypt(cipher)
            encrypted.store(File(outputFile).sink())
        }

        "decrypt" -> {
            val original = Text.load(File(inputFile))
            val decrypted = original.decrypt(cipher)
            decrypted.store(File(outputFile).sink())
        }

        "edit" -> {
            val currentDirectory = Paths.get(".").toFile()
            val tempFile = File.createTempFile("edit-", ".txt", currentDirectory)
            try {
                val original = Text.load(File(inputFile))
                val rewound = original.rewind(cipher)
                rewound.store(tempFile.sink())
                ProcessBuilder(listOf(editor, tempFile.path))
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .waitFor()
                val modified = Text.load(tempFile)
                val encrypted = modified.encrypt(cipher)
                encrypted.store(File(outputFile).sink())
            } finally {
                tempFile.delete()
            }
        }

        else -> {
            usage()
        }
    }
}
