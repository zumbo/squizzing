package ch.quizzing.squizzing.service

import ch.quizzing.squizzing.config.AppProperties
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class ImageStorageService(
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(ImageStorageService::class.java)
    private lateinit var uploadPath: Path

    @PostConstruct
    fun init() {
        uploadPath = Paths.get(appProperties.uploadDir).toAbsolutePath().normalize()
        try {
            Files.createDirectories(uploadPath)
            log.info("Upload directory initialized: {}", uploadPath)
        } catch (e: IOException) {
            throw RuntimeException("Could not create upload directory: $uploadPath", e)
        }
    }

    fun store(file: MultipartFile, subdirectory: String = ""): String {
        if (file.isEmpty) {
            throw IllegalArgumentException("Cannot store empty file")
        }

        val originalFilename = file.originalFilename ?: "file"
        val extension = originalFilename.substringAfterLast('.', "")
        val newFilename = "${UUID.randomUUID()}.$extension"

        val targetDir = if (subdirectory.isNotEmpty()) {
            uploadPath.resolve(subdirectory).also { Files.createDirectories(it) }
        } else {
            uploadPath
        }

        val targetPath = targetDir.resolve(newFilename)

        file.inputStream.use { inputStream ->
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        log.info("Stored file: {}", targetPath)

        return if (subdirectory.isNotEmpty()) "$subdirectory/$newFilename" else newFilename
    }

    fun load(filename: String): Resource {
        val filePath = uploadPath.resolve(filename).normalize()
        val resource = UrlResource(filePath.toUri())

        if (resource.exists() && resource.isReadable) {
            return resource
        }

        throw RuntimeException("Could not read file: $filename")
    }

    fun delete(filename: String) {
        try {
            val filePath = uploadPath.resolve(filename).normalize()
            Files.deleteIfExists(filePath)
            log.info("Deleted file: {}", filePath)
        } catch (e: IOException) {
            log.warn("Could not delete file: {}", filename, e)
        }
    }

    fun exists(filename: String): Boolean {
        val filePath = uploadPath.resolve(filename).normalize()
        return Files.exists(filePath)
    }
}
