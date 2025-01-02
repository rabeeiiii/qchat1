package fragments
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.qchat.R
import com.example.qchat.adapter.SettingsAdapter
import com.example.qchat.model.SettingItem
import com.google.android.material.appbar.MaterialToolbar

class SettingsFragment : Fragment() {

    private val settingsItems = listOf(
        SettingItem(
            R.drawable.ic_key,
            "Account",
            "Privacy, security, change number"
        ),
        SettingItem(
            R.drawable.ic_chat,
            "Chat",
            "Chat history, themes, wallpapers"
        ),
        SettingItem(
            R.drawable.ic_notifications,
            "Notifications",
            "Messages, group and others"
        ),
        SettingItem(
            R.drawable.ic_help,
            "Help",
            "Help center, contact us, privacy policy"
        ),
        SettingItem(
            R.drawable.ic_storage,
            "Storage and data",
            "Network usage, storage usage"
        ),
        SettingItem(
            R.drawable.ic_invite,
            "Invite a friend",
            "Share this app with friends"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(view)
        setupRecyclerView(view)
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.settingsRecyclerView)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = SettingsAdapter(settingsItems)
        }
    }

    private fun setupToolbar(view: View) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        (requireActivity() as AppCompatActivity).apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Settings"
        }
    }
}