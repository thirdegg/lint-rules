package com.thirdegg.lintrules.android.testcases

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.thirdegg.lintrules.android.ISSUE_PATTERN
import com.thirdegg.lintrules.android.Utils
import org.junit.Test


class ThrowsInThrowsSecondTestCase {

    val ThrowsInThrowsClassKotlin = TestFiles.kotlin("""
        
            package com.thirdegg.lintrules.android

            import java.io.IOException

            class CheckLint {
                
                @Throws(IOException::class)
                fun openFile() {
                    fooBar()
                    throw IOException()
                }
            
                @Throws(IOException::class)
                fun doesIO() {
                    openFile()  // <-- I get a warning here
                }
                
                fun fooBar() {
            
                }
            
                fun test() {
                    doesIO()
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
                src/com/thirdegg/lintrules/android/CheckLint.kt:24: Warning: Unhandled exception: java.io.IOException [CheckedExceptions]
                        doesIO()
                        ~~~~~~
                0 errors, 1 warnings
            """.trimIndent())

    }

}