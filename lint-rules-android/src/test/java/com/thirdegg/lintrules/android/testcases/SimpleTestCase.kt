package com.thirdegg.lintrules.android.testcases

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.thirdegg.lintrules.android.ISSUE_PATTERN
import com.thirdegg.lintrules.android.Utils
import org.junit.Test

class SimpleTestCase {

    private val TestClassKotlin = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class TestClassKotlin {

                fun test() {
                    if (false) {
                        throw TextExceptionKotlin()
                    } else {
                        throw Exception()
                    }
                }

                fun tryCatch() {
                    try {
                        test()
                    } catch (e:TextExceptionKotlin) {
                        e.printStackTrace()
                    }
                    test()
                }

            }

            class TextExceptionKotlin:Exception("TextException")

        """).indented()

    @Test
    fun check_kotlin() {

        TestLintTask.lint()
            .sdkHome(Utils.getSdk())
            .issues(ISSUE_PATTERN)
            .files(TestClassKotlin)
            .run()
            .expect("""
                src/com/thirdegg/lintrules/android/TestClassKotlin.kt:20: Warning: Unhandled exception: com.thirdegg.lintrules.android.TextExceptionKotlin [CheckedExceptions]
                        test()
                        ~~~~
                0 errors, 1 warnings
            """.trimIndent())

    }

}