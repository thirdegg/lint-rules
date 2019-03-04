package com.thirdegg.lintrules.android

import com.android.tools.lint.checks.infrastructure.TestFiles

object CheckedExceptionsCoroutinesClasses {

    val KotlinCallClass = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class Call {
                fun enqueue(callback:Callback) {
                    callback.onResponse(Response())
                }

                fun await():String = suspendCoroutine { cont ->
                    enqueue(object: Callback {

                        override fun onResponse(response: Response) {
                            if (response.isSuccessful) {
                                cont.resume(response.body())
                            } else {
                                when (response.code()) {
                                    401-> {
                                        cont.resumeWithException(
                                            NotAuthorizedException("NotAuthorizedException")
                                        )
                                    }
                                    403-> {
                                        cont.resumeWithException(
                                            ForbiddenException("ForbiddenException")
                                        )
                                    }
                                    404-> {
                                        cont.resumeWithException(
                                            NotFoundException("NotFoundException")
                                        )
                                    }
                                    500-> {
                                        cont.resumeWithException(
                                            InternalServerErrorException("InternalServerErrorException")
                                        )
                                    }
                                    502-> {
                                        cont.resumeWithException(
                                            BadRequestException("BadRequestException")
                                        )
                                    }
                                    else->{
                                        cont.resumeWithException(
                                            Exception()
                                        )
                                    }
                                }
                            }
                        }
                    })
                }

            }

    """).indented()

    val KotlinCallbackInterface = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            interface Callback {
                fun onResponse(response: Response)
            }

    """).indented()

    val KotlinResponseClass = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class Response {
                fun body() = "{'result':'success'}"
                fun code() = 200
                val isSuccessful = true
            }

    """).indented()

    val KotlinErrorsClass = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android


            class NotAuthorizedException(error: String):Exception(error)
            class ForbiddenException(error: String):Exception(error)
            class NotFoundException(error: String):Exception(error)
            class InternalServerErrorException(error: String):Exception(error)
            class BadRequestException(error: String):Exception(error)


    """).indented()

    val KotlinCheckClass = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class Main {

                fun check() {
                    try {
                        Call().await()
                    } catch (e:Exception) {
                        e.printStackTrace()
                    }
                }

            }


    """).indented()

    val KotlinCoroutineMock = TestFiles.kotlin("""

            package com.thirdegg.lintrules.android

            class Continuation {

                lateinit var result:String

                fun resume(result:String) {
                    this.result = result
                }

                fun resumeWithException(e:Exception) {

                }
            }


            suspend inline fun suspendCoroutine(crossinline block: (Continuation) -> Unit): String {
                val cont = Continuation()
                block(cont)
                return cont.result
            }



    """).indented()

}