package ch.quizzing.squizzing.controller

import ch.quizzing.squizzing.service.ImageStorageService
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/images")
class ImageController(
    private val imageStorageService: ImageStorageService
) {

    @GetMapping("/{*filename}")
    fun serveImage(@PathVariable filename: String): ResponseEntity<Resource> {
        val resource = imageStorageService.load(filename)

        val contentType = when {
            filename.endsWith(".png", ignoreCase = true) -> MediaType.IMAGE_PNG
            filename.endsWith(".gif", ignoreCase = true) -> MediaType.IMAGE_GIF
            filename.endsWith(".webp", ignoreCase = true) -> MediaType.parseMediaType("image/webp")
            else -> MediaType.IMAGE_JPEG
        }

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
            .body(resource)
    }
}
