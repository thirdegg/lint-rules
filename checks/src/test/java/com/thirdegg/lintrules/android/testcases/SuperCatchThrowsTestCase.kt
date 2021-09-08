package com.thirdegg.lintrules.android.testcases

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.thirdegg.lintrules.android.CheckedExceptionsDetector.Companion.ISSUE_PATTERN
import com.thirdegg.lintrules.android.Utils
import org.junit.Test

class SuperCatchThrowsTestCase {

    val SuperCatchThrowsClassKotlin = TestFiles.kotlin("""
        
            package com.thirdegg.lintrules.android

            import java.io.IOException
            import java.lang.Exception

            class CheckLint {
                
                @Throws(SuperPuperException::class)
                fun openFile() {
                    throw SuperPuperException()
                }
            
                @Throws(SuperException::class)
                fun doesIO() {
                    openFile()
                }
                
                @Throws(SuperPuperException::class)
                fun doesIOTwo() {
                    doesIO()
                }

            
                fun test() {
                    try {
                        doesIO()
                    } catch(e: IOException) {
                        e.printStackTrace()
                    }
                    try {
                        doesIO()
                    } catch(e: SuperPuperException) {
                        e.printStackTrace()
                    }
                    doesIO()
                }
                
                class SuperException:IOException()
                
                class SuperPuperException:SuperException()
            
            }
        
    """.trimIndent())

    @Test
    fun check_kotlin() {

        TestLintTask.lint()
            .sdkHome(Utils.getSdk())
            .issues(ISSUE_PATTERN)
            .files(SuperCatchThrowsClassKotlin)
            .run()
            .expect("""
                src/com/thirdegg/lintrules/android/CheckLint.kt:21: Warning: Unhandled exception: com.thirdegg.lintrules.android.CheckLint.SuperException [CheckedExceptions]
                        doesIO()
                        ~~~~~~
                src/com/thirdegg/lintrules/android/CheckLint.kt:32: Warning: Unhandled exception: com.thirdegg.lintrules.android.CheckLint.SuperException [CheckedExceptions]
                            doesIO()
                            ~~~~~~
                src/com/thirdegg/lintrules/android/CheckLint.kt:36: Warning: Unhandled exception: com.thirdegg.lintrules.android.CheckLint.SuperException [CheckedExceptions]
                        doesIO()
                        ~~~~~~
                0 errors, 3 warnings
            """.trimIndent())

    }

}