/**
 * This file was modified by Amazon:
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
/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2016 Amazon Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
/**
 * Tried to be as close as LeanBack's Action class.
 */
package com.amazon.android.model;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Zype, Evgeny Cherkasov
 *
 * This class extends Action class, with additional functionality of extra fields.
 */
public class PlaylistAction extends Action {

    public static final String EXTRA_PLAYLIST_ID = "PlaylistId";

    private Map<String, Object> extras;

    /**
     * A specific constructor for creating new actions.
     *
     * @param id The id for the new action.
     * @param tag The tag for the new action.
     * @param iconResourceID The icon resource ID for the new Action.
     */
    public PlaylistAction(long id, String tag, int iconResourceID) {
        super(id, tag, iconResourceID);
    }

    /**
     * Constructor for Action class.
     */
    public PlaylistAction() {
        super();
    }

    // //////////
    // Extras
    //

    /**
     * Get the map of extra data.
     *
     * @return Extra data map.
     */
    public Map<String, Object> getExtras() {
        return extras;
    }

    /**
     * Set extra data into a map. The map will be created the first time a value is set. If the key
     * already exists, its value will be overwritten with the newly supplied value.
     *
     * @param key   Key value as string.
     * @param value Value as object.
     */
    public void setExtraValue(String key, Object value) {
        if (extras == null) {
            extras = new HashMap<>();
        }
        extras.put(key, value);
    }

    /**
     * Get extra data as a string from the internal map.
     *
     * @param key Key value as a string.
     * @return Value as a string.
     */
    public String getExtraValueAsString(String key) {
        if (extras == null || extras.get(key) == null) {
            return null;
        }
        return extras.get(key).toString();
    }
}
