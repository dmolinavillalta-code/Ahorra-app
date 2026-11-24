package kotlin.test

annotation class Test

class AssertionFailed(msg: String) : AssertionError(msg)

fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    if (expected != actual) throw AssertionFailed((message ?: "") + " expected:<$expected> but was:<$actual>")
}
fun assertTrue(actual: Boolean, message: String? = null) { if (!actual) throw AssertionFailed(message ?: "expected true") }
fun assertFalse(actual: Boolean, message: String? = null) { if (actual) throw AssertionFailed(message ?: "expected false") }
fun assertNull(actual: Any?, message: String? = null) { if (actual != null) throw AssertionFailed((message ?: "") + " expected null but was:<$actual>") }
fun <T : Any> assertNotNull(actual: T?, message: String? = null): T = actual ?: throw AssertionFailed(message ?: "expected non-null")
fun <T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
    if (illegal == actual) throw AssertionFailed((message ?: "") + " illegal value: <$actual>")
}
fun assertContentEquals(expected: ByteArray?, actual: ByteArray?, message: String? = null) {
    if (!java.util.Arrays.equals(expected, actual)) throw AssertionFailed((message ?: "") + " arrays differ")
}
inline fun <reified T : Throwable> assertFailsWith(message: String? = null, block: () -> Unit): T {
    try { block() } catch (e: Throwable) {
        if (e is T) return e
        throw AssertionFailed((message ?: "") + " expected ${T::class.java.simpleName} but was $e")
    }
    throw AssertionFailed((message ?: "") + " expected ${T::class.java.simpleName} but nothing was thrown")
}
