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
            val method = parrentNode.resolve()?:return
            val uMethod = context.uastContext.getMethod(method)

            val haveTryCatch = ArrayList<String>()

            val tryException = findParrentByUast(parrentNode,UTryExpression::class.java)
            if (tryException!=null) {
                for (catchCause in tryException.catchClauses) {
                    haveTryCatch.add(findExceptionClassName(catchCause))
                }
            }

            uMethod.accept(object:AbstractUastVisitor() {

                override fun visitThrowExpression(node: UThrowExpression): Boolean {

                    node.accept(object:AbstractUastVisitor() {
                        override fun visitCallExpression(node: UCallExpression): Boolean {
                            if (node is KotlinUFunctionCallExpression) {
                                //TODO kotlin.Exception() not catch
                                val clazz = node.resolve()
                                val clazzName = clazz?.containingClass?.qualifiedName
                                if (clazzName==null || haveTryCatch.contains(clazzName)) return super.visitCallExpression(node)
                                context.report(ISSUE_PATTERN, parrentNode, context.getNameLocation(parrentNode), clazzName+" "+haveTryCatch)
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