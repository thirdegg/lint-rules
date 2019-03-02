package com.thirdegg.lintrules.android

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import org.junit.Test

import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import java.io.File

class CheckedExceptionsUnitTest {
    @Test
    fun check_kotlin() {

        val testFileKotlin = kotlin("""

            package com.i90g.checkexceptlint

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
                }

            }

            class TextExceptionKotlin:Exception("TextException")

        """).indented()

        val testFileJava = java("""

            package com.i90g.checkexceptlint;

            public class TestClassJava {

                private void test() throws TextExceptionJava {
                    if (true) {
                        throw new TextExceptionJava();
                    }
                }

                public void tryCatch() {
                    try {
                        test();
                    } catch (TextExceptionJava e) {
                        e.printStackTrace();
                    }
                }

            }

            class TextExceptionJava extends Exception {
                TextExceptionJava() {
                    super("TextException");
                }
            }

        """).indented()

        val sdkPath = if (System.getProperty("os.name").startsWith("Windows")) {
            File(System.getenv("LOCALAPPDATA")+"\\Android\\Sdk")
        } else {
            File(System.getProperty("user.home")+"/Android/Sdk/")
        }

        lint()
            .sdkHome(sdkPath)
            .issues(ISSUE_PATTERN)
            .files(testFileKotlin)
            .run()
            .expect("")

    }

}