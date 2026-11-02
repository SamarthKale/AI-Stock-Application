package com.stockpredictor.app.ui.profile

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stockpredictor.app.data.remote.firebase.FirebaseAuthRepository
import com.stockpredictor.app.data.remote.firebase.FirestoreSyncRepository
import com.stockpredictor.app.data.remote.firebase.ProfilePhotoRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UserProfile(
    val displayName: String,
    val firstName: String,
    val email: String?,
    val photo: Bitmap?,
)

/**
 * Activity-scoped (one instance for the whole authenticated session, obtained in AppNavHost) so the
 * Home greeting, the navigation drawer and the Profile header always agree on name/photo without
 * each screen re-fetching. Settings' own SettingsViewModel still owns *editing* the photo; call
 * [refreshPhoto] after it changes.
 */
class UserProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuthRepository()
    private val photoRepository = ProfilePhotoRepository()
    private val syncRepository = FirestoreSyncRepository.getInstance(application)

    private val _photo = MutableStateFlow<Bitmap?>(null)

    val profile: StateFlow<UserProfile> = combine(
        kotlinx.coroutines.flow.flow { emit(null); auth.authStateFlow().collect { emit(it) } },
        _photo,
    ) { user, photo ->
        val email = user?.email ?: auth.currentUser?.email
        val name = user?.displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')?.replaceFirstChar { it.uppercase() }
            ?: "there"
        UserProfile(displayName = name, firstName = name.substringBefore(' '), email = email, photo = photo)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UserProfile("there", "there", auth.currentUser?.email, null))

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    init {
        refreshPhoto()
    }

    fun refreshPhoto() {
        viewModelScope.launch { _photo.value = photoRepository.load() }
    }

    /** Same ordered sequence SettingsViewModel.logout() runs (stop listener, clear the local
     *  cache, then sign out) so the drawer's Log Out is exactly as safe as Settings'. */
    fun logout() {
        viewModelScope.launch {
            syncRepository.stopListening()
            syncRepository.clearLocalCache()
            auth.signOut()
            _photo.value = null
            _logoutEvent.emit(Unit)
        }
    }
}
