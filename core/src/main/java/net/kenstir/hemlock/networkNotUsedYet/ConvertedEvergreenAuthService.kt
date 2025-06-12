package net.kenstir.hemlock.networkNotUsedYet

import android.text.TextUtils
import net.kenstir.hemlock.data.AuthenticationException
import org.evergreen_ils.Api
import org.evergreen_ils.android.Analytics.log
import org.evergreen_ils.android.Log
import org.opensrf.Method
import org.opensrf.net.http.GatewayRequest
import org.opensrf.net.http.HttpConnection
import org.opensrf.net.http.HttpRequest
import java.net.MalformedURLException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object ConvertedEvergreenAuthService {
    private val TAG: String = ConvertedEvergreenAuthService::class.java.simpleName

    private fun md5(s: String): String {
        try {
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuilder()
            for (i in messageDigest.indices) {
                val hex = Integer.toHexString(0xFF and messageDigest[i].toInt())
                if (hex.length == 1) {
                    // could use a for loop, but we're only dealing with a
                    // single byte
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            Log.d(TAG, "no MD5", e)
        }

        return ""
    }

    @Throws(Exception::class)
    fun doRequest(
        conn: HttpConnection?,
        service: String?,
        methodName: String,
        params: Array<Any>
    ): Any? {
        val method = Method(methodName)

        Log.d(TAG, "doRequest> Method :$methodName:")
        for (i in params.indices) {
            method.addParam(params[i])
            Log.d(TAG, "doRequest> Param " + i + ": " + params[i])
        }

        // sync request
        val req: HttpRequest = GatewayRequest(conn, service, method).send()
        val resp: Any

        while ((req.recv().also { resp = it }) != null) {
            Log.d(
                TAG,
                "doRequest> Sync Response: $resp"
            )
            val response = resp
            return response
        }

        if (req.failed()) {
            throw req.failure
        }
        return null
    }

    @Throws(AuthenticationException::class)
    fun signIn(library_url: String, username: String, password: String): String? {
        log(TAG, "signIn: library_url=$library_url")

        val conn: HttpConnection
        try {
            conn = HttpConnection("$library_url/osrf-gateway-v1")
        } catch (e: MalformedURLException) {
            throw AuthenticationException(e)
        }

        // step 1: get seed
        var resp: Any? = null
        try {
            resp = doRequest(conn, Api.AUTH, Api.AUTH_INIT, arrayOf(username))
        } catch (e: Exception) {
            throw AuthenticationException(e)
        }
        if (resp == null) {
            throw AuthenticationException(
                """
                    Can't reach server at $library_url
                    
                    The server may be offline.
                    """.trimIndent()
            )
        }
        val seed = resp.toString()

        // step 2: complete auth with seed + password
        val param = HashMap<String, String>()
        param["type"] = "persist" // {opac|persist}, controls authtoken timeout
        param["username"] = username
        param["password"] = md5(seed + md5(password))
        try {
            resp = doRequest(conn, Api.AUTH, Api.AUTH_COMPLETE, arrayOf(param))
        } catch (e: Exception) {
            throw AuthenticationException(e)
        }

        if (resp == null) throw AuthenticationException("Unable to complete login")


        // parse response, throw if error
        // {"payload":[{"payload":{"authtoken":"***","authtime":1209600},"ilsevent":0,"textcode":"SUCCESS","desc":"Success"}],"status":200}
        val textcode = (resp as Map<String?, String?>)["textcode"]
        Log.d(TAG, "textcode: $textcode")
        if (textcode == "SUCCESS") {
            val payload: Any? = resp["payload"]
            Log.d(TAG, "payload: $payload")
            val authtoken = (payload as Map<String?, String?>)["authtoken"]
            Log.d(TAG, "authtoken: $authtoken")
            val authtime = (payload as Map<String?, Int?>)["authtime"]
            Log.d(TAG, "authtime: $authtime")
            return authtoken
        } else if (!TextUtils.isEmpty(textcode)) {
            val desc = resp["desc"]
            Log.d(TAG, "desc: $desc")
            if (!TextUtils.isEmpty(desc)) {
                throw AuthenticationException(desc!!)
            }
        }
        throw AuthenticationException("Login failed")
    }
}
