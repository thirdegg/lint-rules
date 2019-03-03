package com.thirdegg.lintrules.android

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.*

class LintRulesRegistry: IssueRegistry() {

    override val issues: List<Issue> = listOf(ISSUE_PATTERN)

}