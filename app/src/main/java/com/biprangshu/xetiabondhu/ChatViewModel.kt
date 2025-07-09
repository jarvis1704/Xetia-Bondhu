package com.biprangshu.xetiabondhu

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.biprangshu.xetiabondhu.datamodel.MessageModel
import com.biprangshu.xetiabondhu.repository.FirebaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val auth: FirebaseAuth,
    application: Application
): AndroidViewModel(application){


    private val _messages= MutableStateFlow<List<MessageModel>>(emptyList())
    val messages: StateFlow<List<MessageModel>> = _messages.asStateFlow()

    private var messageListener: ValueEventListener? = null
    private var currentMessagesRef: DatabaseReference? = null

    private val realtimeDatabase = FirebaseDatabase.getInstance()

    fun sendMessageToAi(message: String){
        viewModelScope.launch {
            firebaseRepository.sendMessageToAi(message)
        }
    }

    fun loadMessages(){
        val currentUser = auth.currentUser?: return

        messageListener?.let { listener ->
            currentMessagesRef?.removeEventListener(listener)
        }
        val chatId = currentUser.uid

        currentMessagesRef = realtimeDatabase.getReference("chats/$chatId/messages")
        messageListener = currentMessagesRef!!.orderByChild("timestamp")
            .addValueEventListener(
                object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val messageList = mutableListOf<MessageModel>()
                        for(messageSnapshot in snapshot.children){
                            val message = messageSnapshot.getValue(MessageModel::class.java)
                            message?.let {
                                messageList.add(it)
                            }
                        }
                        _messages.value = messageList
                    }

                    override fun onCancelled(error: DatabaseError) {
                        //handle error
                    }
                }
            )
    }

    override fun onCleared() {
        super.onCleared()
        messageListener?.let { listener ->
            currentMessagesRef?.removeEventListener(listener)
        }
        messageListener = null
        currentMessagesRef = null
    }
}