package com.ghosttrack.app.data.remote

import com.ghosttrack.app.data.local.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class DynamicHostInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val customUrl = runBlocking { tokenStore.getServerUrl() }
        if (!customUrl.isNullOrBlank()) {
            val parsedUrl = customUrl.trim().removeSuffix("/").toHttpUrlOrNull()
            if (parsedUrl != null) {
                val newUrl = request.url.newBuilder()
                    .scheme(parsedUrl.scheme)
                    .host(parsedUrl.host)
                    .port(parsedUrl.port)
                    .build()
                request = request.newBuilder().url(newUrl).build()
            }
        }
        return chain.proceed(request)
    }
}
