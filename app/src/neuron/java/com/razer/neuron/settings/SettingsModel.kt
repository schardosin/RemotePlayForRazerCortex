import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.limelight.R

sealed class SettingsState {
    object Finish : SettingsState()
}

abstract class SettingsGroup {
    abstract val groupId: Int
    abstract val titleId: Int
    abstract val items: List<SettingsItem>

    class AppSettingsGroup(override val items: List<SettingsItem>) : SettingsGroup() {

        override val groupId = R.id.menu_group_app_settings

        @StringRes
        override val titleId = R.string.rn_app_settings
    }


    class RemotePlayGroup(override val items: List<SettingsItem>) : SettingsGroup() {

        override val groupId = R.id.menu_group_remote_play

        @StringRes
        override val titleId = R.string.rn_remote_play_settings_localized
    }

    class HelpGroup(override val items: List<SettingsItem>) : SettingsGroup() {

        override val groupId = R.id.menu_group_help

        @StringRes
        override val titleId = R.string.rn_settings_help
    }
}

enum class SettingsItem(
    val itemId: Int,
    val titleId: Int,
    val iconId: Int,
    @IdRes val navigationId: Int
) {
    COMPUTERS(R.id.menu_item_computers, R.string.rn_settings_computers, R.drawable.ic_neuron_computer_offline, R.id.computers),
    APPEARANCE(R.id.menu_item_appearance, R.string.rn_settings_appearance, R.drawable.ic_appearance, R.id.appearance),
    STREAMING_OPTIONS(R.id.menu_item_streaming_options, R.string.rn_settings_streaming_options, R.drawable.ic_remote_controller, R.id.streaming_options),
    ABOUT(R.id.menu_item_about, R.string.rn_settings_about, R.drawable.ic_circled_info, R.id.about),
    DEV_OPTIONS(R.id.menu_item_dev_options, R.string.settings_dev_options, R.drawable.ic_settings, R.id.dev_options);

    companion object {
        fun getSettingsItemById(itemId: Int): SettingsItem? {
            return entries.find { itemId == it.itemId }
        }
    }
}