package org.flux.scripting.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LuaApiClass(
    val name: String,
    val description: String = ""
)

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class LuaApiField(
    val type: String,
    val description: String = ""
)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class LuaApiFunction(
    val params: Array<String> = [],
    val returnType: String = "nil",
    val description: String = ""
)

@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class LuaApiGlobal(
    val name: String,
    val type: String,
    val description: String = ""
)
