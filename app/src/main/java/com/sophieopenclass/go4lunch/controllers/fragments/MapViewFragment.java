package com.sophieopenclass.go4lunch.controllers.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.sophieopenclass.go4lunch.MyViewModel;
import com.sophieopenclass.go4lunch.R;
import com.sophieopenclass.go4lunch.base.BaseActivity;
import com.sophieopenclass.go4lunch.controllers.activities.LoginPageActivity;
import com.sophieopenclass.go4lunch.controllers.activities.RestaurantDetailsActivity;
import com.sophieopenclass.go4lunch.databinding.FragmentMapBinding;
import com.sophieopenclass.go4lunch.models.User;
import com.sophieopenclass.go4lunch.models.json_to_java.PlaceDetails;
import com.sophieopenclass.go4lunch.models.json_to_java.RestaurantsResult;

import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.sophieopenclass.go4lunch.utils.Constants.PLACE_ID;

public class MapViewFragment extends Fragment implements OnMapReadyCallback {

    private MyViewModel viewModel;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private Location currentLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    private static final float DEFAULT_ZOOM = 17.5f;
    private BaseActivity context;
    private RestaurantsResult restaurantsResult;
    private Location cameraLocation;

    public MapViewFragment() {
    }

    public static Fragment newInstance() {
        return new MapViewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentMapBinding binding = FragmentMapBinding.inflate(inflater, container, false);

        if (getActivity() != null) {
            context = (BaseActivity) getActivity();
            viewModel = (MyViewModel) ((BaseActivity) getActivity()).getViewModel();
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        if (ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermission();
        else {
            if (cameraLocation == null)
                fetchLastLocation();
            else
                getNearbyPlaces(currentLocation);
        }

        binding.fab.setOnClickListener(v -> fetchLastLocation());
        return binding.getRoot();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.setOnInfoWindowClickListener(this::startRestaurantActivity);
    }

    private void fetchLastLocation() {
        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
            requestLocationActivation();

        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        initMap();
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()) {
                currentLocation = task1.getResult();
                if (currentLocation != null) {
                    configureMap(currentLocation);
                    context.currentLocation = currentLocation;
                }
            } else {
                Toast.makeText(getActivity(), "unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void configureMap(Location currentLocation) {
        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
        getNearbyPlaces(currentLocation);
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                cameraLocation = new Location("cameraLocation");
                cameraLocation.setLongitude(mMap.getCameraPosition().target.longitude);
                cameraLocation.setLatitude(mMap.getCameraPosition().target.latitude);
                getNearbyPlaces(cameraLocation);
            }
        });
    }

    private void requestPermission() {
        requestPermissions(new String[]{ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLastLocation();
            } else {
                if (getView() != null)
                    Snackbar.make(getView(), "Géolocalisation désactivée", BaseTransientBottomBar.LENGTH_INDEFINITE)
                            .setDuration(5000).show();
            }
        }
    }

    private void requestLocationActivation() {
        new AlertDialog.Builder(context)
                .setMessage(R.string.gps_network_not_enabled)
                .setPositiveButton(R.string.open_location_settings, (paramDialogInterface, paramInt) ->
                        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton(R.string.Cancel, null)
                .show();
    }

    private void getNearbyPlaces(Location currentLocation) {
        viewModel.getNearbyPlaces(getLatLngString(currentLocation))
                .observe(getViewLifecycleOwner(), this::initMarkers);
    }

    static String getLatLngString(Location currentLocation) {
        return currentLocation.getLatitude() + "," + currentLocation.getLongitude();
    }

    private void initMarkers(RestaurantsResult restaurants) {
        this.restaurantsResult = restaurants;
        for (PlaceDetails placeDetails : restaurants.getPlaceDetails()) {
            viewModel.getUsersByPlaceId(placeDetails.getPlaceId()).observe(getViewLifecycleOwner(), users -> {
                int markerDrawable;
                if (users.isEmpty())
                    markerDrawable = R.drawable.ic_marker_red;
                else
                    markerDrawable = R.drawable.ic_marker_green;

                mMap.addMarker(new MarkerOptions().title(placeDetails.getName()).position(
                        new LatLng(placeDetails.getGeometry().getLocation().getLat(), placeDetails.getGeometry().getLocation().getLng()))
                        .icon(getBitmapFromVector(markerDrawable))).setTag(placeDetails.getPlaceId());
            });
        }
    }

    private void startRestaurantActivity(Marker marker) {
        if (marker.getTag() != null) {
            Intent intent = new Intent(getActivity(), RestaurantDetailsActivity.class);
            intent.putExtra(PLACE_ID, marker.getTag().toString());
            startActivity(intent);
        }
    }

    private BitmapDescriptor getBitmapFromVector(int drawableId) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), drawableId, null);
        Bitmap bitmap = null;
        if (drawable != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = (DrawableCompat.wrap(drawable)).mutate();
            }
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy");
        cameraLocation = null;
    }
}
