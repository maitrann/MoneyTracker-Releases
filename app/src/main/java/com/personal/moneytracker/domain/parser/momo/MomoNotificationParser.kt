package com.personal.moneytracker.domain.parser.momo

import com.personal.moneytracker.domain.parser.SourceApp
import com.personal.moneytracker.domain.parser.SourceParserSkeleton
import javax.inject.Inject

class MomoNotificationParser @Inject constructor() : SourceParserSkeleton("momo", SourceApp.MOMO)
