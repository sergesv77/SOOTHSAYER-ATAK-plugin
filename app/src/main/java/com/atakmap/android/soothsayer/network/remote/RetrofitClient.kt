package com.atakmap.android.soothsayer.network.remote

import com.atakmap.android.soothsayer.network.ApiService
import com.atakmap.android.soothsayer.util.Constant
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.security.cert.CertificateException


object RetrofitClient {
    private const val mContentType = "Content-type"
    private const val mContentTypeJson = "application/json"
    private const val mAuthorizationKey = "key"

    const val DEFAULT_URL = "https://api.cloudrf.com"
    const val DEFAULT_APIKEY = "49553-866c50dd76f5712328c6c63a712f306ceed2b5ab" // ATAK DEMO ACCOUNT

    val BASE_URL = Constant.sServerUrl

    private val OK_HTTP_CLIENT by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor())
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
    }

    private val RETROFIT by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
//            .client(OK_HTTP_CLIENT.build())
            .client(getUnsafeOkHttpClient().build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService? by lazy { RETROFIT.create(ApiService::class.java) }

    private class AuthorizationInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val authRequest = originalRequest.newBuilder()
                .header(mContentType, mContentTypeJson)
                .header(
                    mAuthorizationKey, Constant.sAccessToken
                ) // This should be from user input.
                .build()
            return chain.proceed(authRequest)
        }
    }


    fun getUnsafeOkHttpClient(): OkHttpClient.Builder {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}

                @Throws(CertificateException::class)
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}

                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                    return arrayOf()
                }
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("TLSv1.2")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
//                .addInterceptor(LOGGING_INTERCEPTOR)
                .addInterceptor(AuthorizationInterceptor())
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }

            return builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}