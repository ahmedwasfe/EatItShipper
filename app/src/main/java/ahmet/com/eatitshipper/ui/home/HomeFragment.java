package ahmet.com.eatitshipper.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ahmet.com.eatitshipper.adapter.ShippingOrderAdapter;
import ahmet.com.eatitshipper.common.Common;
import ahmet.com.eatitshipper.R;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.supercharge.shimmerlayout.ShimmerLayout;

public class HomeFragment extends Fragment {

    @BindView(R.id.recycler_shipping_order)
    RecyclerView mRecyclerShippingOrder;
    @BindView(R.id.shimmer_layout_shipping_order)
    ShimmerLayout mShimmerLayout;

    private HomeViewModel homeViewModel;

    private ShippingOrderAdapter shippingOrderAdapter;

    private LayoutAnimationController mAnimationController;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = ViewModelProviders.of(this).get(HomeViewModel.class);
        View layoutView = inflater.inflate(R.layout.fragment_home, container, false);

        ButterKnife.bind(this, layoutView);

        homeViewModel.getMutableLMessageError()
                .observe(getActivity(), error -> {
                    Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
                    Log.e("LOAD_SHIP_ORDER_ERROR", error);
                });

        homeViewModel.getMutableLShippingOrderLIst(Common.currentShipper.getPhone())
                .observe(this, mLIstShippingOrder -> {

                    shippingOrderAdapter = new ShippingOrderAdapter(getActivity(), mLIstShippingOrder);
                    mRecyclerShippingOrder.setAdapter(shippingOrderAdapter);
                    mRecyclerShippingOrder.setLayoutAnimation(mAnimationController);

                    mShimmerLayout.stopShimmerAnimation();
                    mShimmerLayout.setVisibility(View.GONE);
                });

        initViews();

        return layoutView;
    }

    private void initViews() {

        mShimmerLayout.stopShimmerAnimation();

        mAnimationController = AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.raw_item_from_left);

        mRecyclerShippingOrder.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        mRecyclerShippingOrder.setLayoutManager(layoutManager);
        mRecyclerShippingOrder.addItemDecoration(new DividerItemDecoration(getActivity(), layoutManager.getOrientation()));
    }
}
