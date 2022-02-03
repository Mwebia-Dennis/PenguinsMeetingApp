package com.penguinstech.penguinsmeetingapp

import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap

class ZoomUser(private val zoomUserListener: ZoomUserListener) {

    fun getUserId(token:ZoomAccessToken) {

        GlobalScope.launch {

            val mURL = URL(ZoomConfigs.ZOOM_USER_URL)

            try {
                with(mURL.openConnection() as HttpURLConnection) {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer  ${token.accessToken}")
                    setRequestProperty("Content-Type", " application/json")
                    doOutput = false

                    try{
                        BufferedReader(InputStreamReader(inputStream)).use {
                            val response = StringBuffer()

                            var inputLine = it.readLine()
                            while (inputLine != null) {
                                response.append(inputLine)
                                inputLine = it.readLine()
                            }
                            println("Response : $response")
                            val resp: HashMap<String, Any> = Gson().fromJson(
                                response.toString(),
                                HashMap::class.java
                            ) as HashMap<String, Any>

                            val user_Id = resp["id"].toString()
                            zoomUserListener.onSuccess(user_Id)
                        }
                    }catch (ex:Exception) {

                        println("http userId inputStream Error: ${ex.message}")
                    }
                    try{
                        BufferedReader(InputStreamReader(errorStream)).use {
                            val response = StringBuffer()

                            var inputLine = it.readLine()
                            while (inputLine != null) {
                                response.append(inputLine)
                                inputLine = it.readLine()
                            }
                            println("error : $response")
                            zoomUserListener.onError(response.toString())
                        }
                    }catch (ex:Exception) {

                        println("http userId inputStream Error: ${ex.message}")
                    }
                }

            }catch (ex: Exception) {
                println("http Error: ${ex.message}")
            }

        }
    }
}