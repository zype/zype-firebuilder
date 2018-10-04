package com.amazon.android.model.event;

import android.os.Bundle;

/**
 * Created by Evgeny Cherkasov on 29.08.2017.
 *
 * Subscription purchase event class for event broadcasting.
 */

public class PurchaseEvent {
    /**
     * Dismiss flag.
     */
    private Bundle extras = new Bundle();

    /**
     * Constructor
     *
     * @param extras Bundle with products data.
     */
    public PurchaseEvent(Bundle extras) {
        this.extras = extras;
    }

    public Bundle getExtras() {
        return extras;
    }
}
