package com.amazon.android.model.event;

import android.os.Bundle;

/**
 * Created by Evgeny Cherkasov on 25.08.2017.
 *
 * Subscription products update event class for event broadcasting.
 */

public class ProductsUpdateEvent {
    /**
     * Dismiss flag.
     */
    private Bundle extras = new Bundle();

    /**
     * Constructor
     *
     * @param extras Bundle with products data.
     */
    public ProductsUpdateEvent(Bundle extras) {
        this.extras = extras;
    }

    public Bundle getExtras() {
        return extras;
    }
}
