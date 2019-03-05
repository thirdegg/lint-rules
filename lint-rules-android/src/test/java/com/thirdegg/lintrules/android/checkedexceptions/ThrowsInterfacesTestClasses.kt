package com.thirdegg.lintrules.android.checkedexceptions

import com.android.tools.lint.checks.infrastructure.TestFiles

object ThrowsInterfacesTestClasses {

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

}