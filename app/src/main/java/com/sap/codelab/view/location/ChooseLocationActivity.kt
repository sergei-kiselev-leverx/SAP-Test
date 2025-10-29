package com.sap.codelab.view.location

import android.os.Bundle
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityChooseLocationBinding
import com.sap.codelab.model.MemoLocation
import com.sap.codelab.utils.extensions.setupEdgeToEdge
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker


class ChooseLocationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChooseLocationBinding
    private lateinit var model: ChooseLocationViewModel
    private var userMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupEdgeToEdge(binding.root)
        model = ViewModelProvider(this)[ChooseLocationViewModel::class.java]
        val args = ChooseLocationContract.Companion.getArgs(intent)
        if (savedInstanceState == null) {
            model.initArgs(args, this)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.data.collect { uiState ->
                    updateUI(uiState)
                }
            }
        }
        initMap(args)
        initListeners(args)
        setupVisibility(args)
    }

    private fun setupVisibility(args: ChooseLocationArgs) {
        binding.contentChooseLocation.save.isVisible = args.canChooseLocation
    }

    private fun initListeners(args: ChooseLocationArgs) {
        if (!args.canChooseLocation) return

        binding.contentChooseLocation.save.setOnClickListener {
            finishWithResult(model.data.value)
        }

        onBackPressedDispatcher.addCallback {
            finishWithResult(args.location)
        }
    }

    private fun finishWithResult(location: MemoLocation?) {
        setResult(
            RESULT_OK,
            ChooseLocationContract.Companion.createResult(ChooseLocationResult(location))
        )
        finish()
    }

    private fun initMap(args: ChooseLocationArgs) {
        with(binding.contentChooseLocation.map) {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            val location = model.data.value
            controller.setCenter(GeoPoint(location.latitude, location.longitude))

            if (!args.canChooseLocation) return

            val mapEventsReceiver = object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    if (p != null) {
                        model.updateLocation(p.latitude, p.longitude)
                    }
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            }
            val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
            overlays.add(mapEventsOverlay)
        }
    }

    private fun updateUI(location: MemoLocation) {
        with(binding.contentChooseLocation) {
            val geoPoint = GeoPoint(location.latitude, location.longitude)

            userMarker?.let { map.overlays.remove(it) }

            userMarker = Marker(map)
                .apply {
                    position = geoPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                .also {
                    map.overlays.add(it)
                }

            map.controller.animateTo(geoPoint)
            coordinates.text = getString(R.string.location_data, location.latitude, location.longitude)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.contentChooseLocation.map.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.contentChooseLocation.map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.contentChooseLocation.map.onDetach()
    }
}