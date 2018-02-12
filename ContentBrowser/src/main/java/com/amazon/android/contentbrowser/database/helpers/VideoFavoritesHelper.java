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
package com.amazon.android.contentbrowser.database.helpers;

import com.amazon.android.contentbrowser.database.records.VideoFavoriteRecord;
import com.amazon.android.contentbrowser.database.tables.VideoFavoritesTable;
import com.amazon.utils.StringManipulation;

import android.content.Context;
import android.util.Log;

import java.util.List;

/**
 * A helper class for the use of the content database. This helper class contains
 * CRUD methods for records in the VideoFavoriteTable.
 */
public class VideoFavoritesHelper extends DatabaseHelper {

    /**
     * Debug tag.
     */
    private static final String TAG = VideoFavoritesHelper.class.getSimpleName();

    /**
     * The Content Database instance.
     */
    private static VideoFavoritesHelper sInstance;

    /**
     * Get the content database helper instance.
     *
     * @return The helper instance.
     */
    public static VideoFavoritesHelper getInstance() {

        if (sInstance == null) {
            synchronized (VideoFavoritesHelper.class) {
                if (sInstance == null) {
                    sInstance = new VideoFavoritesHelper();
                }
            }
        }
        return sInstance;
    }

    /**
     * Construct a database that has {@link com.amazon.android.model.content.Content} related
     * tables: one for saving playback states; one for saving recommendations.
     */
    private VideoFavoritesHelper() {
        super(new VideoFavoritesTable());
    }

    /**
     * Get all video favorites records.
     *
     * @return List of video favorites records.
     */
    public List<VideoFavoriteRecord> getVideoFavorites(Context context) {
        return (List<VideoFavoriteRecord>) getTable().readMultipleRecords(getDatabase(context), VideoFavoritesTable.SQL_SELECT_ALL_COLUMNS);
    }

    /**
     * Store or update a video favorite in the database. If an existing entry is found for
     * the given video id, the record is updated with the new information.
     *
     * @param videoId        The video id.
     * @param videoFavoriteId The video favorite id.
     * @return True if a record was entered or updated in the database; false otherwise.
     */
    public boolean addVideoFavorite(Context context, String videoId, String videoFavoriteId) {
        if (StringManipulation.isNullOrEmpty(videoId)) {
            Log.e(TAG, "addVideoFavorite(): videoId can not be empty");
            return false;
        }
        return writeRecord(context, new VideoFavoriteRecord(videoId, videoFavoriteId));
    }

}


