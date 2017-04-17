package com.zype.fire.auth;

import com.amazon.android.module.IImplCreator;

/**
 * Created by Evgeny Cherkasov on 11.04.2017.
 */

public class ZypeAuthenticationImplCreator implements IImplCreator {
    /**
     * Creates an implementation which is defined by interface I.
     *
     * @return Interface of the implementation.
     */
    @Override
    public Object createImpl() {
        return new ZypeAuthentication();
    }
}
