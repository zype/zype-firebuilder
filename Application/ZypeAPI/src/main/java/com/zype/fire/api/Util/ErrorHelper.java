package com.zype.fire.api.Util;

import com.zype.fire.api.Model.ErrorBody;
import com.zype.fire.api.ZypeApi;

import java.io.IOException;
import java.lang.annotation.Annotation;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Response;

/**
 * Created by Evgeny Cherkasov on 28.11.2017.
 */

public class ErrorHelper {
    public static ErrorBody parseError(Response<?> response) {
        Converter<ResponseBody, ErrorBody> converter = ZypeApi.getInstance().retrofit()
                .responseBodyConverter(ErrorBody.class, new Annotation[0]);
        ErrorBody errorBody;

        try {
            errorBody = converter.convert(response.errorBody());
        }
        catch (IOException e) {
            return new ErrorBody();
        }

        return errorBody;
    }
}
