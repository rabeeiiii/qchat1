package com.example.qchat.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.qchat.R
import com.example.qchat.model.User
import com.example.qchat.utils.decodeToBitmap

class ProfileFragment : Fragment(R.layout.profile_fragment) {

    private lateinit var user: User
    private lateinit var userName: TextView
    private lateinit var userImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.profile_fragment, container, false)

        user = arguments?.getSerializable("user") as? User ?: User("", "", "", "", "")
        userName = view.findViewById(R.id.username)
        userImage = view.findViewById(R.id.profileImage)

        loadUserProfile(user)

        return view
    }

    private fun loadUserProfile(user: User) {
        userName.text = user.name

        user.image?.let {
            val bitmap = it.decodeToBitmap()
            if (bitmap != null) {
                userImage.setImageBitmap(bitmap)
            }
        }
    }

    companion object {
        fun newInstance(user: User): ProfileFragment {
            val fragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putSerializable("user", user)
            fragment.arguments = bundle
            return fragment
        }
    }
}
