package com.personal.moneytracker.domain.model

enum class RuleField { MERCHANT, SERVICE, DESCRIPTION, SOURCE_APP, PAYMENT_CHANNEL }
enum class RuleOperator { EQUALS, CONTAINS, STARTS_WITH, REGEX }
enum class RuleSource { SYSTEM, USER }
