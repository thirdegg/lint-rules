package com.thirdegg.lintrules.android

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Severity.WARNING
import com.intellij.psi.PsiElement
import org.jetbrains.uast.*
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

    fun findChildsInPsi(item: PsiElement): ArrayList<PsiElement> {
        val list = ArrayList<PsiElement>()
        for (child in item.children) {
            list.add(child)
            if (child.children.isNotEmpty()) {
                list.addAll(findChildsInPsi(child))
            }
        }
        return list
    }


    fun findExceptionClassName(catchClause: UCatchClause): String {
        return catchClause.parameters[0].psi.type.canonicalText
    }

    fun findNamedExpressionsInAnnotation(uAnnotation: UAnnotation): HashSet<String?> {
        val namedExpressions = HashSet<String?>()
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


    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        init {
//            println(context.uastFile?.asRecursiveLogString())
        }

        override fun visitCallExpression(node: UCallExpression) {

            val call = node
            val method = call.resolve() ?: return
            val uMethod = context.uastContext.getMethod(method)

            val throwsExceptions = HashSet<String>()

            // Find @Throws in annotation expression
            for (annotation in uMethod.annotations) {
                if (annotation.qualifiedName != "kotlin.jvm.Throws") continue
                for (throwsException in findNamedExpressionsInAnnotation(annotation)) {
                    throwsException ?: continue
                    if (throwsExceptions.contains(throwsException)) continue
                    throwsExceptions.add(throwsException)
                }
            }

            // Find throws in method
            for (child in uMethod.uastBody.getQualifiedChain()) {
                if (child is UBlockExpression) {
                    child.sourcePsi ?: continue
                    for (psi in findChildsInPsi(child.sourcePsi!!)) {
                        val uElement = psi.toUElement()
                        if (uElement == null || uElement !is UThrowExpression) continue
                        uElement.thrownExpression as UCallExpression
                        val clazz = (uElement.thrownExpression as UCallExpression).resolve()
                        val clazzName = clazz?.containingClass?.qualifiedName
                        clazzName ?: continue
                        throwsExceptions.add(clazzName)
                    }
                }
            }


            val ignoreExceptions = HashSet<String>()
            for (element in call.withContainingElements) {
                if (element !is UMethod) continue
                for (child in element.annotations) {
                    for (classInAnnotation in findNamedExpressionsInAnnotation(child)) {
                        classInAnnotation ?: continue
                        ignoreExceptions.add(classInAnnotation)
                    }
                }
                break
            }

            for (element in call.withContainingElements) {
                if (element !is UTryExpression) continue
                for (catchCause in element.catchClauses) {
                    val clazzName = findExceptionClassName(catchCause)
                    ignoreExceptions.add(clazzName)
                }
                break
            }

            for (exceptions in throwsExceptions) {
                if (!ignoreExceptions.contains(exceptions)) {
                    context.report(
                        ISSUE_PATTERN, call, context.getNameLocation(call),
                        "Unhandled exception: $exceptions"
                    )
                }
            }

        }
    }
}