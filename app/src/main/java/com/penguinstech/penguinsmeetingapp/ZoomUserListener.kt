package com.penguinstech.penguinsmeetingapp

interface ZoomUserListener {
    fun onSuccess(user_id:String)
    fun onError(error:String)
}