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

        val allClass = mutableMapOf<String, MyClass>()

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
                    mc.id = allClass.size
                    mc.file = file.fileName
                    mc.name = dec.qualifiedName?.asString() ?: dec.toString()
                    mc.kind = dec.classKind.toString()
                    mc.label = dec.toString()

                    allClass[mc.name] = mc

                    dec.superTypes.forEach {
                        val declaration = it.resolve().declaration
                        (declaration as? KSClassDeclaration)?.classKind?.let {s->
                            if (s == ClassKind.INTERFACE && dec.classKind != ClassKind.INTERFACE) {
                                mc.realization.add(qualifiedName(it))
                            } else {
                                mc.generalization.add(qualifiedName(it))
                            }
                        }
                    }

                    mc.label += "\\n-------------------------\\n"
                    dec.getAllProperties().forEach { prop ->
                        mc.association.add(qualifiedName(prop.type))
                        mc.label += "- $prop: ${prop.type}\\n"
                    }

                    mc.label += "-------------------------\\n"
                    dec.getDeclaredFunctions().forEach { func ->
                        if (func.toString() == "<init>" || func.toString().startsWith("synthetic constructor for")) {
                            // ignore
                        } else {
                            func.returnType?.let {
                                mc.dependency.add(qualifiedName(it))
                            }
                            func.parameters.forEach {
                                mc.dependency.add(qualifiedName(it.type))
                            }
                            val l = func.parameters.joinToString {
                                it.type.toString()
                            }
                            mc.label += "+ $func($l): ${func.returnType}\\n"
                        }
                    }
                }
            }
        }

        saveNodesFile(allClass)
        saveEdgesFile(allClass)

        return emptyList()
    }

    private fun saveNodesFile(allClass: MutableMap<String, MyClass>) {
        val f = environment.codeGenerator.createNewFile(
            Dependencies(false),
            "",
            "nodes",
            extensionName = "json"
        )
        val json = MyClass.toJsonString(allClass.values)
        f.write(json.toByteArray())
        f.close()
    }

    private fun saveEdgesFile(allClass: MutableMap<String, MyClass>) {
        val edges = mutableListOf<MyEdge>()
        val f = environment.codeGenerator.createNewFile(
            Dependencies(false),
            "",
            "edges",
            extensionName = "json"
        )
        allClass.values.forEach { from ->
            from.generalization.forEach {
                allClass[it]?.let { to ->
                    edges.add(MyEdge("generalization", from.id, to.id))
                }
            }
            from.realization.forEach {
                allClass[it]?.let { to ->
                    edges.add(MyEdge("realization", from.id, to.id))
                }
            }
            from.association.forEach {
                allClass[it]?.let { to ->
                    edges.add(MyEdge("association", from.id, to.id))
                }
            }
            from.dependency.forEach {
                allClass[it]?.let { to ->
                    edges.add(MyEdge("dependency", from.id, to.id))
                }
            }
        }
        val json = MyEdge.toJsonString(edges)
        f.write(json.toByteArray())
        f.close()
    }

    private fun qualifiedName(type: KSTypeReference) : String {
        var ret = type.toString()
        try {
            val typeDeclaration = type.resolve().declaration
            if (typeDeclaration is KSClassDeclaration) {
                ret = typeDeclaration.qualifiedName?.asString() ?: typeDeclaration.toString()
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return ret
    }

    override fun finish() {
        environment.logger.warn("finish--------------------------------")
    }

    override fun onError() {
        environment.logger.warn("onError!!!")
    }
}

data class MyEdge(var kind: String,
                  var from: Int,
                  var to: Int) {

    private fun str(k: String, v: Any): String {
        return "\"$k\":\"$v\""
    }
    fun toJsonString(): String {
        return "{${str("kind", kind)}, ${str("from", from)}, ${str("to", to)}}"
    }

    companion object {
        fun toJsonString(list:Iterable<MyEdge>): String {
            val l = list.joinToString {
                it.toJsonString()
            }
            return "[$l]"
        }
    }

}

class MyClass {
    var id = 0
    var file = ""
    var name = ""
    var kind = ""
    var label = ""
    var generalization = mutableListOf<String>()
    var realization = mutableListOf<String>()
    var association = mutableSetOf<String>()
    var dependency = mutableSetOf<String>()

    private fun str(k: String, v: Any): String {
        return "\"$k\":\"$v\""
    }

    private fun list(k: String, list:Iterable<String>): String {
        val l = list.joinToString {
            "\"$it\""
        }
        return "\"$k\":[$l]"
    }

    fun toJsonString(): String {
        return "{\"id\": $id, ${str("file", file)}, ${str("name", name)}, ${str("label", label)}, ${str("kind", kind)}, ${list("generalization", generalization)}, ${list("realization", realization)}, ${list("association", association)}, ${list("dependency", dependency)}}"
    }

    companion object {
        fun toJsonString(list:Iterable<MyClass>): String {
            val l = list.joinToString {
                it.toJsonString()
            }
            return "[$l]"
        }
    }

}
