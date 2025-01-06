import java.net.URLEncoder


fun String?.urlEncode() = try {
    URLEncoder.encode(this, "utf-8")
} catch (t: Throwable) {
    null
}