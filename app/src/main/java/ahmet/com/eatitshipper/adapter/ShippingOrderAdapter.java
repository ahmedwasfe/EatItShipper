package ahmet.com.eatitshipper.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.List;

import ahmet.com.eatitshipper.common.Common;
import ahmet.com.eatitshipper.model.ShippingOrder;
import ahmet.com.eatitshipper.R;
import ahmet.com.eatitshipper.ShippingMapActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.paperdb.Paper;


public class ShippingOrderAdapter extends RecyclerView.Adapter<ShippingOrderAdapter.ShippingOrderHolder> {

    private Context mContext;
    private List<ShippingOrder> mListShippingOrder;
    private LayoutInflater inflater;

    private SimpleDateFormat dateFormat;

    public ShippingOrderAdapter(Context mContext, List<ShippingOrder> mListShippingOrder) {
        this.mContext = mContext;
        this.mListShippingOrder = mListShippingOrder;

        inflater = LayoutInflater.from(mContext);

        dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a");

        Paper.init(mContext);
    }

    @NonNull
    @Override
    public ShippingOrderHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = inflater.inflate(R.layout.raw_shipping_order, parent, false);
        return new ShippingOrderHolder(layoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull ShippingOrderHolder holder, int position) {

        Picasso.get()
                .load(mListShippingOrder.get(position).getOrder().getCarts().get(0).getFoodImage())
                .into(holder.mImgFood);

        holder.mTxtShippingOrderDate.setText(new StringBuilder(dateFormat.format(
                mListShippingOrder.get(position).getOrder()
                        .getDate())));

        Common.setSpanStringColor(mContext.getString(R.string.number)+" ",
                mListShippingOrder.get(position).getOrder().getKey(),
                holder.mTxtShippingOrderNumber,
                mContext.getColor(android.R.color.holo_blue_dark));

        Common.setSpanStringColor(mContext.getString(R.string.address)+" ",
                mListShippingOrder.get(position).getOrder().getShippingAddress(),
                holder.mTxtShippingOrderAddress,
                mContext.getColor(R.color.colorDarkRed));

        Common.setSpanStringColor(mContext.getString(R.string.payment)+" ",
                mListShippingOrder.get(position).getOrder().getTransactionId(),
                holder.mTxtShippingOrderPayment,
                mContext.getColor(R.color.colorDarkRed));

        // Disable button if already start trip
        if (mListShippingOrder.get(position).isStartTrip())
            holder.mBtnShipNow.setEnabled(false);

        // Event
        holder.mBtnShipNow.setOnClickListener(view -> {
            Paper.book().write(Common.KEY_SHIPPING_ORDER_DATA, new Gson()
                                .toJson(mListShippingOrder.get(position)));
            mContext.startActivity(new Intent(mContext, ShippingMapActivity.class));
        });

    }

    @Override
    public int getItemCount() {
        return mListShippingOrder.size();
    }

    class ShippingOrderHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.txt_shipping_order_food_date)
        TextView mTxtShippingOrderDate;
        @BindView(R.id.txt_shipping_order_food_number)
        TextView mTxtShippingOrderNumber;
        @BindView(R.id.txt_shipping_order_food_address)
        TextView mTxtShippingOrderAddress;
        @BindView(R.id.txt_shipping_order_payment)
        TextView mTxtShippingOrderPayment;
        @BindView(R.id.img_shipping_order_food)
        ImageView mImgFood;
        @BindView(R.id.btn_ship_now)
        MaterialButton mBtnShipNow;

        public ShippingOrderHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}