package com.thirdegg.lintrules.android.testcases

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.thirdegg.lintrules.android.CheckedExceptionsDetector.Companion.ISSUE_PATTERN
import com.thirdegg.lintrules.android.Utils
import org.junit.Test

class StdJavaTestCase {

    private val TestClassKotlin = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            import java.util.Scanner
            import java.io.File
            import java.io.FileReader
            
            class TestClassKotlin {
                fun test() {
                    val file = File("file")
                    val reader = FileReader(file)
                    Scanner(File("test"))
                }
            }


        """).indented()



    @Test
    fun check_kotlin() {

        TestLintTask.lint()
            .sdkHome(Utils.getSdk())
            .issues(ISSUE_PATTERN)
            .files(TestClassKotlin)
            .run()
            .expect("""
                src/com/thirdegg/lintrules/android/TestClassKotlin.kt:11: Warning: Unhandled exception: java.io.FileNotFoundException [CheckedExceptions]
                        val reader = FileReader(file)
                                     ~~~~~~~~~~
                src/com/thirdegg/lintrules/android/TestClassKotlin.kt:12: Warning: Unhandled exception: java.io.FileNotFoundException [CheckedExceptions]
                        Scanner(File("test"))
                        ~~~~~~~
                0 errors, 2 warnings
            """.trimIndent())

    }

}