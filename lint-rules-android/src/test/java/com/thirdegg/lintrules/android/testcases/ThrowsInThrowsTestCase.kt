package com.thirdegg.lintrules.android.testcases

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.thirdegg.lintrules.android.ISSUE_PATTERN
import com.thirdegg.lintrules.android.Utils
import org.junit.Test


class ThrowsInThrowsTestCase {

    val ThrowsInThrowsClassKotlin = TestFiles.kotlin("""
        
            package com.thirdegg.lintrules.android

            import java.io.IOException
            import java.lang.IllegalStateException

            class ThrowsInThrowsClassKotlin {
                
                fun main() {
                    doesIO()
                }
            
                @Throws(IOException::class, IllegalStateException::class)
                fun doesIO() {
                    openFile()
                }
            
                @Throws(IllegalStateException::class)
                fun openFile() {
                    fooBar()
                }
            
            }
        
    """.trimIndent())

    @Test
    fun check_kotlin() {

        TestLintTask.lint()
            .sdkHome(Utils.getSdk())
            .issues(ISSUE_PATTERN)
            .files(ThrowsInThrowsClassKotlin)
            .run()
            .expect("""
                src/com/thirdegg/lintrules/android/ThrowsInThrowsClassKotlin.kt:10: Warning: Unhandled exception: java.io.IOException [CheckedExceptions]
                        doesIO()
                        ~~~~~~
                src/com/thirdegg/lintrules/android/ThrowsInThrowsClassKotlin.kt:10: Warning: Unhandled exception: java.lang.IllegalStateException [CheckedExceptions]
                        doesIO()
                        ~~~~~~
                0 errors, 2 warnings
            """.trimIndent())

    }

}