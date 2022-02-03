package com.penguinstech.penguinsmeetingapp

class ZoomConfigs {
    companion object {
        const val BASE_URL = "https://zoom.us/"
        const val CLIENT_ID = "GoL2V6JSTYOM25BbWL38gQ"
        const val CLIENT_SECRET = "g5VQbU97fiLGiKjFnwQWXLgj6uErl1Z4"
        const val APP_DYNAMIC_LINK = "https://penguins-app-redirect.herokuapp.com/"
        const val ZOOM_ZAK_URL = "https://api.zoom.us/v2/users/me/zak"
        const val ZOOM_USER_URL = "https://api.zoom.us/v2/users/me"
        fun getZoomScheduleUrl (userId:String):String {
            return "https://api.zoom.us/v2/users/${userId}/meetings"
        }

    }
}