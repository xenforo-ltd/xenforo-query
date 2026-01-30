package com.github.xenforo.query.utils

import com.github.xenforo.query.constants.FinderMethods
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.AssignmentExpression
import com.jetbrains.php.lang.psi.elements.ClassConstantReference
import com.jetbrains.php.lang.psi.elements.ClassReference
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpTypedElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.Variable
import com.jetbrains.php.lang.psi.resolve.types.PhpType

/**
 * Resolves the table name for a XenForo Entity Finder.
 *
 * Resolution chain:
 * 1. Find the Finder class from the method call (e.g., ThreadFinder::class)
 * 2. Parse `@extends Finder<EntityClass>` from Finder's PHPDoc to get Entity class
 * 3. Find Entity's `getStructure()` method
 * 4. Parse `$structure->table = 'table_name'` to get table name
 */
object FinderEntityResolver {
    data class FinderTableInfo(val tableName: String, val entityClass: String?, val finderClass: String?)

    fun resolveTable(methodRef: MethodReference): FinderTableInfo? {
        if (!methodRef.isValid) return null

        val project = methodRef.project

        val finderFqn = resolveFinderClassFqn(methodRef, project) ?: return null
        val entityFqn = resolveEntityFqnFromFinder(finderFqn, project) ?: return null
        val tableName = resolveTableFromEntity(entityFqn, project) ?: return null

        return FinderTableInfo(tableName = tableName, entityClass = entityFqn, finderClass = finderFqn)
    }

    private fun resolveFinderClassFqn(methodRef: MethodReference, project: Project): String? {
        return resolveFinderClassFqnRecursive(methodRef, project, mutableSetOf())
    }

    private fun resolveFinderClassFqnRecursive(
        element: PsiElement?,
        project: Project,
        visited: MutableSet<PsiElement>,
    ): String? {
        if (element == null || !element.isValid || element in visited) return null
        visited.add(element)

        when (element) {
            is MethodReference -> {
                val methodName = element.name

                if (methodName == "finder" || methodName == "getFinder") {
                    val finderArg = element.parameters.firstOrNull()

                    if (finderArg is ClassConstantReference) {
                        val classRef = finderArg.classReference
                        if (classRef is ClassReference) {
                            classRef.fqn?.let {
                                return it
                            }
                        }
                    } else if (finderArg is StringLiteralExpression) {
                        resolveXenForoShortName(finderArg.contents, "Finder", project)?.let {
                            return it
                        }
                    }
                }

                val classRef = element.classReference
                if (classRef != null && classRef.isValid) {
                    resolveFinderClassFqnRecursive(classRef, project, visited)?.let {
                        return it
                    }
                }

                getFinderFqnFromType(element.type)?.let {
                    return it
                }

                if (classRef is PhpTypedElement && classRef.isValid) {
                    getFinderFqnFromType(classRef.type)?.let {
                        return it
                    }
                }
            }

            is Variable -> {
                if (!element.isValid) return null

                val resolved = resolveVariableAssignment(element)
                if (resolved != null) {
                    resolveFinderClassFqnRecursive(resolved, project, visited)?.let {
                        return it
                    }
                }

                getFinderFqnFromType(element.type)?.let {
                    return it
                }
            }

            is PhpTypedElement -> {
                if (!element.isValid) return null
                return getFinderFqnFromType(element.type)
            }
        }

        return null
    }

    private fun resolveXenForoShortName(shortName: String, suffix: String, project: Project): String? {
        if (!shortName.contains(":")) return null

        val parts = shortName.split(":", limit = 2)
        if (parts.size != 2) return null

        val prefix = parts[0]
        val name = parts[1]

        val fqn =
            when (prefix) {
                "XF" -> "\\XF\\$suffix\\$name"
                else -> "\\$prefix\\$suffix\\$name"
            }

        val phpIndex = PhpIndex.getInstance(project)
        return if (phpIndex.getClassesByFQN(fqn).isNotEmpty()) fqn else null
    }

    private fun getFinderFqnFromType(type: PhpType): String? {
        for (typeName in type.types) {
            if (typeName.startsWith("#") || typeName.contains("(")) continue

            if (isFinderTypeName(typeName)) {
                return if (typeName.startsWith("\\")) typeName else "\\$typeName"
            }
        }
        return null
    }

    private fun isFinderTypeName(typeName: String): Boolean {
        val normalizedType = if (typeName.startsWith("\\")) typeName else "\\$typeName"

        return normalizedType.equals(FinderMethods.FINDER_FQN, ignoreCase = true) ||
            normalizedType.contains("\\Finder\\", ignoreCase = true) ||
            (normalizedType.startsWith("\\XF\\", ignoreCase = true) &&
                normalizedType.endsWith("Finder", ignoreCase = true))
    }

    private fun resolveEntityFqnFromFinder(finderFqn: String, project: Project): String? {
        val phpIndex = PhpIndex.getInstance(project)
        val finderClasses = phpIndex.getClassesByFQN(finderFqn)

        for (finderClass in finderClasses) {
            if (!finderClass.isValid) continue

            resolveEntityFromFinderClass(finderClass, project)?.let {
                return it
            }
        }
        return null
    }

    /** Parses `@extends Finder<EntityClass>` from the Finder's PHPDoc. */
    private fun resolveEntityFromFinderClass(finderClass: PhpClass, project: Project): String? {
        if (!finderClass.isValid) return null

        val docComment = finderClass.docComment
        if (docComment == null || !docComment.isValid) return null

        val extendsPattern = Regex("""@extends\s+Finder\s*<\s*([^>]+)\s*>""")
        val match = extendsPattern.find(docComment.text) ?: return null
        val entityRef = match.groupValues[1].trim()

        return resolveEntityFqn(entityRef, finderClass, project)
    }

    private fun resolveEntityFqn(entityRef: String, contextClass: PhpClass, project: Project): String? {
        if (entityRef.startsWith("\\")) return entityRef

        val phpIndex = PhpIndex.getInstance(project)

        // Check use statements
        if (contextClass.isValid) {
            val containingFile = contextClass.containingFile
            if (containingFile is com.jetbrains.php.lang.psi.PhpFile && containingFile.isValid) {
                val useStatements =
                    PsiTreeUtil.findChildrenOfType(
                        containingFile,
                        com.jetbrains.php.lang.psi.elements.PhpUseList::class.java,
                    )
                for (useList in useStatements) {
                    if (!useList.isValid) continue
                    for (declaration in useList.declarations) {
                        if (!declaration.isValid) continue
                        val alias = declaration.aliasName ?: declaration.name
                        if (alias == entityRef) {
                            return declaration.fqn
                        }
                    }
                }
            }
        }

        // Try XenForo namespace convention: \XF\Finder\ -> \XF\Entity\
        val finderNamespace = if (contextClass.isValid) contextClass.namespaceName else ""

        if (finderNamespace.startsWith("\\XF\\Finder") || finderNamespace.startsWith("XF\\Finder")) {
            val entityFqn = "\\XF\\Entity\\$entityRef"
            if (phpIndex.getClassesByFQN(entityFqn).isNotEmpty()) {
                return entityFqn
            }
        }

        // Generic: replace \Finder\ with \Entity\ in namespace
        val entityNamespace = finderNamespace.replace("\\Finder", "\\Entity")
        val entityFqn = "$entityNamespace\\$entityRef"

        if (phpIndex.getClassesByFQN(entityFqn).isNotEmpty()) {
            return entityFqn
        }

        val normalizedFqn = if (entityFqn.startsWith("\\")) entityFqn else "\\$entityFqn"
        if (phpIndex.getClassesByFQN(normalizedFqn).isNotEmpty()) {
            return normalizedFqn
        }

        return null
    }

    private fun resolveTableFromEntity(entityFqn: String, project: Project): String? {
        val phpIndex = PhpIndex.getInstance(project)
        val entityClasses = phpIndex.getClassesByFQN(entityFqn)

        for (entityClass in entityClasses) {
            if (!entityClass.isValid) continue

            extractTableFromGetStructure(entityClass)?.let {
                return it
            }
        }

        return null
    }

    /** Finds `$structure->table = 'table_name'` in the Entity's getStructure() method. */
    private fun extractTableFromGetStructure(entityClass: PhpClass): String? {
        if (!entityClass.isValid) return null

        val getStructureMethod = entityClass.findMethodByName("getStructure") ?: return null
        if (!getStructureMethod.isValid) return null

        val assignments = PsiTreeUtil.findChildrenOfType(getStructureMethod, AssignmentExpression::class.java)

        for (assignment in assignments) {
            if (!assignment.isValid) continue

            val variable = assignment.variable ?: continue
            if (!variable.isValid) continue

            if (variable is com.jetbrains.php.lang.psi.elements.FieldReference) {
                if (variable.name == "table") {
                    val value = assignment.value
                    if (value is StringLiteralExpression && value.isValid) {
                        return value.contents
                    }
                }
            }
        }

        return null
    }

    private fun resolveVariableAssignment(variable: Variable): PsiElement? {
        return VariableAssignmentResolver.resolveLatestMethodReference(variable)
    }
}
