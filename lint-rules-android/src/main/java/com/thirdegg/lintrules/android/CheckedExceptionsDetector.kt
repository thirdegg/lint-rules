package com.thirdegg.lintrules.android

import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Severity.WARNING
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.KotlinUFunctionCallExpression
import java.util.*
import org.jetbrains.uast.visitor.AbstractUastVisitor
import kotlin.collections.ArrayList


val ISSUE_PATTERN = Issue.create("CheckedExceptions",
        "Checked exceptions for kotlin",
        "Checked exceptions for kotlin",
        CORRECTNESS,
        7,
        WARNING,
        Implementation(CheckedExceptionsDetector::class.java, EnumSet.of(Scope.JAVA_FILE))
)

class CheckedExceptionsDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes() = listOf<Class<out UElement>>(UCallExpression::class.java)

    fun <T : UElement> findParentByUast(item:UElement, clazz:Class<T>):T? {
        var parent = item.uastParent
        while (parent!=null) {
            if (clazz.isInstance(parent)) {
                return (parent as T)
            }
            parent = parent.uastParent
        }
        return null
    }

    fun findExceptionClassName(catchClause: UCatchClause):String {
        return catchClause.parameters[0].psi.type.canonicalText
    }

    fun findNamedExpressionsInAnnotation(uAnnotation: UAnnotation):ArrayList<String?> {
        val namedExpressions = ArrayList<String?>()
        for (uNamedExpression in uAnnotation.attributeValues) {
            if (uNamedExpression.expression is UClassLiteralExpression) {
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

    override fun createUastHandler(context: JavaContext) = object:UElementHandler() {

        init {
//            println(context.uastFile?.asRecursiveLogString())
        }

        override fun visitCallExpression(node: UCallExpression) {

            val parentNode = node
            val method = parentNode.resolve() ?: return
            val uMethod = context.uastContext.getMethod(method)

            val haveTryCatch = ArrayList<String>()

            findParentByUast(parentNode, UTryExpression::class.java).also { tryException ->
                tryException?.catchClauses?:return@also
                for (catchCause in tryException.catchClauses) {
                    haveTryCatch.add(findExceptionClassName(catchCause))
                }
            }


            findParentByUast(parentNode,UAnnotationMethod::class.java)?.also { throwsAnnotation->
                for (annotation in throwsAnnotation.annotations) {
                    if (annotation.qualifiedName != "kotlin.jvm.Throws") continue
                    for (throwsException in findNamedExpressionsInAnnotation(annotation)) {
                        throwsException ?: continue
                        if (!haveTryCatch.contains(throwsException)) haveTryCatch.add(throwsException)
                    }
                }
            }

            //find throws annotation in method
            uMethod.accept(object : AbstractUastVisitor() {
                override fun visitAnnotation(node: UAnnotation): Boolean {

                    if (node.qualifiedName!="kotlin.jvm.Throws") return super.visitAnnotation(node)

                    node.accept(object : AbstractUastVisitor() {

                        override fun visitClassLiteralExpression(node: UClassLiteralExpression): Boolean {

                            val clazzName = node.type?.canonicalText?:return super.visitClassLiteralExpression(node)

                            if (haveTryCatch.contains(clazzName))
                                return super.visitClassLiteralExpression(node)

                            node.type?.superTypes?.forEach {
                                if (haveTryCatch.contains(it.canonicalText))
                                    return super.visitClassLiteralExpression(node)
                            }

                            context.report(ISSUE_PATTERN, parentNode, context.getNameLocation(parentNode),
                                    "Unhandled exception: $clazzName")

                            return super.visitClassLiteralExpression(node)

                        }

                    })

                    return super.visitAnnotation(node)
                }

            })

            //find in suspendCoroutine resumeWithException
            uMethod.accept(object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    if (node.uastParent !is UCallExpression) return super.visitCallExpression(node)
                    val parentResolve = (node.uastParent as UCallExpression).resolve()
                    val resolve = node.resolve()
                    if (parentResolve?.containingClass?.qualifiedName?.contains("Continuation") != true) {
                        return super.visitCallExpression(node)
                    }

                    if ((node.uastParent as UCallExpression?)?.methodName != "resumeWithException")
                        return super.visitCallExpression(node)

                    val clazzName = resolve?.containingClass?.qualifiedName
                            ?: return super.visitCallExpression(node)

                    if (haveTryCatch.contains(clazzName))
                        return super.visitCallExpression(node)

                    var superClass = resolve.containingClass?.superClass
                    while (superClass!=null) {
                        if (haveTryCatch.contains(superClass.qualifiedName))
                            return super.visitCallExpression(node)
                        superClass = superClass.superClass
                    }

                    context.report(ISSUE_PATTERN, parentNode, context.getNameLocation(parentNode),
                            "Unhandled exception: $clazzName")

                    return super.visitCallExpression(node)
                }
            })

            //find throw in method
            uMethod.accept(object : AbstractUastVisitor() {

                override fun visitThrowExpression(node: UThrowExpression): Boolean {

                    node.accept(object : AbstractUastVisitor() {

                        override fun visitCallExpression(node: UCallExpression): Boolean {
                            if (node is KotlinUFunctionCallExpression) {
                                //TODO kotlin.Exception() not catch
                                val clazz = node.resolve()
                                val clazzName = clazz?.containingClass?.qualifiedName?:return super.visitCallExpression(node)
                                if (haveTryCatch.contains(clazzName)) return super.visitCallExpression(node)

                                var superClass = clazz.containingClass?.superClass
                                while (superClass!=null) {
                                    if (haveTryCatch.contains(superClass?.qualifiedName))
                                        return super.visitCallExpression(node)
                                    superClass = superClass?.superClass
                                }
                                context.report(ISSUE_PATTERN, parentNode, context.getNameLocation(parentNode), "Unhandled exception: $clazzName")
                            }
                            return super.visitCallExpression(node)
                        }

                    })

                    return super.visitThrowExpression(node)
                }

            })
        }


    }
}