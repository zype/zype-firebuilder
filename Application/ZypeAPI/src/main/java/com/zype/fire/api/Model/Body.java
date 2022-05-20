package com.zype.fire.api.Model;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evgeny Cherkasov on 14.04.2017.
 */

public class Body {
    @Expose
    public Advertising advertising;

    @Expose
    public Analytics analytics;

    @Expose
    public List<File> files = new ArrayList<>();

    @Expose
    public List<SubTitle> subtitles = new ArrayList<>();
}
