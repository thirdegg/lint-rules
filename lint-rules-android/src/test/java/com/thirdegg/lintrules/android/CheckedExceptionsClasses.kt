package com.thirdegg.lintrules.android

import com.android.tools.lint.checks.infrastructure.TestFiles

object CheckedExceptionsClasses {

    val TestClassKotlin = TestFiles.kotlin("""

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
                }

            }

            class TextExceptionKotlin:Exception("TextException")

        """).indented()

    val TestClassJava = TestFiles.java("""

            package com.thirdegg.lintrules.android;

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

}