package com.penguinstech.penguinsmeetingapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MeetingsAdapter(context: Context, meetingList: List<Meeting>) :
    RecyclerView.Adapter<MeetingsAdapter.ViewHolder>() {
    lateinit var meetingList: List<Meeting>
    lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val myView: View =
            LayoutInflater.from(context).inflate(R.layout.meeting_details_layout, parent, false)
        return ViewHolder(myView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meeting: Meeting = meetingList[position]
        holder.topicTv.text = meeting.topic
        holder.meetingIdTv.text = meeting.meetingId
        holder.startMeetingBtn.setOnClickListener {

            val browserIntent = Intent(
                Intent.ACTION_VIEW, Uri.parse(
                    meeting.startUrl
                )
            )
            context.startActivity(browserIntent)
        }
        holder.joinMeetingBtn.setOnClickListener {

            val browserIntent = Intent(
                Intent.ACTION_VIEW, Uri.parse(
                    meeting.joinUrl
                )
            )
            context.startActivity(browserIntent)
        }
    }

    override fun getItemCount(): Int {
        return meetingList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var topicTv: TextView
        var meetingIdTv: TextView
        var startMeetingBtn: Button
        var joinMeetingBtn: Button

        init {
            topicTv = itemView.findViewById(R.id.topicTv)
            meetingIdTv = itemView.findViewById(R.id.meetingIdTv)
            startMeetingBtn = itemView.findViewById(R.id.startMeetingBtn)
            joinMeetingBtn = itemView.findViewById(R.id.joinMeetingBtn)
        }
    }

    init {
        this.meetingList = meetingList
        this.context = context
    }
}
