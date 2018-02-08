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
package com.amazon.android.contentbrowser.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.util.Log;

import com.amazon.utils.DateAndTimeHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Zype, Evgeny Cherkasov
 *
 * This class represents the database table that holds video favorites records. It contains columns
 * for the content id (video is) and the video favorite object id.
 * There should be no duplicate recommendations in the database.
 * Each content id and video favorite id should be unique.
 */
class VideoFavoritesTable implements BaseColumns {
    private static final String TAG = VideoFavoritesTable.class.getSimpleName();

    /**
     * Name of table.
     */
    private static final String TABLE_NAME = "VideoFavorites";

    /**
     * The video id column.
     */
    private static final String VIDEO_ID = "VideoId";

    /**
     * The video favorite object id column.
     */
    private static final String VIDEO_FAVORITE_ID = "VideoFavoriteId";

//    /**
//     * Time to live value for database records in seconds. (5 days)
//     */
//    static int RECORD_TTL = 432000;

    /**
     * The string used in a SQL query to create the video favorites table.
     */
    static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY, " +
                    VIDEO_ID + " TEXT, " +
                    VIDEO_FAVORITE_ID + " TEXT " +
                    ")";

    /**
     * The string used to select the video favorite id column.
     */
    private static final String SQL_SELECT_VIDEO_FAVORITE_ID_COLUMN = "SELECT " +
            VIDEO_FAVORITE_ID +
            " FROM " + TABLE_NAME;

    /**
     * The string used in a SQL query to select all the columns.
     */
    public static final String SQL_SELECT_ALL_COLUMNS = "SELECT " +
            _ID + ", " +
            VIDEO_ID + ", " +
            VIDEO_FAVORITE_ID +
            " FROM " + TABLE_NAME;

    /**
     * The string used in a SQL query to drop the table.
     */
    static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    /**
     * Find the record in the database containing the video id.
     *
     * @param db        The database to query.
     * @param videoId The video id to search for.
     * @return The row id, or -1 if the video id match was not found.
     */
    static long findRowId(SQLiteDatabase db, String videoId) {

        return DatabaseUtil.findRowId(db, "SELECT " + _ID +
                " FROM " + TABLE_NAME +
                " WHERE " + VIDEO_ID + "='" + videoId + "' ");
    }

    /**
     * Find the record in the database containing the video favorite id.
     *
     * @param db               The database to query.
     * @param videoFavoriteId The video favorite id to search for.
     * @return The row id, or -1 if the recommendation id match was not found.
     */
    static long findRowId(SQLiteDatabase db, long videoFavoriteId) {

        return DatabaseUtil.findRowId(db, "SELECT " + _ID +
                " FROM " + TABLE_NAME +
                " WHERE " + VIDEO_FAVORITE_ID + "='"
                + videoFavoriteId + "' ");
    }

    /**
     * Writes a video favorite record to the database.
     * It tries to find an existing row in the database based on video id. If the row
     * exists, the record is just updated with the new info. If the row does not exist, a new row
     * is inserted in the database.
     *
     * @param db             The database.
     * @param videoFavorite The video favorite to write.
     * @return The row of the record or -1 if there was an error.
     */
    static long write(SQLiteDatabase db, VideoFavoriteRecord videoFavorite) {

        ContentValues contentValues = new ContentValues();

        contentValues.put(VIDEO_ID, videoFavorite.getVideoId());
        contentValues.put(VIDEO_FAVORITE_ID, videoFavorite.getVideoFavoriteId());

        // Check if the row exists
        long rowId = findRowId(db, videoFavorite.getVideoId());
        if (rowId == -1) {
            rowId = db.insert(TABLE_NAME, null, contentValues);
            Log.d(TAG, "record inserted to database: " + videoFavorite.toString());
        }
        else {
            rowId = db.update(TABLE_NAME, contentValues, _ID + "=" + rowId, null);
            Log.d(TAG, "record updated in database: " + videoFavorite.toString());
        }

        return rowId;
    }

    /**
     * Delete the record by video id.
     *
     * @param db        The database.
     * @param videoId The video id of the record to delete.
     * @return True if a row was deleted; false otherwise.
     */
    static boolean delete(SQLiteDatabase db, String videoId) {

        Log.d(TAG, "deleting video favorite with video id " + videoId);
        return DatabaseUtil.deleteByContentId(TABLE_NAME, VIDEO_ID, db, videoId);
    }

//    /**
//     * Delete the record by video favorite id.
//     *
//     * @param db               The database.
//     * @param videoFavoriteId The video favorite object id of the record to delete.
//     * @return True if a row was deleted; false otherwise.
//     */
//    static boolean delete(SQLiteDatabase db, String videoFavoriteId) {
//
//        int affectedRows = db.delete(TABLE_NAME, VIDEO_FAVORITE_ID + "=" +
//                videoFavoriteId + " ", null);
//        Log.d(TAG, "deleting video favorite with id " + videoFavoriteId);
//        return (affectedRows > 0);
//    }

    /**
     * Read a video favorite record from the database with the given video id.
     *
     * @param db        The database.
     * @param videoId The video id of the video favorite to read.
     * @return The video favorite record.
     */
    static VideoFavoriteRecord read(SQLiteDatabase db, String videoId) {

        Cursor cursor = db.rawQuery(SQL_SELECT_ALL_COLUMNS + " WHERE " + VIDEO_ID + "='" +
                                            videoId + "' ", null);

        return readVideoFavoriteRecord(cursor);
    }

//    /**
//     * Read a recommendation record from the database with the given recommendation id.
//     *
//     * @param db               The database.
//     * @param recommendationId The recommendation id of the recommendation to read.
//     * @return The recommendation record.
//     */
//    static RecommendationRecord read(SQLiteDatabase db, long recommendationId) {
//
//        Cursor cursor = db.rawQuery(SQL_SELECT_ALL_COLUMNS + " WHERE " + COLUMN_RECOMMENDATION_ID
//                                            + "=" + recommendationId + " ", null);
//
//        return readRecommendationRecord(cursor);
//    }

    /**
     * Reads video favorite records from the database that are returned as a result of the query.
     *
     * @param db    The database.
     * @param query The query
     * @return List of video favorite records.
     */
    static List<VideoFavoriteRecord> readMultipleRecords(SQLiteDatabase db, String query) {

        List<VideoFavoriteRecord> records = new ArrayList<>();

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {

            do {
                VideoFavoriteRecord record = readVideoFavoriteRecordFromCursor(cursor);
                if (record != null) {
                    records.add(record);
                }
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }

        return records;
    }

    /**
     * Reads a video favorite record from a cursor, closes the cursor once finished.
     *
     * @param cursor The cursor containing the data to read.
     * @return The recommendation record.
     */
    private static VideoFavoriteRecord readVideoFavoriteRecord(Cursor cursor) {

        VideoFavoriteRecord record = null;

        if (cursor != null && cursor.moveToFirst()) {

            record = readVideoFavoriteRecordFromCursor(cursor);

        }
        if (cursor != null) {
            cursor.close();
        }
        return record;

    }

    /**
     * Reads a video favorite record from a cursor. Does not close the cursor when finished.
     *
     * @param cursor The cursor containing the data to read.
     * @return The recommendation record.
     */
    private static VideoFavoriteRecord readVideoFavoriteRecordFromCursor(Cursor cursor) {

        if (cursor == null) {
            return null;
        }

        int column = 1; // skipping 0 since that's the row id and we don't need it right now.

        VideoFavoriteRecord record = new VideoFavoriteRecord();

        record.setVideoId(cursor.getString(column++));
        record.setVideoFavoriteId(cursor.getString(column++));

        Log.d(TAG, "read record: " + record.toString());

        return record;
    }

//    /**
//     * Get a list of recommendation ids from the database.
//     *
//     * @param db The database.
//     * @return A list of recommendation ids.
//     */
//    static List<Integer> getRecommendationIds(SQLiteDatabase db) {
//
//        List<Integer> ids = new ArrayList<>();
//
//        Cursor cursor = db.rawQuery(SQL_SELECT_REC_ID_COLUMN, null);
//
//        if (cursor != null && cursor.moveToFirst()) {
//
//            do {
//                ids.add(cursor.getInt(0));
//
//            } while (cursor.moveToNext());
//        }
//
//        if (cursor != null) {
//            cursor.close();
//        }
//
//        return ids;
//    }

    /**
     * Delete all records from the table.
     *
     * @param db The database.
     */
    static void deleteAll(SQLiteDatabase db) {

        db.delete(TABLE_NAME, null, null);
    }

    /**
     * Get the count of video favorite records in the database.
     *
     * @param db The database.
     * @return The number of recommendation records.
     */
    static int getVideoFavoritesCount(SQLiteDatabase db) {

        return DatabaseUtil.getCount(db, TABLE_NAME);

    }

//    /**
//     * Get the list of expired recommendation records from the database.
//     *
//     * @param db          The database.
//     * @param currentTime The time to use to calculate if the recommendation is expired.
//     * @return The list of expired recommendation records.
//     */
//    static List<RecommendationRecord> getExpiredRecommendations(SQLiteDatabase db,
//                                                                long currentTime) {
//
//        return readMultipleRecords(db, getSqlSelectExpiredQuery(currentTime));
//    }

//    /**
//     * Purges all expired recommendation records from the database.
//     *
//     * @param db The database.
//     * @return True if at least one record was delete; false otherwise.
//     */
//    static boolean purge(SQLiteDatabase db) {
//
//        long currentTime = DateAndTimeHelper.getCurrentDate().getTime();
//
//        List<RecommendationRecord> records = getExpiredRecommendations(db, currentTime);
//
//        return records.size() > 0 && DatabaseUtil.deleteExpired(db, TABLE_NAME,
//                                                                COLUMN_EXPIRATION, currentTime);
//    }


}
