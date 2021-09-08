package com.thirdegg.lintrules.android.testcases

import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.thirdegg.lintrules.android.CheckedExceptionsDetector.Companion.ISSUE_PATTERN
import com.thirdegg.lintrules.android.Utils
import org.junit.Test

class WithJavaTestCase {

    private val TestClassKotlin = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class TestClassKotlin {

                fun tryCatch() {
                    val testClassJava = TestClassJava()
                    try {
                        testClassJava.test()
                    } catch (e:TextExceptionJava) {
                        e.printStackTrace()
                    }
                    testClassJava.test()
                }

            }

        """).indented()

    private val TestClassJava = TestFiles.java("""

            package com.thirdegg.lintrules.android;

            public class TestClassJava {

                private void test() throws TextExceptionJava {
                    if (true) {
                        throw new TextExceptionJava();
                    }
                }
            }

        """).indented()

    private val TextExceptionJava = TestFiles.java("""

            package com.thirdegg.lintrules.android;

            public class TextExceptionJava extends Exception {
                TextExceptionJava() {
                    super("TextException");
                }
            }

    """).indented()



    @Test
    fun check_kotlin() {

        TestLintTask.lint()
            .sdkHome(Utils.getSdk())
            .issues(ISSUE_PATTERN)
            .files(TestClassKotlin, TestClassJava, TextExceptionJava)
            .run()
            .expect("""
                src/com/thirdegg/lintrules/android/TestClassKotlin.kt:13: Warning: Unhandled exception: com.thirdegg.lintrules.android.TextExceptionJava [CheckedExceptions]
                        testClassJava.test()
                                      ~~~~
                0 errors, 1 warnings
            """.trimIndent())

    }

}