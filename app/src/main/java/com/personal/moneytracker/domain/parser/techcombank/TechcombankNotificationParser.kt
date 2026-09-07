package com.personal.moneytracker.domain.parser.techcombank

import com.personal.moneytracker.domain.parser.SourceApp
import com.personal.moneytracker.domain.parser.SourceParserSkeleton
import javax.inject.Inject

class TechcombankNotificationParser @Inject constructor() : SourceParserSkeleton("techcombank", SourceApp.TECHCOMBANK)
