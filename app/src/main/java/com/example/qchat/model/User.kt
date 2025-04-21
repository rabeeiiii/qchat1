package com.example.qchat.model

import java.io.Serializable

data class User(
    val name: String = "",
    var image: String? = null,
    var email: String? = null,
    var token: String? = null,
    val id: String = "",
    var status: String = "Just joined QChat — let's talk!"

): Serializable
