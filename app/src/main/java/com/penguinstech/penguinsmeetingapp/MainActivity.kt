package com.penguinstech.penguinsmeetingapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {


    val ZAK_TOKEN = "zak_token"

    var recyclerView: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.mainRv)
        setUpAuth(intent)
        refreshAccessTokens(false)

        updateUi()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.scheduleMeeting) {
            //get zoomAccessToken from shared preferences else get from zoom
            val sharedPref: SharedPreferences = getSharedPreferences(
                Util.ZOOM_ACCESS_TOKEN,
                Context.MODE_PRIVATE
            )

            val zoomToken = sharedPref.getString(Util.ZOOM_ACCESS_TOKEN, "")

            if(zoomToken == ""){
                authorize()
            }else {

                val token: ZoomAccessToken = Gson().fromJson(
                    zoomToken,
                    ZoomAccessToken::class.java
                ) as ZoomAccessToken
                //check if access tokens are expired
                val expiresAt: Calendar = Calendar.getInstance()
                expiresAt.timeInMillis = token.expiresAt.toLong()
                expiresAt.add(Calendar.MINUTE, -30)//SUBTRACT A FEW MINUTES TO CATER FOR THE SERVER TO CLIENT NETWORK-LATENCY
                if(Calendar.getInstance() > expiresAt) {
                    //token is expired
                    refreshAccessTokens(true)
                }else {
                    //get zak and set up meeting
                    startMeeting(token)
                }

            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    //redirect to browser for zoom authentication
    private fun authorize() {
        val browserIntent = Intent(
            Intent.ACTION_VIEW, Uri.parse(
                "${ZoomConfigs.BASE_URL}/oauth/authorize?" +
                        "client_id=${ZoomConfigs.CLIENT_ID}&" +
                        "response_type=code&redirect_uri=${ZoomConfigs.APP_DYNAMIC_LINK}"
            )
        )
        startActivity(browserIntent)
    }
    private fun updateUi() {

        val list:MutableList<Meeting> = ArrayList()
        FirebaseDatabase.getInstance().getReference("meetings")
            .child("user_1").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {
                        list.clear()
                        for (snapshot2 in snapshot.children) {
                            val firebaseMeeting: Map<String, Any> = snapshot2.value as Map<String, Any>
                            val meeting = Meeting()
                            for ((key, value) in firebaseMeeting.entries) {
                                if (key == "topic")
                                    meeting.topic = value.toString()
                                if (key == "start_url")
                                    meeting.startUrl = value.toString()
                                if (key == "join_url")
                                    meeting.joinUrl = value.toString()
                                if (key == "id")
                                    meeting.meetingId = value.toString()
                            }
                            list.add(meeting)


                        }

                        recyclerView!!.layoutManager =
                            LinearLayoutManager(
                                this@MainActivity,
                                LinearLayoutManager.VERTICAL,
                                false
                            )
                        val adapter = MeetingsAdapter(this@MainActivity, list)
                        recyclerView!!.adapter = adapter
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })

    }

    //@method setUpAuth is used to retrieve the zoom oath authorization_code
    private fun setUpAuth(intent: Intent) {

        if(intent.data != null) {
            val code:String = intent.data!!.getQueryParameter("code").toString()
            val state:String = intent.data!!.getQueryParameter("state").toString()
            Log.i("intent", intent.toString())
//            val code = "YZU27g5T6g_Kg0qW6XaRaawAQnxR_hzIQ"
            Log.i("code", code)
            if(code != null) {

                GlobalScope.launch {
                    getAccessToken(code, "authorization_code", state)
                }
            }

        }

    }

    //get zak and start meeting
    private fun startMeeting(token: ZoomAccessToken) {
        // start meeting
        ScheduleMeetingDialog(token).show(supportFragmentManager, "MeetingDialogForm")
    }


    //get zoom access token
    private fun getAccessToken(code: String, grant_type: String, state: String) {


        val client:String = ZoomConfigs.CLIENT_ID + ":"+ ZoomConfigs.CLIENT_SECRET
        val base64Client = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(client.toByteArray(StandardCharsets.UTF_8))
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        var reqParam = URLEncoder.encode("code", "UTF-8") + "=" + URLEncoder.encode(code, "UTF-8")
        reqParam += "&" + URLEncoder.encode("grant_type", "UTF-8") + "=" + URLEncoder.encode(
            grant_type,
            "UTF-8"
        )
        reqParam += "&" + URLEncoder.encode("state", "UTF-8") + "=" + URLEncoder.encode(
            state,
            "UTF-8"
        )
        reqParam += "&" + URLEncoder.encode("redirect_uri", "UTF-8") + "=" + URLEncoder.encode(
            ZoomConfigs.APP_DYNAMIC_LINK,
            "UTF-8"
        )
        val mURL = URL("${ZoomConfigs.BASE_URL}/oauth/token")

        try {
            with(mURL.openConnection() as HttpURLConnection) {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Basic $base64Client")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                setRequestProperty(
                    "Content-Length",
                    reqParam.toByteArray(StandardCharsets.UTF_8).size.toString()
                )
                doOutput = true

                val wr = OutputStreamWriter(outputStream)
                wr.write(reqParam)

                wr.flush()

                println("URL : $url")
                println("Response Code : $responseCode")
                println("Response message : $responseMessage")

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
                    val token = ZoomAccessToken()
                    token.accessToken = resp["access_token"].toString()
                    token.refreshToken = resp["refresh_token"].toString()
                    token.tokenType = resp["token_type"].toString()
                    token.expiresIn = resp["expires_in"].toString()
                    token.scope = resp["scope"].toString()
                    val expiresAt = Calendar.getInstance()
                    expiresAt.add(Calendar.HOUR_OF_DAY, 1)//acce
                    token.expiresAt = expiresAt.timeInMillis.toString()
                    val tokenString: String = GsonBuilder().create().toJson(
                        token,
                        ZoomAccessToken::class.java
                    )
                    Log.i("token", tokenString)
                    val sharedPref:SharedPreferences = getSharedPreferences(
                        Util.ZOOM_ACCESS_TOKEN,
                        Context.MODE_PRIVATE
                    )
                    sharedPref.edit().putString(Util.ZOOM_ACCESS_TOKEN, tokenString).apply()
//                    getZakTokens(token, true)
                    startMeeting(token)
                }

            }

        }catch (ex: Exception) {
            println("http Error: ${ex.message}")
        }
    }

    //get access tokens via refresh tokens
    private fun refreshAccessTokens(startMeeting: Boolean) {

        GlobalScope.launch {
            val sharedPref:SharedPreferences = getSharedPreferences(
                Util.ZOOM_ACCESS_TOKEN,
                Context.MODE_PRIVATE
            )

            val zoomToken = sharedPref.getString(Util.ZOOM_ACCESS_TOKEN, "")
            if(zoomToken != ""){
                val token: ZoomAccessToken = Gson().fromJson(
                    zoomToken,
                    ZoomAccessToken::class.java
                ) as ZoomAccessToken


                val expiresAt:Calendar = Calendar.getInstance()
                expiresAt.timeInMillis = token.expiresAt.toLong()
                expiresAt.add(Calendar.MINUTE, -30)//SUBTRACT A FEW MINUTES TO CATER FOR THE SERVER TO CLIENT NETWORK-LATENCY
                if(Calendar.getInstance() > expiresAt) {

                    //get refresh tokens
                    val client: String = ZoomConfigs.CLIENT_ID + ":" + ZoomConfigs.CLIENT_SECRET
                    val base64Client =
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            Base64.getEncoder()
                                .encodeToString(client.toByteArray(StandardCharsets.UTF_8))
                        } else {
                            TODO("VERSION.SDK_INT < O")
                        }
                    var reqParam = URLEncoder.encode(
                        "refresh_token",
                        "UTF-8"
                    ) + "=" + URLEncoder.encode(token.refreshToken, "UTF-8")
                    reqParam += "&" + URLEncoder.encode(
                        "grant_type",
                        "UTF-8"
                    ) + "=" + URLEncoder.encode(
                        "refresh_token",
                        "UTF-8"
                    )
                    val mURL = URL("${ZoomConfigs.BASE_URL}/oauth/token")

                    try {
                        with(mURL.openConnection() as HttpURLConnection) {
                            requestMethod = "POST"
                            setRequestProperty("Authorization", "Basic $base64Client")
                            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                            setRequestProperty(
                                "Content-Length",
                                reqParam.toByteArray(StandardCharsets.UTF_8).size.toString()
                            )
                            doOutput = true

                            val wr = OutputStreamWriter(outputStream)
                            wr.write(reqParam)

                            wr.flush()

                            println("URL : $url")
                            println("Response Code : $responseCode")
                            println("Response message : $responseMessage")

                            BufferedReader(InputStreamReader(errorStream)).use {
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
                                val token = ZoomAccessToken()
                                token.accessToken = resp["access_token"].toString()
                                token.refreshToken = resp["refresh_token"].toString()
                                token.tokenType = resp["token_type"].toString()
                                token.expiresIn = resp["expires_in"].toString()
                                token.scope = resp["scope"].toString()
                                val expiresAt = Calendar.getInstance()
                                expiresAt.add(Calendar.HOUR_OF_DAY, 1)//acce
                                token.expiresAt = expiresAt.timeInMillis.toString()
                                val tokenString: String = GsonBuilder().create().toJson(
                                    token,
                                    ZoomAccessToken::class.java
                                )
                                Log.i("token", tokenString)
                                val sharedPref: SharedPreferences = getSharedPreferences(
                                    Util.ZOOM_ACCESS_TOKEN,
                                    Context.MODE_PRIVATE
                                )
                                sharedPref.edit()
                                    .putString(Util.ZOOM_ACCESS_TOKEN, tokenString).apply()
//                                getZakTokens(token, startMeeting)

                                if(startMeeting){
                                    startMeeting(token)
                                }

                            }
                        }

                    } catch (ex: Exception) {
                        println("http Error: ${ex.message}")
                    }
                }
            }
        }
    }
}