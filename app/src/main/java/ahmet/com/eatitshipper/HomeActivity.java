package ahmet.com.eatitshipper;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.squareup.picasso.Picasso;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import ahmet.com.eatitshipper.common.Common;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import io.paperdb.Paper;

public class HomeActivity extends AppCompatActivity {

    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawer;
    @BindView(R.id.nav_view)
    NavigationView mNavigationView;

    private NavController mNavController;

    private AppBarConfiguration mAppBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);
        Paper.init(this);

        updateToken();

        checkStartTrip();

        initViews();


    }

    private void checkStartTrip() {


        if (!TextUtils.isEmpty(Paper.book().read(Common.KEY_START_TRIP)))
            startActivity(new Intent(this, ShippingMapActivity.class));
    }

    private void updateToken() {

        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnFailureListener(e -> {
                    Log.e("GET_TOKEN_ERROR", e.getMessage());
                }).addOnCompleteListener(task -> {
                    Common.updateToken(this, task.getResult().getToken(),false,true);
                });
    }

    private void initViews() {
       // Paper.init(this);
//        Paper.book().write(Common.KEY_USER_LOGGED, Common.currentShipper.getPhone());
//        // Delete data offline
//        Paper.book().delete(Common.KEY_START_TRIP);
//        Paper.book().delete(Common.KEY_SHIPPING_ORDER_DATA);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setDrawerLayout(mDrawer)
                .build();
        mNavController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, mNavController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(mNavigationView, mNavController);

        View headerView = mNavigationView.getHeaderView(0);
        CircleImageView mImgShiperAvater = headerView.findViewById(R.id.img_heder_shipper_user_avater);
        TextView mTxtShiperName = headerView.findViewById(R.id.txt_header_shipper_name);
        TextView mImgShiperPhone = headerView.findViewById(R.id.txt_header_shipper_phone);

        Common.setSpanString(getString(R.string.welcome)+" ", Common.currentShipper.getName(), mTxtShiperName);
        mImgShiperPhone.setText(Common.currentShipper.getPhone());
        Picasso.get()
                .load("https://i.postimg.cc/ZqMZ3KJ5/man-character-face-avatar-in-glasses-vector-17074986.jpg")
                .into(mImgShiperAvater);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
