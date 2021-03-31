package org.openvoipalliance.voiplibexample.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_dialer.*
import org.openvoipalliance.voiplib.VoIPLib
import org.openvoipalliance.voiplibexample.R
import org.openvoipalliance.voiplibexample.ui.Dialer

class DialerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dialer, container, false)
        return root
    }

    override fun onResume() {
        super.onResume()
        requestCallingPermissions()
    }

    override fun onPause() {
        super.onPause()
    }


    private fun requestCallingPermissions() {
        val requiredPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)

        requiredPermissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(requireActivity(), permission) == PERMISSION_DENIED) {
                requireActivity().requestPermissions(requiredPermissions, 101)
                return
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialer.onCallListener = Dialer.OnCallListener { number ->
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                VoIPLib.getInstance(requireContext()).callTo(number)
            }

        }
    }
}
