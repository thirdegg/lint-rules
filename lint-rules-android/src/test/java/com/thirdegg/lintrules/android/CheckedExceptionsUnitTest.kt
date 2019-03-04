package com.thirdegg.lintrules.android

import org.junit.Test

import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.thirdegg.lintrules.android.CheckedExceptionsClasses.TestClassKotlin

import com.thirdegg.lintrules.android.CheckedExceptionsCoroutinesClasses.KotlinCallClass
import com.thirdegg.lintrules.android.CheckedExceptionsCoroutinesClasses.KotlinCallbackInterface
import com.thirdegg.lintrules.android.CheckedExceptionsCoroutinesClasses.KotlinCheckClass
import com.thirdegg.lintrules.android.CheckedExceptionsCoroutinesClasses.KotlinCoroutineMock
import com.thirdegg.lintrules.android.CheckedExceptionsCoroutinesClasses.KotlinErrorsClass
import com.thirdegg.lintrules.android.CheckedExceptionsCoroutinesClasses.KotlinResponseClass

import java.io.File
import java.net.URI

class CheckedExceptionsUnitTest {

    fun getSdk():File {
        //  TestUtils.getSdk() not working
        return if (System.getProperty("os.name").startsWith("Windows")) {
            File(System.getenv("LOCALAPPDATA")+"\\Android\\Sdk")
        } else {
            File(System.getProperty("user.home")+"/Android/Sdk/")
        }
    }


    @Test
    fun check_kotlin() {

        lint()
            .sdkHome(getSdk())
            .issues(ISSUE_PATTERN)
            .files(TestClassKotlin)
            .run()
            .expect("")

    }

    @Test
    fun check_kotlin_coroutines() {

        lint()
            .sdkHome(getSdk())
            .issues(ISSUE_PATTERN)
            .files(
                KotlinCallClass,
                KotlinCallbackInterface,
                KotlinErrorsClass,
                KotlinResponseClass,
                KotlinCheckClass,
                KotlinCoroutineMock
            ).run()
            .expect("")

    }
}