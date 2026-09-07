package com.personal.moneytracker.domain.parser.timo

import com.personal.moneytracker.domain.parser.SourceApp
import com.personal.moneytracker.domain.parser.SourceParserSkeleton
import javax.inject.Inject

class TimoNotificationParser @Inject constructor() : SourceParserSkeleton("timo", SourceApp.TIMO)
