package com.penguinstech.penguinsmeetingapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*


class ScheduleMeetingDialog(private var token: ZoomAccessToken):
    BottomSheetDialogFragment() {

    val scheduleMeetingDialog:ScheduleMeetingDialog = this

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.schedule_meeting, container, false)
        val mCalendar: Calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            context,
            { timePicker: TimePicker?, selectedHour: Int, selectedMinute: Int ->
                mCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                mCalendar.set(Calendar.MINUTE, selectedMinute)
            },
            Calendar.getInstance()[Calendar.HOUR_OF_DAY],
            Calendar.getInstance()[Calendar.MINUTE],
            false
        )
        timePickerDialog.setTitle("Select Start Time")

        val submitBtn: Button = view.findViewById(R.id.submit)
        val startDate: Button = view.findViewById(R.id.startDate)
        val startTime: Button = view.findViewById(R.id.startTime)
        val topicTv: EditText = view.findViewById(R.id.topic)
        val durationTv: EditText = view.findViewById(R.id.duration)

        startTime.setOnClickListener { timePickerDialog.show() }
        startDate.setOnClickListener {
            context?.let {
                DatePickerDialog(
                    it,
                    { picker, year, monthOfYear, dayOfMonth ->
                        mCalendar[Calendar.YEAR] = year
                        mCalendar[Calendar.MONTH] = monthOfYear
                        mCalendar[Calendar.DAY_OF_MONTH] = dayOfMonth
                    },
                    mCalendar[Calendar.YEAR],
                    mCalendar[Calendar.MONTH],
                    mCalendar[Calendar.DAY_OF_MONTH],
                )
            }?.show()
        }
        submitBtn.setOnClickListener {
            if(mCalendar < Calendar.getInstance()) {
                Snackbar.make(it, "INVALID DATE", Snackbar.LENGTH_LONG).show()
            }else {
                val meeting = ScheduledMeeting()
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
                format.timeZone = TimeZone.getDefault()
                meeting.duration = if ( durationTv.text.toString().trim() != "")durationTv.text.toString() else "60"//duration in minutes
                meeting.topic = if ( topicTv.text.toString().trim() != "")topicTv.text.toString() else "New Appointment"
                meeting.start_time = format.format(mCalendar.time)
                meeting.timezone = TimeZone.getDefault().id
                scheduleMeeting(meeting)
            }
        }
        return view
    }


    private fun scheduleMeeting(meeting: ScheduledMeeting) {
        ZoomUser(object : ZoomUserListener {
            override fun onError(error: String) {
                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            }
            override fun onSuccess(user_id: String) {
                val meetingJsonToMap: HashMap<String, Any> = Gson().fromJson(
                    GsonBuilder().create().toJson(
                        meeting,
                        ScheduledMeeting::class.java
                    ),
                    HashMap::class.java
                ) as HashMap<String, Any>

                var reqParam = ""
                val jsonString = JSONObject()
                for ((key, value) in meetingJsonToMap.entries) {

                    jsonString.put(key, value.toString())
//                    if (reqParam != "") {
//                        reqParam += "&"
//                    }
//                    reqParam += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(
//                        value.toString(),
//                        "UTF-8"
//                    )
                }
                reqParam = jsonString.toString()

                println("request params $reqParam")

                val mURL = URL(ZoomConfigs.getZoomScheduleUrl(user_id))

                try {
                    with(mURL.openConnection() as HttpURLConnection) {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "Bearer  ${token.accessToken}")
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

                            //display/save meeting details
                            val databaseReference: DatabaseReference =  FirebaseDatabase.getInstance().getReference("meetings").child("user_1");
                            val firebaseId:String = databaseReference.push().key.toString()
                            databaseReference.child(firebaseId).setValue(resp).addOnSuccessListener {

                                    Toast.makeText(context, "saved in firebase", Toast.LENGTH_LONG)
                                        .show()
                                }
                            Toast.makeText(
                                context,
                                "start_url: " + resp["start_url"].toString(),
                                Toast.LENGTH_LONG
                            ).show()
                            Toast.makeText(
                                context,
                                "join_url: " + resp["join_url"].toString(),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        scheduleMeetingDialog.dismiss()
                    }

                } catch (ex: Exception) {
                    println("http Error: ${ex.message}")
                }

            }
        }).getUserId(token)

    }
}