package ahmet.com.eatitshipper.services;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

import ahmet.com.eatitshipper.common.Common;

public class FCMServices extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {

        Map<String, String> dataRecv = remoteMessage.getData();

        if (dataRecv != null)
            Common.showNotification(this, new Random().nextInt(),
                    dataRecv.get(Common.KEY_NOTFI_TITLE),
                    dataRecv.get(Common.KEY_NOTFI_CONTENT),
                    null);

    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // we are in shipper app so shipper = true
        Common.updateToken(this, token,false, true);
    }
}
