package com.example.ksp
import com.google.devtools.ksp.*
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*

class MySymbolProcessorProvider:SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return MySymbolProcessor(environment)
    }
}

class MySymbolProcessor(private val environment: SymbolProcessorEnvironment): SymbolProcessor {

    var processCount = 0

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        environment.logger.warn("process--------------------------------$processCount")
        processCount++
        val logger = environment.logger

        val files = resolver.getAllFiles()

        val allClass = mutableListOf<MyClass>()
        val dictNameId = mutableMapOf<String,Int>()

        if (processCount > 1) { // second time pass
            files.forEach { file ->
                logger.warn("file: ${file.fileName}")
            }
            return emptyList()
        }

        files.forEach { file ->
            logger.warn("file: ${file.fileName}")
            file.declarations.forEach { dec ->
                if (dec is KSClassDeclaration) {
                    logger.warn("class: $dec")

                    var mc = MyClass()
                    allClass.add(mc)
                    mc.id = allClass.size
                    mc.file = file.fileName
                    mc.name = dec.toString()
                    mc.kind = "class"
                    mc.detail = mc.name

                    dictNameId[mc.name] = mc.id

                    dec.superTypes.forEach {
                        mc.parents.add(it.toString())
                    }

                    mc.detail += "\\n-------------------------\\n"
                    dec.getAllProperties().forEach { prop ->
                        mc.variables.add(prop.type.toString())
                        mc.detail += "- $prop: ${prop.type}\\n"
                    }

                    mc.detail += "-------------------------\\n"
                    dec.getAllFunctions().forEach { func ->
                        if (func.toString() == "<init>") {
                            // ignore
                        } else {
                            mc.temporaries.add(func.returnType.toString())
                            func.parameters.forEach {
                                mc.temporaries.add(it.type.toString())
                            }
                            val l = func.parameters.joinToString {
                                it.type.toString()
                            }
                            mc.detail += "+ $func($l): ${func.returnType}\\n"
                        }
                    }

                }
            }
        }

        environment.logger.warn("ksp")
        environment.logger.warn(environment.options.toString())

        val f = environment.codeGenerator.createNewFile(
            Dependencies(false),
            "",
            "data",
            extensionName = "json"
        )
        val json = MyClass.toJsonString(allClass)
        logger.warn(json)

        f.write(json.toByteArray())
        f.close()

        return emptyList()
    }

    override fun finish() {
        environment.logger.warn("finish--------------------------------")
    }

    override fun onError() {
        environment.logger.warn("onError!!!")
    }
}

class MyClass {
    var id = 0
    var file = ""
    var name = ""
    var kind = ""
    var detail = ""
    var parents = mutableListOf<String>()
    var protocols = mutableListOf<String>()
    var variables = mutableListOf<String>()
    var temporaries = mutableListOf<String>()

    private fun str(k: String, v: Any): String {
        return "\"$k\":\"$v\""
    }

    private fun list(k: String, list:MutableList<String>): String {
        val l = list.joinToString {
            "\"$it\""
        }
        return "\"$k\":[$l]"
    }

    fun toJsonString(): String {
        return "{\"id\": $id, ${str("file", file)}, ${str("name", name)}, ${str("detail", detail)}, ${str("kind", kind)}, ${list("parents", parents)}, ${list("protocols", protocols)}, ${list("variables", variables)}, ${list("temporaries", temporaries)}}"
    }

    companion object {
        fun toJsonString(list:MutableList<MyClass>): String {
            val l = list.joinToString {
                it.toJsonString()
            }
            return "[$l]"
        }
    }

}
