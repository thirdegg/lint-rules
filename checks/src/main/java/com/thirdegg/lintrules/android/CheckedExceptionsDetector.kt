package com.thirdegg.lintrules.android

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Severity.WARNING
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import org.jetbrains.uast.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

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

    fun findClassParents(psiClass: PsiType):HashSet<String> {
        val classes = HashSet<String>()
        classes.add(psiClass.canonicalText)
        psiClass.superTypes.forEach {
            classes.addAll(findClassParents(it))
        }
        return classes
    }

    fun findNamedExpressionsInAnnotation(uAnnotation: UAnnotation): HashSet<PsiType> {
        val namedExpressions = HashSet<PsiType>()
        for (uNamedExpression in uAnnotation.attributeValues) {
            if (uNamedExpression.expression is UClassLiteralExpression) {
                // If UClassLiteralExpression.type.canonicalText is null then maybe no import of Exception
                (uNamedExpression.expression as UClassLiteralExpression).type?.let {
                    namedExpressions.add(it)
                }
                continue
            }
            if (uNamedExpression.expression is UCallExpression) {
                for (argument in (uNamedExpression.expression as UCallExpression).valueArguments) {
                    if (argument is UClassLiteralExpression) {
                        argument.type?.let {
                            namedExpressions.add(it)
                        }
                        continue
                    }
                }
            }
        }
        return namedExpressions
    }

    override fun createUastHandler(context: JavaContext) = object : UElementHandler() {

        override fun visitCallExpression(node: UCallExpression) {

            val method = node.resolve() ?: return
            val uMethod = UastFacade.convertElement(method, null, UMethod::class.java) as UMethod

            var throwsExceptions = HashMap<String, HashSet<String>>()

            // Find @Throws in annotation expression
            for (annotation in uMethod.uAnnotations) {
                if (annotation.qualifiedName != "kotlin.jvm.Throws") continue
                for (throwsException in findNamedExpressionsInAnnotation(annotation)) {
                    throwsExceptions[throwsException.canonicalText] = findClassParents(throwsException)
                }
            }

            // Find throws in constructor
            val throwsList = method.throwsList
            throwsList.referencedTypes.forEach {
                throwsExceptions[it.canonicalText] = findClassParents(it)
            }

            // Find throws in method
            for (child in uMethod.uastBody.getQualifiedChain()) {
                if (child is UBlockExpression) {
                    child.sourcePsi ?: continue
                    for (psi in findChildsInPsi(child.sourcePsi!!)) {
                        val uElement = psi.toUElement()
                        if (uElement == null || uElement !is UThrowExpression) continue
                        val clazz = (uElement.thrownExpression as UCallExpression).resolve()
                        val mainClassName = clazz?.containingClass?.qualifiedName
                        mainClassName ?: continue
                        clazz.containingClass?.superTypes?.forEach {
                            throwsExceptions[mainClassName] = (findClassParents(it).apply { add(mainClassName) })
                        }

                    }
                }
            }


            // Remove catched
            for (element in node.withContainingElements) {
                if (element !is UMethod) continue
                for (child in element.uAnnotations) {
                    for (classInAnnotation in findNamedExpressionsInAnnotation(child)) {
                        throwsExceptions = throwsExceptions.filterTo(HashMap()) {
                            !it.value.contains(classInAnnotation.canonicalText)
                        }
                    }
                }
                break
            }

            for (element in node.withContainingElements) {
                if (element !is UTryExpression) continue
                for (catchCause in element.catchClauses) {
                    catchCause.types.forEach { catch ->
                        throwsExceptions = throwsExceptions.filterTo(HashMap()) {
                            !it.value.contains(catch.canonicalText)
                        }
                    }
                }
                break
            }

            for (exceptions in throwsExceptions) {
                context.report(
                    ISSUE_PATTERN, node, context.getNameLocation(node),
                    "Unhandled exception: ${exceptions.key}"
                )
            }

        }
    }

    companion object {
        val ISSUE_PATTERN = Issue.create(
            "CheckedExceptions",
            "Checked exceptions for kotlin",
            "Checked exceptions for kotlin",
            CORRECTNESS,
            7,
            WARNING,
            Implementation(CheckedExceptionsDetector::class.java, EnumSet.of(Scope.JAVA_FILE))
        )
    }
}