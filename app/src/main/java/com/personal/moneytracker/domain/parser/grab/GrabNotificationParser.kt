package com.personal.moneytracker.domain.parser.grab

import com.personal.moneytracker.domain.parser.SourceApp
import com.personal.moneytracker.domain.parser.SourceParserSkeleton
import javax.inject.Inject

class GrabNotificationParser @Inject constructor() : SourceParserSkeleton("grab", SourceApp.GRAB)
