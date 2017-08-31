package com.zype.fire.api.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evgeny Cherkasov on 13.04.2017.
 */

public class BifrostResponse {
    @Expose
    public boolean success;

    @SerializedName("is_valid")
    @Expose
    public boolean isValid;

    @Expose
    public boolean expired;

}
