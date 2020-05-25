package ahmet.com.eatitshipper.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import ahmet.com.eatitshipper.callback.IShippingOrderCallbackListener;
import ahmet.com.eatitshipper.common.Common;
import ahmet.com.eatitshipper.model.ShippingOrder;

public class HomeViewModel extends ViewModel implements IShippingOrderCallbackListener {

    private MutableLiveData<List<ShippingOrder>> mutableLShippingOrderLIst;
    private MutableLiveData<String> mutableLMessageError;

    private IShippingOrderCallbackListener shippingOrderCallbackListener;

    public HomeViewModel() {

        if (mutableLShippingOrderLIst == null){
            mutableLShippingOrderLIst = new MutableLiveData<>();
            mutableLMessageError = new MutableLiveData<>();
        }

        shippingOrderCallbackListener = this;
    }


    public MutableLiveData<String> getMutableLMessageError() {
        return mutableLMessageError;
    }

    public MutableLiveData<List<ShippingOrder>> getMutableLShippingOrderLIst(String shipperPhone) {
        loadOrdersByShipper(shipperPhone);
        return mutableLShippingOrderLIst;
    }

    private void loadOrdersByShipper(String shipperPhone) {

        Query queryShippingOrder = FirebaseDatabase.getInstance()
                .getReference(Common.KEY_SHIPPING_ORDER_REFERANCE)
                .orderByChild("shipperPhone")
                .equalTo(shipperPhone);

        queryShippingOrder.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                List<ShippingOrder> mListShippingOrder = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    ShippingOrder shippingOrder = snapshot.getValue(ShippingOrder.class);
                    shippingOrder.setKey(snapshot.getKey());
                    mListShippingOrder.add(shippingOrder);
                }

                shippingOrderCallbackListener.onLoadShippingOrderSuccess(mListShippingOrder);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                shippingOrderCallbackListener.onLoadShippingOrderFailed(databaseError.getMessage());
            }
        });
    }

    @Override
    public void onLoadShippingOrderSuccess(List<ShippingOrder> mLIstShippingOrder) {
        mutableLShippingOrderLIst.setValue(mLIstShippingOrder);
    }

    @Override
    public void onLoadShippingOrderFailed(String error) {
        mutableLMessageError.setValue(error);
    }
}