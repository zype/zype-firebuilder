package com.amazon.android.utils.glide;

import android.content.Context;
import android.graphics.Bitmap;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gifbitmap.GifBitmapWrapper;
import com.bumptech.glide.load.resource.transcode.TranscoderRegistry;
import com.bumptech.glide.module.GlideModule;

import java.lang.reflect.Field;
import java.util.Map;

public class GlideConfiguration implements GlideModule {

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {

    }

    @Override
    public void registerComponents(Context context, Glide glide) {
        try {
            Field field = Glide.class.getDeclaredField("transcoderRegistry");
            field.setAccessible(true);

            Object object = field.get(glide);

            if (object instanceof TranscoderRegistry) {
                TranscoderRegistry transcoderRegistry = (TranscoderRegistry) object;

                //need to clear the map of the transcoderregistry
                Field factoryField = TranscoderRegistry.class.getDeclaredField("factories");
                factoryField.setAccessible(true);
                Object mapObject = factoryField.get(transcoderRegistry);

                if (mapObject instanceof Map) {
                    ((Map) mapObject).clear();
                }

                transcoderRegistry.register(GifBitmapWrapper.class, GlideDrawable.class,
                        new GifBitmapWrapperDrawableTranscoder(new GlideBitmapDrawableTranscoder(context.getResources(), glide.getBitmapPool())));

                transcoderRegistry.register(Bitmap.class, GlideBitmapDrawable.class,
                        new GlideBitmapDrawableTranscoder(context.getResources(), glide.getBitmapPool()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
