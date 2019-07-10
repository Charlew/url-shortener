package charlew.urlshortener

import org.apache.commons.validator.routines.UrlValidator
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import com.google.common.hash.Hashing.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import java.nio.charset.StandardCharsets

@Controller
class ShortenerController(private val redis: StringRedisTemplate) {

    @GetMapping(value = ["/{id}"])
    fun redirect(@PathVariable id: String, response: HttpServletResponse) {
        val url = redis.opsForValue().get(id)

        if (url != null) {
            response.sendRedirect(url)
            return
        }
        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }

    @PostMapping
    fun create(request : HttpServletRequest) : ResponseEntity<String> { 
        val queryParams = if (request.queryString != null) "?" + request.queryString else ""
        val url = (request.requestURI + queryParams).substring(1)
        val appUrl = request.scheme + "://" + request.serverName + request.serverPort

        return if (UrlValidator(arrayOf("http", "https")).isValid(url)) {
            val id = murmur3_32().hashString(url, StandardCharsets.UTF_8).toString()

            redis.opsForValue().set(id, url)
            ResponseEntity(appUrl + "/$id", HttpStatus.OK)
        } else {
            ResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

}