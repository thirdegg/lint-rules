package com.thirdegg.lintrules.android

import org.junit.Test

import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.thirdegg.lintrules.android.checkedexceptions.SimpleTestClasses.TestClassKotlin

import com.thirdegg.lintrules.android.checkedexceptions.CoroutinesTestClasses.KotlinCallClass
import com.thirdegg.lintrules.android.checkedexceptions.CoroutinesTestClasses.KotlinCallbackInterface
import com.thirdegg.lintrules.android.checkedexceptions.CoroutinesTestClasses.KotlinCheckClass
import com.thirdegg.lintrules.android.checkedexceptions.CoroutinesTestClasses.KotlinCoroutineMock
import com.thirdegg.lintrules.android.checkedexceptions.CoroutinesTestClasses.KotlinErrorsClass
import com.thirdegg.lintrules.android.checkedexceptions.CoroutinesTestClasses.KotlinResponseClass


import com.thirdegg.lintrules.android.checkedexceptions.ThrowsTestClasses.ThrowsClassJava
import com.thirdegg.lintrules.android.checkedexceptions.ThrowsTestClasses.ThrowsExceptionsKotlin
import com.thirdegg.lintrules.android.checkedexceptions.ThrowsTestClasses.ThrowsInterfaceKotlin


import java.io.File

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

    @Test
    fun check_kotlin_throws() {

        lint()
            .sdkHome(getSdk())
            .issues(ISSUE_PATTERN)
            .files(
                ThrowsClassJava,
                ThrowsExceptionsKotlin,
                ThrowsInterfaceKotlin
            ).run()
            .expect("")

    }
}