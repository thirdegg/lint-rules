package com.thirdegg.lintrules.android.testcases

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.thirdegg.lintrules.android.ISSUE_PATTERN
import com.thirdegg.lintrules.android.Utils
import org.junit.Test

class ThrowsTestCase {

    val ThrowsInterfaceKotlin = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class ThrowsInterfaceKotlin {

                fun tryOne() {
                    throw ThrowsException()
                }

                fun tryTwo() {
                    throw ThrowsException()
                    throw ThrowsTwoException()
                }
            }

        """).indented()

    val ThrowsExceptionsKotlin = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            import java.lang.Exception

            class ThrowsException:Exception("ThrowsException")
            class ThrowsTwoException:Exception("ThrowsException")
            class ThrowsThreeException:ThrowsTwoException()

        """).indented()

    val ThrowsClassJava = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class ThrowsClassJava {

                @Throws(ThrowsException::class)
                fun tryTwo() {
                    ThrowsInterfaceKotlin().tryOne()
                }

                @Throws(ThrowsException::class,ThrowsTwoException::class)
                fun tryThree() {
                    ThrowsInterfaceKotlin().tryTwo()
                }

                @Throws(ThrowsException::class,ThrowsTwoException::class,ThrowsThreeException::class)
                fun test() {
                    tryTwo()
                    tryThree()
                }

                fun test2() {
                    try {
                        test()
                    } catch(e:ThrowsException) {

                    }
                }

            }


        """).indented()


    @Test
    fun check_kotlin_throws() {

        TestLintTask.lint()
            .sdkHome(Utils.getSdk())
            .issues(ISSUE_PATTERN)
            .files(
                ThrowsClassJava,
                ThrowsExceptionsKotlin,
                ThrowsInterfaceKotlin
            ).run()
            .expect("""
                src/com/thirdegg/lintrules/android/ThrowsClassJava.kt:24: Warning: Unhandled exception: com.thirdegg.lintrules.android.ThrowsThreeException [CheckedExceptions]
                            test()
                            ~~~~
                src/com/thirdegg/lintrules/android/ThrowsClassJava.kt:24: Warning: Unhandled exception: com.thirdegg.lintrules.android.ThrowsTwoException [CheckedExceptions]
                            test()
                            ~~~~
                0 errors, 2 warnings
            """.trimIndent())

    }

}