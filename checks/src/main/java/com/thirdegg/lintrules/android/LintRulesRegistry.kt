package com.thirdegg.lintrules.android

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.*
import com.thirdegg.lintrules.android.CheckedExceptionsDetector.Companion.ISSUE_PATTERN

class LintRulesRegistry: IssueRegistry() {

    override val issues: List<Issue> = listOf(ISSUE_PATTERN)

    override val api: Int = CURRENT_API

}