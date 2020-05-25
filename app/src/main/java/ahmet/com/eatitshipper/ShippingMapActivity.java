package ahmet.com.eatitshipper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;
import com.squareup.picasso.Picasso;

import net.cachapa.expandablelayout.ExpandableLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ahmet.com.eatitshipper.common.Common;
import ahmet.com.eatitshipper.model.ShippingOrder;
import ahmet.com.eatitshipper.remote.IGoogleAPI;
import ahmet.com.eatitshipper.remote.RetrofitClient;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.paperdb.Paper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ShippingMapActivity extends FragmentActivity implements OnMapReadyCallback {

    @BindView(R.id.img_order_food)
    ImageView mImgOrderFood;
    @BindView(R.id.txt_order_food_date)
    TextView mTxtOrderFoodDate;
    @BindView(R.id.txt_order_food_number)
    TextView mTxtOrderFoodNumber;
    @BindView(R.id.txt_order_food_address)
    TextView mTxtOrderFoodAddress;
    @BindView(R.id.txt_order_user_name)
    TextView mTxtOrderUsername;

    @BindView(R.id.btn_start_trip)
    MaterialButton mBtnStartTrip;
    @BindView(R.id.btn_call)
    MaterialButton mBtnCall;
    @BindView(R.id.btn_done)
    MaterialButton mBtnDone;
    @BindView(R.id.btn_show_hide_shipping_order)
    MaterialButton mBtnShowHide;

    @BindView(R.id.expandable_layout)
    ExpandableLayout mExpandableLayout;


    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private Location previousLocation = null;

    private AutocompleteSupportFragment mAutocompletePlacesFragment;
    private PlacesClient mPlacesClient;
    private List<Place.Field> mListPlaceField;


    private boolean isInit = false;

    private Marker mShipperMarker;

    private ShippingOrder shippingOrder;

    // Animation
    private Handler handler;
    private int index, next;
    private LatLng start, end;
    private float v;
    private double lat, lng;
    private Polyline blackPolyline, grayPolyline, redPolyline, yellowPolyline;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private List<LatLng> mListPolyLine;

    private IGoogleAPI mIGoogleAPI;
    private CompositeDisposable mDisposable;

    @OnClick(R.id.btn_start_trip)
    void onStartTripClick() {

        String data = Paper.book().read(Common.KEY_SHIPPING_ORDER_DATA);
        Paper.book().write(Common.KEY_START_TRIP, data);

        mBtnStartTrip.setEnabled(false);

        shippingOrder = new Gson().fromJson(data, new TypeToken<ShippingOrder>() {
        }.getType());
        // Update
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {

                    mDisposable.add(mIGoogleAPI.getDirections("driving",
                            "less_driving",
                            Common.buildLocationString(location),
                            new StringBuilder().append(shippingOrder.getOrder().getLat())
                                    .append(",")
                                    .append(shippingOrder.getOrder().getLng())
                                    .toString(),
                            getString(R.string.google_maps_key))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(s -> {
                                // Get estimate from API
                                String estimateTime = "UNKNOWN";
                                JSONObject jsonObject = new JSONObject(s);
                                JSONArray routes = jsonObject.getJSONArray("routes");
                                JSONObject object = routes.getJSONObject(0);
                                JSONArray legs = object.getJSONArray("legs");
                                JSONObject legsObject = legs.getJSONObject(0);
                                // Time
                                JSONObject time = legsObject.getJSONObject("duration");
                                estimateTime = time.getString("text");

                                Map<String, Object> mapUpdateData = new HashMap<>();
                                mapUpdateData.put("currentLat", location.getLatitude());
                                mapUpdateData.put("currentLng", location.getLongitude());
                                mapUpdateData.put("estimateTime", estimateTime);

                                FirebaseDatabase.getInstance().getReference()
                                        .child(Common.KEY_SHIPPING_ORDER_REFERANCE)
                                        .child(shippingOrder.getKey())
                                        .updateChildren(mapUpdateData)
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.e("BTNUPDATE_ORDERLOCATION", e.getMessage());
                                        }).addOnSuccessListener(aVoid -> {
                                    drawRoutes(data);
                                });

                            }, throwable -> {
                                Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }));

                }).addOnFailureListener(e -> {
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("BTN_START_TRIP_ERRPR", e.getMessage());
        });
    }

    @OnClick(R.id.btn_show_hide_shipping_order)
    void onShowHideClick() {
        if (mExpandableLayout.isExpanded())
            mBtnShowHide.setText(R.string.show);
        else
            mBtnShowHide.setText(R.string.hide);
        mExpandableLayout.toggle();
    }

    @OnClick(R.id.btn_call)
    void onCallUserClicke() {

        if (shippingOrder != null) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

                Dexter.withActivity(this)
                        .withPermission(Manifest.permission.CALL_PHONE)
                        .withListener(new PermissionListener() {
                            @Override
                            public void onPermissionGranted(PermissionGrantedResponse response) {

                                Intent intent = new Intent(Intent.ACTION_CALL);
                                intent.setData(Uri.parse(new StringBuilder("tel:")
                                        .append(shippingOrder.getOrder().getUserPhone())
                                        .toString()));

                                startActivity(intent);
                            }

                            @Override
                            public void onPermissionDenied(PermissionDeniedResponse response) {
                                Toast.makeText(ShippingMapActivity.this, getString(R.string.enable_permission) +
                                        " " + response.getPermissionName(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                            }
                        }).check();
                return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipping_map);

        ButterKnife.bind(this);
        Paper.init(this);

        init();
        initPlaces();

        setupeAutocompletePlaces();

        // location
        buildLocationRequest();
        buildLocationCallback();


        // Permission
        requestPermission();


    }

    private void setupeAutocompletePlaces() {

        mAutocompletePlacesFragment = (AutocompleteSupportFragment) getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        mAutocompletePlacesFragment.setPlaceFields(mListPlaceField);
        mAutocompletePlacesFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                drawRoutes(place);
            }

            @Override
            public void onError(@NonNull Status status) {
               // Log.e("PLACES_ERROR", status.getStatusMessage());
                Toast.makeText(ShippingMapActivity.this, ""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void drawRoutes(Place place) {


        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                .title(place.getName())
                .snippet(place.getAddress())
                .position(place.getLatLng()));

        mFusedLocationClient.getLastLocation()
                .addOnFailureListener(e -> {
                    Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnSuccessListener(location -> {
            String to = new StringBuilder()
                    .append(place.getLatLng().latitude)
                    .append(",")
                    .append(place.getLatLng().longitude)
                    .toString();
            String from = new StringBuilder()
                    .append(location.getLatitude())
                    .append(",")
                    .append(location.getLongitude())
                    .toString();

            mDisposable.add(mIGoogleAPI.getDirections("driving",
                    "less_driving",
                    from,to,
                    getString(R.string.google_maps_key))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> {
                        try{
                            // Parse json
                            JSONObject jsonObject = new JSONObject(s);
                            JSONArray jsonArray = jsonObject.getJSONArray("routes");
                            for (int i = 0; i < jsonArray.length(); i++){
                                JSONObject route = jsonArray.getJSONObject(i);
                                JSONObject poly = route.getJSONObject("overview_polyline");
                                String polyline = poly.getString("points");
                                mListPolyLine = Common.decodePoly(polyline);
                            }

                            polylineOptions = new PolylineOptions();
                            polylineOptions.color(Color.YELLOW);
                            polylineOptions.width(12);
                            polylineOptions.startCap(new SquareCap());
                            polylineOptions.jointType(JointType.ROUND);
                            polylineOptions.addAll(mListPolyLine);
                            yellowPolyline = mMap.addPolyline(polylineOptions);

                        }catch (Exception e){
                            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    }, throwable -> {
                        Toast.makeText(this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }));
        });

    }

    private void drawRoutes(String shippingOrderData) {

        ShippingOrder shippingOrder = new Gson()
                .fromJson(shippingOrderData, new TypeToken<ShippingOrder>(){}.getType());
        // Add box
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                .title(shippingOrder.getOrder().getUserName())
                .snippet(shippingOrder.getOrder().getShippingAddress())
                .position(new LatLng(shippingOrder.getOrder().getLat(),shippingOrder.getOrder().getLng())));

        mFusedLocationClient.getLastLocation()
                .addOnFailureListener(e -> {
                    Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnSuccessListener(location -> {
            String to = new StringBuilder()
                    .append(shippingOrder.getOrder().getLat())
                    .append(",")
                    .append(shippingOrder.getOrder().getLng())
                    .toString();
            String from = new StringBuilder()
                    .append(location.getLatitude())
                    .append(",")
                    .append(location.getLongitude())
                    .toString();

            mDisposable.add(mIGoogleAPI.getDirections("driving",
                    "less_driving",
                    from,to,
                    getString(R.string.google_maps_key))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(s -> {
                        try{
                            // Parse json
                            JSONObject jsonObject = new JSONObject(s);
                            JSONArray jsonArray = jsonObject.getJSONArray("routes");
                            for (int i = 0; i < jsonArray.length(); i++){
                                JSONObject route = jsonArray.getJSONObject(i);
                                JSONObject poly = route.getJSONObject("overview_polyline");
                                String polyline = poly.getString("points");
                                mListPolyLine = Common.decodePoly(polyline);
                            }

                            polylineOptions = new PolylineOptions();
                            polylineOptions.color(Color.RED);
                            polylineOptions.width(12);
                            polylineOptions.startCap(new SquareCap());
                            polylineOptions.jointType(JointType.ROUND);
                            polylineOptions.addAll(mListPolyLine);
                            redPolyline = mMap.addPolyline(polylineOptions);

                        }catch (Exception e){
                            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    }, throwable -> {
                        Toast.makeText(this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }));
        });
    }

    private void initPlaces() {

        Places.initialize(this, getString(R.string.google_maps_key));

        mPlacesClient = Places.createClient(this);

        mListPlaceField = Arrays.asList(Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG);

    }

    private void init() {

        mDisposable = new CompositeDisposable();
        mIGoogleAPI = RetrofitClient.getRetrofit().create(IGoogleAPI.class);
    }

    private void setShippingOrderData() {

        String shippingOrderData;

        if (TextUtils.isEmpty(Paper.book().read(Common.KEY_START_TRIP))){
            // if empty just do normal
            mBtnStartTrip.setEnabled(true);
            shippingOrderData = Paper.book().read(Common.KEY_SHIPPING_ORDER_DATA);
        }else{
            mBtnStartTrip.setEnabled(false);
            shippingOrderData = Paper.book().read(Common.KEY_START_TRIP);
        }

        if (!TextUtils.isEmpty(shippingOrderData)) {

            drawRoutes(shippingOrderData);

            shippingOrder = new Gson().fromJson(shippingOrderData,
                    new TypeToken<ShippingOrder>() {
                    }.getType());

            if (shippingOrder != null) {

                Common.setSpanStringColor(getString(R.string.name) + " ",
                        shippingOrder.getOrder().getUserName(),
                        mTxtOrderUsername,
                        getColor(R.color.colorDarkBlack));

                mTxtOrderFoodDate.setText(new StringBuilder(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a")
                        .format(shippingOrder.getOrder().getDate())));

                Common.setSpanStringColor(getString(R.string.number) + " ",
                        shippingOrder.getOrder().getKey(),
                        mTxtOrderFoodNumber,
                        getColor(android.R.color.holo_blue_dark));

                Common.setSpanStringColor(getString(R.string.address) + " ",
                        shippingOrder.getOrder().getShippingAddress(),
                        mTxtOrderFoodAddress,
                        getColor(R.color.colorPurple));

                Picasso.get().load(shippingOrder.getOrder().getCarts().get(0).getFoodImage()).into(mImgOrderFood);

            }

        } else
            Toast.makeText(this, "can not get Shipping order data", Toast.LENGTH_SHORT).show();
    }


    private void requestPermission() {

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(ShippingMapActivity.this);

                        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(ShippingMapActivity.this);
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());

                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(ShippingMapActivity.this, getString(R.string.enable_permission) +
                                " " + response.getPermissionName(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();
    }

    private void buildLocationCallback() {

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();

                // Add a marker in Sydney and move the camera
                LatLng shipperLocation = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());

                updateLocation(locationResult.getLastLocation());

                if (mShipperMarker == null) {
                    // inflate drawable
                    int height, width;
                    height = width = 80;
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(ShippingMapActivity.this, R.drawable.shipper);
                    Bitmap resize = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), width, height, false);

                    mShipperMarker = mMap.addMarker(new MarkerOptions()
                            .position(shipperLocation)
                            .icon(BitmapDescriptorFactory.fromBitmap(resize))
                            .title("You"));
                                                                                    // Change to 18
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(shipperLocation,18));
                }


                if (isInit && previousLocation != null){

                    String from = new StringBuilder()
                            .append(previousLocation.getLatitude())
                            .append(",")
                            .append(previousLocation.getLongitude())
                            .toString();

                    String to = new StringBuilder()
                            .append(shipperLocation.latitude)
                            .append(",")
                            .append(shipperLocation.longitude)
                            .toString();

                    moveMarkerAnimation(mShipperMarker, from, to);

                    previousLocation = locationResult.getLastLocation();
                }

                if (!isInit){
                    isInit = true;
                    previousLocation = locationResult.getLastLocation();
                }
            }
        };
    }

    private void updateLocation(Location lastLocation) {


        String data = Paper.book().read(Common.KEY_START_TRIP);
        if (!TextUtils.isEmpty(data)){

            ShippingOrder shippingOrder = new Gson().fromJson(data,
                    new TypeToken<ShippingOrder>(){}.getType());
            if (shippingOrder != null){

                mDisposable.add(mIGoogleAPI.getDirections("driving",
                        "less_driving",
                        Common.buildLocationString(lastLocation),
                        new StringBuilder().append(shippingOrder.getOrder().getLat())
                                .append(",")
                                .append(shippingOrder.getOrder().getLng())
                                .toString(),
                        getString(R.string.google_maps_key))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(s -> {
                            // Get estimate from API
                            String estimateTime = "UNKNOWN";
                            JSONObject jsonObject = new JSONObject(s);
                            JSONArray routes = jsonObject.getJSONArray("routes");
                            JSONObject object = routes.getJSONObject(0);
                            JSONArray legs = object.getJSONArray("legs");
                            JSONObject legsObject = legs.getJSONObject(0);
                            // Time
                            JSONObject time = legsObject.getJSONObject("duration");
                            estimateTime = time.getString("text");

                            Map<String, Object> mapUpdateData = new HashMap<>();
                            mapUpdateData.put("currentLat", lastLocation.getLatitude());
                            mapUpdateData.put("currentLng", lastLocation.getLongitude());
                            mapUpdateData.put("estimateTime", estimateTime);

                            FirebaseDatabase.getInstance().getReference()
                                    .child(Common.KEY_SHIPPING_ORDER_REFERANCE)
                                    .child(shippingOrder.getKey())
                                    .updateChildren(mapUpdateData)
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        Log.e("UPDATELOCATION_SHIPPING", e.getMessage());
                                    }); // In this function don not need draw path again

                        }, throwable -> {
                            Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        }));

            }
        }else
            Toast.makeText(this, getString(R.string.please_press_btn_trip), Toast.LENGTH_SHORT).show();
    }

    private void moveMarkerAnimation(Marker marker, String from, String to) {

        // Request directions API to get data
        mDisposable.add(mIGoogleAPI.getDirections("driving",
                "less_driving",
                from, to, getString(R.string.google_maps_key))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(result -> {

            try {
                // Parse json
                JSONObject jsonObject = new JSONObject(result);
                JSONArray jsonArray = jsonObject.getJSONArray("routes");

                for (int i = 0; i < jsonArray.length(); i++){
                    JSONObject route = jsonArray.getJSONObject(i);
                    JSONObject poly = route.getJSONObject("overview_polyline");
                    String polyline = poly.getString("points");
                    mListPolyLine = Common.decodePoly(polyline);
                }

                polylineOptions = new PolylineOptions();
                polylineOptions.color(Color.GRAY);
                polylineOptions.width(5);
                polylineOptions.startCap(new SquareCap());
                polylineOptions.jointType(JointType.ROUND);
                polylineOptions.addAll(mListPolyLine);
                grayPolyline = mMap.addPolyline(polylineOptions);

                blackPolylineOptions = new PolylineOptions();
                blackPolylineOptions.color(Color.BLACK);
                blackPolylineOptions.width(5);
                blackPolylineOptions.startCap(new SquareCap());
                blackPolylineOptions.jointType(JointType.ROUND);
                blackPolylineOptions.addAll(mListPolyLine);
                blackPolyline = mMap.addPolyline(blackPolylineOptions);

                // Animator
                ValueAnimator polylineAnimator = ValueAnimator.ofInt(0,100);
                polylineAnimator.setDuration(2000);
                polylineAnimator.setInterpolator(new LinearInterpolator());
                polylineAnimator.addUpdateListener(valueAnimato -> {
                    List<LatLng> points = grayPolyline.getPoints();
                    int percentValue = (int) valueAnimato.getAnimatedValue();
                    int size = points.size();
                    int newPoint = (int) (size*(percentValue/100.0f));
                    List<LatLng> p = points.subList(0, newPoint);
                    blackPolyline.setPoints(p);

                });

                polylineAnimator.start();

                // Bike moving
                handler = new Handler();
                index = -1;
                next = 1;

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (index < mListPolyLine.size()-1){
                            index++;
                            next = index+1;
                            start = mListPolyLine.get(index);
                            end = mListPolyLine.get(next);
                        }

                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0,1);
                        valueAnimator.setDuration(1500);
                        valueAnimator.setInterpolator(new LinearInterpolator());
                        valueAnimator.addUpdateListener(valueAnimator1 -> {
                            v = valueAnimator1.getAnimatedFraction();
                            lng = v * end.longitude + (1-v) * start.longitude;
                            lat = v * end.latitude + (1-v) * start.latitude;
                            LatLng newPos = new LatLng(lat,lng);
                            marker.setPosition(newPos);
                            marker.setAnchor(0.5f,0.5f);
                            marker.setRotation(Common.getBearing(start, newPos));

                            mMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                        });
                        valueAnimator.start();

                        // Reach destination
                        if (index < mListPolyLine.size() - 2)
                            handler.postDelayed(this, 1500);
                    }
                },1500);


            }catch (Exception e){
                Log.e("JSONOBJECT_ERROR", e.getMessage());
                Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }, throwable -> {
            if (throwable != null) {
                Log.e("ANIMATION_ERROR", throwable.getMessage());
                Toast.makeText(this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void buildLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // 15 secound
        mLocationRequest.setInterval(15000);
        // 10 Secound
        mLocationRequest.setFastestInterval(10000);
        // 20 Metters
        mLocationRequest.setSmallestDisplacement(20f);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Shipping order data
        setShippingOrderData();

        mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.uber_light_with_label));
            if (!success)
                Log.e("MAP_STYLE_ERROR", "Style parsing failed");

        } catch (Resources.NotFoundException ex) {
            Log.e("RESOURSE_FILE_ERROR", "Style parsing failed"+ex.getMessage());
        }

    }

    @Override
    protected void onDestroy() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        mDisposable.clear();
        super.onDestroy();
    }
}
