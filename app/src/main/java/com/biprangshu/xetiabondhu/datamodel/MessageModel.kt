package com.biprangshu.xetiabondhu.datamodel

import androidx.annotation.Keep

@Keep
data class MessageModel(
    val senderId: String ="",
    val message: String = "",
    val messageId: String = "",
    val timestamp: Long = 0
)
