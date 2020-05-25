package ahmet.com.eatitshipper.callback;

import java.util.List;

import ahmet.com.eatitshipper.model.ShippingOrder;

public interface IShippingOrderCallbackListener {

    void onLoadShippingOrderSuccess(List<ShippingOrder> mLIstShippingOrder);
    void onLoadShippingOrderFailed(String error);

}
