package com.personal.moneytracker.sync

import android.content.Context
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleIdentityAuthorizationGateway @Inject constructor(@ApplicationContext private val context: Context) : GoogleAuthorizationGateway {
    override suspend fun accessTokenOrNull(): AccessTokenResult = try {
        val result = Identity.getAuthorizationClient(context).authorize(request()).await()
        if (result.hasResolution()) AccessTokenResult.AuthorizationRequired
        else result.accessToken?.let { AccessTokenResult.Granted(it) } ?: AccessTokenResult.AuthorizationRequired
    } catch (_: Exception) { AccessTokenResult.AuthorizationRequired }

    companion object {
        const val SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets"
        fun request(): AuthorizationRequest = AuthorizationRequest.builder().setRequestedScopes(listOf(Scope(SHEETS_SCOPE))).build()
    }
}
