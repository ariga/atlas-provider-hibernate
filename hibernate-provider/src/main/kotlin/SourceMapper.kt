package io.atlasgo

import org.hibernate.boot.Metadata
import java.io.File

fun extractSourceMappings(sourcesListFile: File, metadata: Metadata): List<String> {
    val sourceMap = makeSourceMap(sourcesListFile)

    // mappers should return list of strings in this format: -- atlas:pos public[type=schema].email[type=table] test/load-models.test.kt:19:25
    data class TableMapping(val schema: String?, val name: String, val className: String)

    val mappers = metadata.collectTableMappings().mapNotNull { t ->
        // try finding an entity
        metadata.entityBindings.find { it.table.exportIdentifier == t.exportIdentifier }?.let { entity ->
            return@mapNotNull TableMapping(entity.table.schema, t.name, entity.entityName)
        }
        // if it's not an entity, it might be a collection
        metadata.collectionBindings.find { it.collectionTable.exportIdentifier == t.exportIdentifier }
            ?.let { collection ->
                return@mapNotNull TableMapping(collection.table.schema, t.name, collection.ownerEntityName)
            }
        // otherwise, check if it's a join table
        metadata.entityBindings.flatMap { entity -> entity.joins.map { entity to it } }
            .find { (_, join) -> join.table.exportIdentifier == t.exportIdentifier }?.let { (entity, join) ->
                return@mapNotNull TableMapping(
                    join.table.schema,
                    t.name,
                    entity.entityName
                )
            }
    }
    return mappers.mapNotNull {
        val schemaPart = it.schema?.let { schema ->
            "$schema[type=schema]."
        } ?: ""
        sourceMap[it.className]?.let { source ->
            "-- atlas:pos ${schemaPart}${it.name} $source"
        }
    }
}

fun makeSourceMap(sourceListFile: File): Map<String, String> {
    if (!sourceListFile.exists()) {
        System.err.println("Error: Source list file not found at ${sourceListFile.absolutePath}")
        return emptyMap()
    }
    return sourceListFile.readLines()
        .map { File(it) }
        .map { processSourceFile(it) }
        .reduce { acc, map -> acc + map }
}

val packageRegex = Regex("""^\s*package\s+([a-zA-Z0-9_.]+)""", RegexOption.MULTILINE)
// Best effort regex to find class definitions in source files.
val typeRegex = Regex("""^(?![ \t]*(?:\/\/|\/\*|\*)).*?\bclass\s+(\w+)""", RegexOption.MULTILINE)

fun processSourceFile(sourceFile: File): Map<String, String> {
    if (!sourceFile.exists()) {
        return emptyMap()
    }
    val fileContent = try {
        sourceFile.readText()
    } catch (e: Exception) {
        System.err.println("Error reading file ${sourceFile.path}: ${e.message}")
        return emptyMap()
    }

    val packageMatch = packageRegex.find(fileContent)
    val packageName = packageMatch?.groups?.get(1)?.value ?: ""

    val mapping = mutableMapOf<String, String>()
    fileContent.lineSequence().forEachIndexed() { i, line ->
        typeRegex.findAll(line).forEach { matchResult ->
            val match = matchResult.groups[1] ?: return@forEach
            val typeName = match.value
            mapping["$packageName.$typeName"] = "${sourceFile.path}:${i + 1}"
        }
    }
    // Fallback for simple Java files that might be missed by the regex.
    if (mapping.isEmpty() && sourceFile.name.endsWith(".java")) {
        val classNameFromFile = sourceFile.nameWithoutExtension
        mapping["$packageName.$classNameFromFile"] = "${sourceFile.path}:1"
    }
    return mapping
}