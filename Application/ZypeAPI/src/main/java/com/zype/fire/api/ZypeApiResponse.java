package com.zype.fire.api;

import com.zype.fire.api.Model.ErrorBody;

/**
 * Created by Evgeny Cherkasov on 11.02.2019.
 */
public class ZypeApiResponse<T> {
    public T data;
    public boolean isSuccessful;
    public ErrorBody errorBody;

    public ZypeApiResponse(T data, boolean isSuccessful) {
        this.data = data;
        this.isSuccessful = isSuccessful;
        this.errorBody = null;
    }

    public ZypeApiResponse(ErrorBody errorBody) {
        this.data = null;
        this.isSuccessful = false;
        this.errorBody = errorBody;
    }

}
