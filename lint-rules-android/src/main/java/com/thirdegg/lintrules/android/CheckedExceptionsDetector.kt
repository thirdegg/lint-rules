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

    fun <T : UElement> findParrentByUast(item:UElement, clazz:Class<T>):T? {
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

    override fun createUastHandler(context: JavaContext) = object:UElementHandler() {

        init {
            println(context.uastFile?.asRecursiveLogString())
        }

        override fun visitCallExpression(node: UCallExpression) {

            val parrentNode = node
            val method = parrentNode.resolve() ?: return
            val uMethod = context.uastContext.getMethod(method)

            val haveTryCatch = ArrayList<String>()

            val tryException = findParrentByUast(parrentNode, UTryExpression::class.java)
            if (tryException != null) {
                for (catchCause in tryException.catchClauses) {
                    haveTryCatch.add(findExceptionClassName(catchCause))
                }
            }

            //throws
            uMethod.accept(object : AbstractUastVisitor() {
                override fun visitAnnotation(node: UAnnotation): Boolean {

                    if (node.qualifiedName!="kotlin.jvm.Throws") return super.visitAnnotation(node)
                    node.accept(object : AbstractUastVisitor() {

                        override fun visitClassLiteralExpression(node: UClassLiteralExpression): Boolean {

                            val clazzName = node.type?.canonicalText?:return super.visitClassLiteralExpression(node)

                            if (haveTryCatch.contains(clazzName))
                                return super.visitClassLiteralExpression(node)

                            val uClass = context.uastContext.getClass(node.getContainingUClass()!!)

                            var superClass = uClass.superClass
                            while (superClass!=null) {
                                if (haveTryCatch.contains(superClass.qualifiedName))
                                    return super.visitClassLiteralExpression(node)
                                superClass = superClass.superClass
                            }
//
                            context.report(ISSUE_PATTERN, parrentNode, context.getNameLocation(parrentNode),
                                    "Exception not checked: $clazzName")

                            return super.visitClassLiteralExpression(node)
                        }

                    })

                    return super.visitAnnotation(node)
                }

            })

            //coroutines
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
                        if (haveTryCatch.contains(superClass?.qualifiedName))
                            return super.visitCallExpression(node)
                        superClass = superClass?.superClass
                    }

                    context.report(ISSUE_PATTERN, parrentNode, context.getNameLocation(parrentNode),
                            "Exception not checked: $clazzName")

                    return super.visitCallExpression(node)
                }
            })

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

                                context.report(ISSUE_PATTERN, parrentNode, context.getNameLocation(parrentNode),"Exception not checked: $clazzName")
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