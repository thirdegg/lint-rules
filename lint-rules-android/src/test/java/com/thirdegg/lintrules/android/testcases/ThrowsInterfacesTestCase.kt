package com.thirdegg.lintrules.android.testcases

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.thirdegg.lintrules.android.ISSUE_PATTERN
import com.thirdegg.lintrules.android.Utils
import org.junit.Test

class ThrowsInterfacesTestCase {

    val ThrowsInterfaceKotlin = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            interface ThrowsInterfaceKotlin {

                @Throws(Exception::class,ThrowsException::class)
                fun tryOne()

                @Throws(ThrowsException::class)
                fun tryTwo()

            }

        """).indented()

    val ThrowsExceptionsKotlin = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class ThrowsException:Exception("ThrowsException")

        """).indented()

    val ThrowsClassJava = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class ThrowsClassJava(val throwsInterfaceKotlin:ThrowsInterfaceKotlin) {

                fun test() {

                    throwsInterfaceKotlin.tryOne()

                    throwsInterfaceKotlin.tryTwo()

                    try {
                        throwsInterfaceKotlin.tryOne()
                    } catch (e:ThrowsException) {
                        e.printStackTrace()
                    }

                }
            }


        """).indented()

    @Test
    fun check_kotlin() {

        TestLintTask.lint()
            .sdkHome(Utils.getSdk())
            .issues(ISSUE_PATTERN)
            .files(ThrowsInterfaceKotlin, ThrowsExceptionsKotlin, ThrowsClassJava)
            .run()
            .expect("""
                src/com/thirdegg/lintrules/android/ThrowsClassJava.kt:8: Warning: Unhandled exception: com.thirdegg.lintrules.android.ThrowsException [CheckedExceptions]
                        throwsInterfaceKotlin.tryOne()
                                              ~~~~~~
                src/com/thirdegg/lintrules/android/ThrowsClassJava.kt:8: Warning: Unhandled exception: java.lang.Exception [CheckedExceptions]
                        throwsInterfaceKotlin.tryOne()
                                              ~~~~~~
                src/com/thirdegg/lintrules/android/ThrowsClassJava.kt:10: Warning: Unhandled exception: com.thirdegg.lintrules.android.ThrowsException [CheckedExceptions]
                        throwsInterfaceKotlin.tryTwo()
                                              ~~~~~~
                src/com/thirdegg/lintrules/android/ThrowsClassJava.kt:13: Warning: Unhandled exception: java.lang.Exception [CheckedExceptions]
                            throwsInterfaceKotlin.tryOne()
                                                  ~~~~~~
                0 errors, 4 warnings
            """.trimIndent())

    }

}