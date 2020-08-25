package com.thirdegg.lintrules.android

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Severity.WARNING
import com.intellij.psi.PsiType
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


val ISSUE_PATTERN = Issue.create(
    "CheckedExceptions",
    "Checked exceptions for kotlin",
    "Checked exceptions for kotlin",
    CORRECTNESS,
    7,
    WARNING,
    Implementation(CheckedExceptionsDetector::class.java, EnumSet.of(Scope.JAVA_FILE))
)

class CheckedExceptionsDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf<Class<out UElement>>(UCallExpression::class.java)

    fun <T : UElement> findParentByUast(item: UElement, clazz: Class<T>): T? {
        var parent = item.uastParent
        while (parent != null) {
            if (clazz.isInstance(parent)) {
                return (parent as T)
            }
            parent = parent.uastParent
        }
        return null
    }

    fun findExceptionClassName(catchClause: UCatchClause): String {
        return catchClause.parameters[0].psi.type.canonicalText
    }

    fun findNamedExpressionsInAnnotation(uAnnotation: UAnnotation): ArrayList<String?> {
        val namedExpressions = ArrayList<String?>()
        for (uNamedExpression in uAnnotation.attributeValues) {
            if (uNamedExpression.expression is UClassLiteralExpression) {
                // If UClassLiteralExpression.type.canonicalText is null then maybe no import of Exception
                namedExpressions.add((uNamedExpression.expression as UClassLiteralExpression).type?.canonicalText)
                continue
            }
            if (uNamedExpression.expression is UCallExpression) {
                for (argument in (uNamedExpression.expression as UCallExpression).valueArguments) {
                    if (argument is UClassLiteralExpression) {
                        namedExpressions.add(argument.type?.canonicalText)
                        continue
                    }
                }
            }
        }
        return namedExpressions
    }

    fun findRecursiveExtensionsInClass(superTypes: Array<PsiType>): HashSet<String> {
        val classes = HashSet<String>()
        superTypes.forEach {
            classes.addAll(findRecursiveExtensionsInClass(it.superTypes))
            classes.add(it.canonicalText)
        }
        return classes
    }

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        init {
//            println(context.uastFile?.asRecursiveLogString())
        }

        override fun visitCallExpression(node: UCallExpression) {

            val parentNode = node
            val method = parentNode.resolve() ?: return
            val uMethod = context.uastContext.getMethod(method)

            val ignoreCatch = HashSet<String>()

            // Has try/catch expression
            findParentByUast(parentNode, UTryExpression::class.java).also { tryException ->
                tryException?.catchClauses ?: return@also
                for (catchCause in tryException.catchClauses) {
                    ignoreCatch.add(findExceptionClassName(catchCause))
                }
            }

            // Has @Throws in annotation expression
            findParentByUast(parentNode, UAnnotationMethod::class.java)?.also { throwsAnnotation ->
                for (annotation in throwsAnnotation.annotations) {
                    if (annotation.qualifiedName != "kotlin.jvm.Throws") continue
                    for (throwsException in findNamedExpressionsInAnnotation(annotation)) {
                        throwsException ?: continue
                        if (ignoreCatch.contains(throwsException)) continue;
                        ignoreCatch.add(throwsException)
                    }
                }
            }

            uMethod.accept(object : AbstractUastVisitor() {

                // Find throws annotation in method
                override fun visitAnnotation(node: UAnnotation): Boolean {

                    if (node.qualifiedName != "kotlin.jvm.Throws") return super.visitAnnotation(node)

                    node.accept(object : AbstractUastVisitor() {

                        override fun visitClassLiteralExpression(node: UClassLiteralExpression): Boolean {

                            val arrayList = ArrayList<String>()

                            val clazzName = node.type?.canonicalText
                                ?: return super.visitClassLiteralExpression(node)

                            arrayList.add(clazzName)

                            if (ignoreCatch.contains(clazzName))
                                return super.visitClassLiteralExpression(node)

                            if (node.type?.superTypes != null) {
                                arrayList.addAll(findRecursiveExtensionsInClass(node.type?.superTypes!!))
                            }

                            for (clazz in arrayList) {
                                if (ignoreCatch.contains(clazz))
                                    return super.visitClassLiteralExpression(node)
                            }

                            context.report(
                                ISSUE_PATTERN, parentNode, context.getNameLocation(parentNode),
                                "Unhandled exception: $clazzName"
                            )

                            return super.visitClassLiteralExpression(node)

                        }

                    })

                    return super.visitAnnotation(node)
                }
            })

            uMethod.accept(object : AbstractUastVisitor() {

                // Find throw in method
                override fun visitThrowExpression(node: UThrowExpression): Boolean {

                    node.accept(object : AbstractUastVisitor() {

                        override fun visitCallExpression(node: UCallExpression): Boolean {
                            //TODO kotlin.Exception() not catch
                            val clazz = node.resolve()
                            val clazzName = clazz?.containingClass?.qualifiedName
                                ?: return super.visitCallExpression(node)
                            if (ignoreCatch.contains(clazzName)) return super.visitCallExpression(node)

                            var superClass = clazz.containingClass?.superClass
                            while (superClass != null) {
                                if (ignoreCatch.contains(superClass.qualifiedName)) {
                                    return super.visitCallExpression(node)
                                }
                                superClass = superClass.superClass
                            }
                            context.report(
                                ISSUE_PATTERN,
                                parentNode,
                                context.getNameLocation(parentNode),
                                "Unhandled exception: $clazzName"
                            )
                            return super.visitCallExpression(node)
                        }

                    })

                    return super.visitThrowExpression(node)
                }

            })

        }
    }
}