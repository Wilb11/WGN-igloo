package com.example.wgn_igloo.inbox


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// ViewModel used to signal that new Notification items need to be fetched
class NotificationsViewModel: ViewModel() {
    private val _refreshNotifications = MutableLiveData<Boolean>()

    val refreshNotifications: LiveData<Boolean> get() = _refreshNotifications

    fun setRefreshNotifications(refresh: Boolean) {
        _refreshNotifications.value = refresh
    }
}