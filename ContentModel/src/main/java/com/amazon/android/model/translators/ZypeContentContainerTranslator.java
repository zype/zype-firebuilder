/**
 * Copyright 2015-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazon.android.model.translators;

import android.util.Log;

import com.amazon.android.model.AModelTranslator;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.recipe.Recipe;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* Zype, Evgeny Cherkasov */

/**
 * This class extends the {@link AModelTranslator} for the {@link ContentContainer} class. It
 * provides a way to translate a {link Map} to a {@link ContentContainer} object by using a
 * {@link Recipe} object.
 */
public class ZypeContentContainerTranslator extends AModelTranslator<ContentContainer> {

    private static final String TAG = ZypeContentContainerTranslator.class.getSimpleName();

    /**
     * {@inheritDoc}
     *
     * @return A new {@link ContentContainer}
     */
    @Override
    public ContentContainer instantiateModel() {

        return new ContentContainer();
    }

    /**
     * Explicitly sets a member variable named field to the given value. If the field does not
     * match one of {@link ContentContainer}'s predefined field names, the field and value will be
     * stored in the {@link ContentContainer#mExtras} map.
     *
     * @param model The {@link ContentContainer} to set the field on.
     * @param field The {@link String} describing what member variable to set.
     * @param value The {@link Object} value to set the member variable.
     * @return True if the value was set, false if there was an error.
     */
    @Override
    public boolean setMemberVariable(ContentContainer model, String field, Object value) {

        if (model == null || field == null || field.isEmpty() || value == null) {
            Log.e(TAG, "Input parameters should not be null and field cannot be empty.");
            return false;
        }

        try {
            if (field.equals(ContentContainer.NAME_FIELD_NAME)) {
                model.setName((String) value);
            }
            else if (field.equals("thumbnails")) {
                model.setExtraValue(Content.CARD_IMAGE_URL_FIELD_NAME, findImageUrl(Content.CARD_IMAGE_URL_FIELD_NAME, value));
                model.setExtraValue(Content.BACKGROUND_IMAGE_URL_FIELD_NAME, findImageUrl(Content.BACKGROUND_IMAGE_URL_FIELD_NAME, value));
            }
            else {
                model.setExtraValue(field, value);
            }
        }
        catch (ClassCastException e) {
            Log.e(TAG, "Error casting value to the required type for field " + field, e);
            return false;
        }
        return true;
    }

    /**
     * This method verifies that the {@link ContentContainer} model was properly translated and all
     * the mandatory fields were set. A valid {@link ContentContainer} model must have a non-empty
     * value for {@link ContentContainer#mName}.
     *
     * @param model The {@link ContentContainer} model to verify.
     * @return True if the model is valid; false otherwise.
     */
    @Override
    public boolean validateModel(ContentContainer model) {

        return (model != null && model.getName() != null && !model.getName().isEmpty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {

        return ZypeContentContainerTranslator.class.getSimpleName();
    }

    private String findImageUrl(String field, Object value) {
        final int IMAGE_HEIGHT_CARD = 452;
        final int IMAGE_HEIGHT_BACKGROUND = 1080;

        String result = "null";
        int requiredImageHeight = 0;
        switch (field) {
            case Content.CARD_IMAGE_URL_FIELD_NAME: {
                requiredImageHeight = IMAGE_HEIGHT_CARD;
                break;
            }
            case Content.BACKGROUND_IMAGE_URL_FIELD_NAME: {
                requiredImageHeight = IMAGE_HEIGHT_BACKGROUND;
                break;
            }

        }
        try {
            JSONArray jsonValue = new JSONArray(value.toString());
            JSONObject jsonImage = null;
            for (int i = 0; i < jsonValue.length(); i++) {
                if (jsonImage == null) {
                    jsonImage = jsonValue.getJSONObject(i);
                }
                else {
                    JSONObject jsonObject = jsonValue.getJSONObject(i);
                    if (Math.abs(jsonObject.getInt("height") - requiredImageHeight) < Math.abs(jsonImage.getInt("height") - requiredImageHeight)) {
                        jsonImage = jsonObject;
                    }
                }
            }
            if (jsonImage != null) {
                result = jsonImage.getString("url");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}