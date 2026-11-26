package org.dueattendant149.bebringo.ui.screens.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dueattendant149.bebringo.di.ServiceLocator
import org.dueattendant149.bebringo.widget.BebringoWidgets

data class SettingsUiState(
    val llmEndpoint: String = "",
    val llmModel: String = "",
    val llmKey: String = "",
    val webdavUrl: String = "",
    val webdavUser: String = "",
    val webdavPassword: String = "",
    val strictDiacritics: Boolean = false,
    val dynamicColor: Boolean = false,
    val soundEnabled: Boolean = true,
    val autoPlayAudio: Boolean = false,
    val supportLevel: Int = 1,
    val desiredRetention: Int = 90,
    val localBackupCount: Int = 0,
    val message: String? = null,
    val busy: Boolean = false,
)

/** Настройки: модель, WebDAV, бэкап/восстановление, диакритика, подпорка, FSRS и Material You. */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = ServiceLocator.settings

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val localFiles = ServiceLocator.backupManager.listLocalBackups()
            _state.update {
                it.copy(
                    llmEndpoint = settings.llmEndpoint.first(),
                    llmModel = settings.llmModel.first(),
                    llmKey = settings.llmKey.first(),
                    webdavUrl = settings.webdavUrl.first(),
                    webdavUser = settings.webdavUser.first(),
                    webdavPassword = settings.webdavPassword.first(),
                    strictDiacritics = settings.strictDiacritics.first(),
                    dynamicColor = settings.dynamicColor.first(),
                    soundEnabled = settings.soundEnabled.first(),
                    autoPlayAudio = settings.autoPlayAudio.first(),
                    supportLevel = settings.supportLevel.first(),
                    desiredRetention = settings.desiredRetention.first(),
                    localBackupCount = localFiles.size,
                )
            }
        }
    }

    fun updateLlm(endpoint: String, model: String, key: String) {
        _state.update { it.copy(llmEndpoint = endpoint, llmModel = model, llmKey = key) }
    }

    fun updateWebdav(url: String, user: String, password: String) {
        _state.update { it.copy(webdavUrl = url, webdavUser = user, webdavPassword = password) }
    }

    fun saveLlm() {
        val state = _state.value
        viewModelScope.launch {
            settings.setLlm(state.llmEndpoint, state.llmModel, state.llmKey)
            _state.update { it.copy(message = "Настройки модели сохранены") }
        }
    }

    fun saveWebdav() {
        val state = _state.value
        viewModelScope.launch {
            settings.setWebdav(state.webdavUrl, state.webdavUser, state.webdavPassword)
            _state.update { it.copy(message = "Настройки WebDAV сохранены") }
        }
    }

    fun setStrictDiacritics(value: Boolean) {
        viewModelScope.launch {
            settings.setStrictDiacritics(value)
            _state.update { it.copy(strictDiacritics = value) }
        }
    }

    fun setDynamicColor(value: Boolean) {
        viewModelScope.launch {
            settings.setDynamicColor(value)
            _state.update { it.copy(dynamicColor = value) }
        }
    }

    fun setSoundEnabled(value: Boolean) {
        viewModelScope.launch {
            settings.setSoundEnabled(value)
            _state.update { it.copy(soundEnabled = value) }
        }
    }

    fun setAutoPlayAudio(value: Boolean) {
        viewModelScope.launch {
            settings.setAutoPlayAudio(value)
            _state.update { it.copy(autoPlayAudio = value) }
        }
    }

    fun setSupportLevel(level: Int) {
        viewModelScope.launch {
            settings.setSupportLevel(level)
            _state.update { it.copy(supportLevel = level.coerceIn(0, 3)) }
        }
    }

    fun setDesiredRetention(percent: Int) {
        viewModelScope.launch {
            settings.setDesiredRetention(percent)
            _state.update { it.copy(desiredRetention = percent.coerceIn(80, 95)) }
        }
    }

    fun exportNow() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val file = ServiceLocator.backupManager.exportToFile()
                ServiceLocator.backupManager.pruneLocal()
                val count = ServiceLocator.backupManager.listLocalBackups().size
                file.name to count
            }.onSuccess { (fileName, count) ->
                _state.update {
                    it.copy(
                        busy = false,
                        localBackupCount = count,
                        message = "Копия сохранена ($fileName)",
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = "Ошибка: ${error.message}") }
            }
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                ServiceLocator.backupManager.import(uri)
                BebringoWidgets.updateAll(getApplication())
            }.onSuccess {
                _state.update { it.copy(busy = false, message = "Данные успешно восстановлены из файла") }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = "Ошибка импорта: ${error.message}") }
            }
        }
    }

    fun restoreLatestLocal() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                val latest = ServiceLocator.backupManager.listLocalBackups().firstOrNull()
                    ?: throw IllegalStateException("Нет локальных копий")
                ServiceLocator.backupManager.importFile(latest)
                BebringoWidgets.updateAll(getApplication())
                latest.name
            }.onSuccess { name ->
                _state.update { it.copy(busy = false, message = "Восстановлена копия: $name") }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = "Ошибка: ${error.message}") }
            }
        }
    }

    fun uploadNow() {
        val state = _state.value
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            runCatching {
                ServiceLocator.backupManager.uploadToWebdav(
                    url = state.webdavUrl,
                    user = state.webdavUser,
                    password = state.webdavPassword,
                )
            }.onSuccess { ok ->
                _state.update {
                    it.copy(
                        busy = false,
                        message = if (ok) "Копия отправлена на WebDAV" else "Проверьте адрес и учётные данные WebDAV",
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(busy = false, message = "Ошибка: ${error.message}") }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
