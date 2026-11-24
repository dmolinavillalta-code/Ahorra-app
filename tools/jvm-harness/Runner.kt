import java.io.File
import java.lang.reflect.InvocationTargetException

fun main(args: Array<String>) {
    val dir = File(args[0])
    val loader = java.net.URLClassLoader(arrayOf(dir.toURI().toURL()), Thread.currentThread().contextClassLoader)
    var passed = 0; var failed = 0
    dir.walkTopDown().filter { it.name.endsWith("Test.class") && !it.path.contains("/kotlin/") }.sortedBy { it.path }.forEach { f ->
        val name = f.relativeTo(dir).path.removeSuffix(".class").replace('/', '.')
        val cls = loader.loadClass(name)
        val testAnn = loader.loadClass("kotlin.test.Test")
        @Suppress("UNCHECKED_CAST")
        val methods = cls.declaredMethods.filter { m -> m.annotations.any { it.annotationClass.java == testAnn } }.sortedBy { it.name }
        val inst = cls.getDeclaredConstructor().newInstance()
        for (m in methods) {
            try { m.invoke(inst); passed++; println("PASS ${cls.simpleName}.${m.name}") }
            catch (e: InvocationTargetException) { failed++; println("FAIL ${cls.simpleName}.${m.name}: ${e.targetException}") }
        }
    }
    println("--- $passed passed, $failed failed")
    if (failed > 0) System.exit(1)
}
