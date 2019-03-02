package com.thirdegg.lintrules.android

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.*

class CheckedExceptionsRegistry: IssueRegistry() {

    override val issues: List<Issue> = listOf(ISSUE_PATTERN)

}