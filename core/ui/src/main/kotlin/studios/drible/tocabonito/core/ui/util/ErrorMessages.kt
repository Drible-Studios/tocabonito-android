package studios.drible.tocabonito.core.ui.util

fun Throwable.toUserMessage(): String = when (this) {
    is java.net.UnknownHostException -> "Check your internet connection"
    is java.net.SocketTimeoutException -> "Connection timed out"
    is kotlinx.serialization.SerializationException -> "Invalid response from server"
    else -> message ?: "Something went wrong"
}
