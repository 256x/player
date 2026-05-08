package fumi.day.literalplayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fumi.day.literalplayer.data.prefs.UserPreferences
import fumi.day.literalplayer.data.prefs.UserPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(prefs: UserPreferences) : ViewModel() {
    val prefs = prefs.prefs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserPrefs()
    )
}
