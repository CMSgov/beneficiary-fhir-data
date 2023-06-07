#!/usr/bin/env kotlin

@file:DependsOn("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.5")
@file:DependsOn("com.amazonaws:aws-encryption-sdk-java:2.4.0")
@file:DependsOn("software.amazon.awssdk:kms:2.20.74")
@file:DependsOn("com.squareup.okio:okio:3.3.0")

import com.amazonaws.encryptionsdk.AwsCrypto
import com.amazonaws.encryptionsdk.CryptoResult
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKey
import com.amazonaws.encryptionsdk.kmssdkv2.KmsMasterKeyProvider
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import okio.*
import okio.ByteString.Companion.EMPTY
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.kms.KmsClient
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.*
import kotlin.io.use
import kotlin.system.exitProcess


val APP_NAME = "cipher"
val DEFAULT_REGION = "us-east-1"
val DEFAULT_EDITOR = "vi"

enum class Token(val bytes: ByteString) {
    SecureStart("<<SECURE>>".encodeUtf8()),
    SecureEnd("<</SECURE>>".encodeUtf8()),
    CipherStart("<<CIPHER>>".encodeUtf8()),
    CipherEnd("<</CIPHER>>".encodeUtf8()),
    None(ByteString.of());

    val endsWith: Token? by lazy {
        when (this) {
            SecureStart -> SecureEnd
            CipherStart -> CipherEnd
            else -> null
        }
    }

    val text: String by lazy {
        bytes.utf8()
    }

    val isEnd: Boolean by lazy {
        when (this) {
            SecureEnd -> true
            CipherEnd -> true
            else -> false
        }
    }
}

enum class Span(val start: Token, val end: Token) {
    Secure(Token.SecureStart, Token.SecureEnd),
    Cipher(Token.CipherStart, Token.CipherEnd),
    Text(Token.None, Token.None)
}

data class Block(val endToken: Token, val bytes: ByteString) {
    override fun toString(): String = "($endToken: ${bytes.utf8()})"
}

class UsageException(message: String) : Exception(message)

class ParseException(message: String) : Exception(message) {
    companion object {
        fun noStartToken(token: Token) = ParseException("end token has no start token: ${token.text}")
        fun noEndToken(token: Token) = ParseException("start token has no end token: ${token.text}")
    }
}

class CipherCache private constructor(private val map: PersistentMap<String, ByteString>) {
    constructor() : this(persistentMapOf())

    fun put(context: ByteString, secureText: ByteString, cipherText: ByteString) =
        CipherCache(map.put(makeKey(context, secureText), cipherText))

    fun get(context: ByteString, secureText: ByteString): ByteString? = map[makeKey(context, secureText)]

    private fun makeKey(context: ByteString, secureText: ByteString): String {
        val newLineString = "\n".encodeUtf8()
        val source = context.toBuffer()
        var newLine = source.indexOf(newLineString)
        while (newLine >= 0) {
            source.skip(newLine + newLineString.size)
            newLine = source.indexOf(newLineString)
        }
        val keyPrefix = source.readUtf8()
        val keySuffix = secureText.utf8()
        return "$keyPrefix:$keySuffix"
    }
}

fun ByteString.toBuffer(): BufferedSource =
    Buffer().readFrom(ByteArrayInputStream(toByteArray()))

fun BufferedSource.nextBlock(): Block? {
    if (this.exhausted()) {
        return null
    }

    var firstIndex = -1L
    var firstToken = Token.None
    for (token in Token.values()) {
        if (token.bytes.size > 0) {
            val index = this.indexOf(token.bytes)
            if (index >= 0) {
                if (firstIndex < 0 || index < firstIndex) {
                    firstIndex = index
                    firstToken = token
//                println("$index $token")
                }
            }
        }
    }
//    println("FOUND $firstIndex $firstToken")

    val result = if (firstIndex < 0L) {
        val bytes = this.readByteString()
        Block(Token.None, bytes)
    } else if (firstIndex == 0L) {
        this.skip(firstToken.bytes.size.toLong())
        Block(firstToken, EMPTY)
    } else {
        val bytes = this.readByteString(firstIndex)
        this.skip(firstToken.bytes.size.toLong())
        Block(firstToken, bytes)
    }
//    println(result)
    return result
}

class KmsCipher(private val endpoint: String?, region: String, keyArn: String) {
    private val crypto = AwsCrypto.standard()

    private val keyProvider = KmsMasterKeyProvider.builder()
        .customRegionalClientSupplier(::createKmsClient)
        .defaultRegion(Region.of(region))
        .buildStrict(keyArn)

    private val context = Collections.singletonMap("Application", APP_NAME)

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
        transform(Span.Secure, Span.Cipher) { _: ByteString, secureText: ByteString ->
            cipher.encrypt(secureText)
        }

    fun encrypt(cipher: KmsCipher, cipherCache: CipherCache): Text =
        transform(Span.Secure, Span.Cipher) { context: ByteString, secureText: ByteString ->
            cipherCache.get(context, secureText) ?: cipher.encrypt(secureText)
        }

    fun decrypt(cipher: KmsCipher): Text = transform(Span.Cipher, Span.Text) { _: ByteString, secureText: ByteString ->
        cipher.decrypt(secureText)
    }

    fun rewind(cipher: KmsCipher): Text = transform(Span.Cipher, Span.Secure) { _: ByteString, secureText: ByteString ->
        cipher.decrypt(secureText)
    }

    fun extractCipherCache(cipher: KmsCipher): CipherCache {
        var cache = CipherCache()
        bytes.toBuffer().use { source ->
            var block = source.nextBlock()
            while (block != null) {
                if (block.endToken == Token.CipherStart) {
                    val nextBlock = source.nextBlock()
                    if (nextBlock?.endToken != Token.CipherEnd) {
                        throw ParseException.noEndToken(block.endToken)
                    }
                    val cipherText = nextBlock.bytes
                    val plainText = cipher.decrypt(cipherText)
                    cache = cache.put(block.bytes, plainText, cipherText)
                }
                block = source.nextBlock()
            }
        }
        return cache
    }

    private fun transform(
        from: Span,
        to: Span,
        op: (ByteString, ByteString) -> ByteString
    ): Text {
        val buffer = Buffer()
        bytes.toBuffer().use { source ->
            var block: Block? = source.nextBlock()
            while (block != null) {
                val b: Block = block!!
                if (b.endToken.isEnd) {
                    throw ParseException.noStartToken(b.endToken)
                }
                var requiredNextToken: Token?
                if (b.endToken == from.start) {
                    val nextBlock = source.nextBlock()
                    if (nextBlock?.endToken != from.end) {
                        throw ParseException.noEndToken(b.endToken)
                    }
                    val fromText = nextBlock.bytes
                    val toText = op(b.bytes, fromText)
                    buffer.write(b.bytes)
                    buffer.write(to.start.bytes)
                    buffer.write(toText)
                    buffer.write(to.end.bytes)
                    requiredNextToken = null
                } else {
                    buffer.write(b.bytes)
                    buffer.write(b.endToken.bytes)
                    requiredNextToken = b.endToken.endsWith
                }
                block = source.nextBlock()
                if (requiredNextToken != null && block?.endToken != requiredNextToken) {
                    throw ParseException.noEndToken(requiredNextToken)
                }
            }
        }
        return Text(buffer.readByteString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Text) return false

        return bytes == other.bytes
    }

    override fun hashCode(): Int {
        return bytes.hashCode()
    }

    fun isEmpty(): Boolean = bytes.size == 0 || bytes.utf8().trim().isEmpty()
}

fun loadText(source: Source): Text = source.buffer().use { Text(it.readByteArray().toByteString()) }
fun loadText(file: File): Text = loadText(file.source())

fun usage() {
    println(
        """
        usage: $APP_NAME [options] mode source [dest]
        
        Modes:
          cat writes a completely decrypted version of source to stdout.
        
          decrypt writes a completely decrypted version of source to dest.
        
          encrypt writes a version of source with all secure blocks replaced by cipher blocks to dest.
          
          rewind writes a version of source with all cipher blocks replaced by secure blocks to dest.
          
          edit creates a temporary file with cipher blocks converted to secure
            blocks and opens it using the editor defined in EDITOR environment variable.
            If the temp file is modified the SECURE blocks are converted back to CIPHER
            blocks the result is written to the dest file.
            Default editor is $DEFAULT_EDITOR.

          Note: Both cat and decrypt remove the secure text delimiters so they are not reversible.
        
        Arguments:
          source is the file to process
          dest is an optional file to write processed file to.

          Note: If dest is omitted the source file will be overwritten.
        
        Options:
        --key ARN      : KMS key ARN (REQUIRED unless CIPHER_KEY is defined).
        --endpoint URI : Localhost endpoint URI for localstack KMS.
        --region name  : AWS region name.  Defaults to $DEFAULT_REGION.
        
        Environment variables:
          EDITOR           Editor used in edit mode.  Defaults to $DEFAULT_EDITOR.
          CIPHER_KEY       Same as --key
          CIPHER_ENDPOINT  Same as --endpoint
          CIPHER_REGION    Same as --region
        
        Secure blocks are created and maintained by humans.
        
          ${Span.Secure.start.text}...any text you would like to have encrypted...${Span.Secure.end.text}
        
        Cipher blocks are created and maintained by the script.
        
          ${Span.Cipher.start.text}...base 64 encrypted data from a secure block...${Span.Cipher.end.text}
    """.trimIndent()
    )
}

data class Config(
    val key: String,
    val endpoint: String?,
    val region: String,
    val mode: String,
    val input: String,
    val output: String,
    val editor: String
)

fun parseArgs(args: Array<String>): Config {
    var key: String? = System.getenv("CIPHER_KEY")
    var endpoint: String? = System.getenv("CIPHER_ENDPOINT")
    var region: String = System.getenv("CIPHER_REGION") ?: DEFAULT_REGION
    val editor = System.getenv("EDITOR") ?: DEFAULT_EDITOR

    var i = 0
    while (i < args.size && args[i].startsWith("--")) {
        when (args[i]) {
            "--key" -> {
                i += 1
                key = args[i]
            }

            "--endpoint" -> {
                i += 1
                endpoint = args[i]
            }

            "--region" -> {
                i += 1
                region = args[i]
            }

            else -> {
                throw UsageException("invalid option ${args[i]}")
            }
        }
        i += 1
    }

    if (key == null) {
        throw UsageException("required --key or CIPHER_KEY not provided")
    }

    val remaining = args.size - i
    if (remaining < 2 || remaining > 3) {
        throw UsageException("expected 2 or 3 arguments")
    }

    val mode = args[i++]
    val input = args[i++]
    val output = if (i < args.size) args[i] else input

    return Config(key, endpoint, region, mode, input, output, editor)
}

fun main(args: Array<String>) {
    val config = parseArgs(args)
    val cipher = KmsCipher(config.endpoint, config.region, config.key)
    val editor = config.editor

    val mode = config.mode
    val inputFile = File(config.input)
    val outputFile = File(config.output)
    when (mode) {
        "cat" -> {
            val original = loadText(inputFile)
            val decrypted = original.decrypt(cipher)
            val printBuffer = Buffer()
            decrypted.store(printBuffer)
            print(printBuffer.readUtf8())
        }

        "encrypt" -> {
            val original = loadText(inputFile)
            val encrypted = original.encrypt(cipher)
            encrypted.store(outputFile.sink())
        }

        "decrypt" -> {
            val original = loadText(inputFile)
            val decrypted = original.decrypt(cipher)
            decrypted.store(outputFile.sink())
        }

        "rewind" -> {
            val original = loadText(inputFile)
            val rewound = original.rewind(cipher)
            rewound.store(outputFile.sink())
        }

        "edit" -> {
            val currentDirectory = Paths.get(".")
            val permissions = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))
            val tempFile = Files.createTempFile(currentDirectory, "edit-", ".txt", permissions).toFile()
            tempFile.deleteOnExit()
            try {
                val original = loadText(inputFile)
                val cipherCache = original.extractCipherCache(cipher)
                val rewound = original.rewind(cipher)
                rewound.store(tempFile.sink())
                ProcessBuilder(listOf(editor, tempFile.path))
                    .redirectInput(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                    .waitFor()
                val modified = loadText(tempFile)
                if (modified.isEmpty()) {
                    println("File is empty - leaving original unchanged.")
                } else if (modified == rewound) {
                    println("File unchanged - leaving original unchanged.")
                } else {
                    val encrypted = modified.encrypt(cipher, cipherCache)
                    encrypted.store(outputFile.sink())
                }
            } finally {
                tempFile.delete()
            }
        }

        else -> {
            usage()
        }
    }
}

try {
    main(args)
} catch (e: UsageException) {
    System.err.println("Usage error: ${e.message}")
    usage()
    exitProcess(1)
} catch (e: ParseException) {
    System.err.println("Parse error: ${e.message}")
    exitProcess(2)
} catch (e: Throwable) {
    System.err.println("Caught exception: ${e.message}")
    e.printStackTrace()
    exitProcess(3)
}
