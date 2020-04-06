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
package com.amazon.android.contentbrowser.database.tables;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.amazon.android.contentbrowser.database.records.Record;
import com.amazon.android.contentbrowser.database.records.VideoEntitlementRecord;
import com.amazon.android.contentbrowser.database.records.VideoFavoriteRecord;

/**
 * Zype, Evgeny Cherkasov
 *
 * This class represents the database table that holds video entitlement records.
 */
public class VideoEntitlementsTable extends Table {
    private static final String TAG = VideoEntitlementsTable.class.getSimpleName();

    /**
     * Name of table.
     */
    private static final String TABLE_NAME = "video_entitlements";

    /**
     * The video favorite object id column.
     */
    private static final String CREATED_AT = "created_at";

//    /**
//     * Time to live value for database records in seconds. (5 days)
//     */
//    static int RECORD_TTL = 432000;

    /**
     * The string used in a SQL query to create the video favorites table.
     */
    public static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_CONTENT_ID + " TEXT, " +
                    CREATED_AT + " TEXT " +
                    ")";

    /**
     * The string used in a SQL query to select all the columns.
     */
    public static final String SQL_SELECT_ALL_COLUMNS = "SELECT " +
            _ID + ", " +
            COLUMN_CONTENT_ID + ", " +
            CREATED_AT +
            " FROM " + TABLE_NAME;

    /**
     * The string used in a SQL query to drop the table.
     */
    static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public VideoEntitlementsTable() {
        super(TABLE_NAME);
    }

//    /**
//     * Find the record in the database containing the video id.
//     *
//     * @param db        The database to query.
//     * @param videoId The video id to search for.
//     * @return The row id, or -1 if the video id match was not found.
//     */
//    public long findRowIdByVideoId(SQLiteDatabase db, String videoId) {
//        String query = "SELECT " + _ID +
//                " FROM " + TABLE_NAME +
//                " WHERE " + COLUMN_CONTENT_ID + "='" + videoId + "' ";
//
//        Cursor cursor = db.rawQuery(query, null);
//
//        long rowId = -1;
//
//        if (cursor != null && cursor.moveToFirst()) {
//            rowId = cursor.getLong(0);
//        }
//
//        if (cursor != null) {
//            cursor.close();
//        }
//
//        return rowId;
//    }

//    /**
//     * Find the record in the database containing the video favorite id.
//     *
//     * @param db               The database to query.
//     * @param videoFavoriteId The video favorite id to search for.
//     * @return The row id, or -1 if the recommendation id match was not found.
//     */
//    public long findRowId(SQLiteDatabase db, long videoFavoriteId) {
//
//        return DatabaseUtil.findRowId(db, "SELECT " + _ID +
//                " FROM " + TABLE_NAME +
//                " WHERE " + VIDEO_FAVORITE_ID + "='"
//                + videoFavoriteId + "' ");
//    }

    /**
     * Writes a video entitlement record to the database.
     * It tries to find an existing row in the database based on video id. If the row
     * exists, the record is just updated with the new info. If the row does not exist, a new row
     * is inserted in the database.
     *
     * @param db             The database.
     * @param videoEntitlement The video entitlement to write.
     * @return The row of the record or -1 if there was an error.
     */
    public long write(SQLiteDatabase db, VideoEntitlementRecord videoEntitlement) {

        ContentValues contentValues = writeContentValues(videoEntitlement);

        // Check if the row exists
        long rowId = findRowId(db, videoEntitlement.getVideoId());
        if (rowId == -1) {
            rowId = db.insert(TABLE_NAME, null, contentValues);
            Log.d(TAG, "record inserted to database: " + videoEntitlement.toString());
        }
        else {
            rowId = db.update(TABLE_NAME, contentValues, _ID + "=" + rowId, null);
            Log.d(TAG, "record updated in database: " + videoEntitlement.toString());
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
    public boolean delete(SQLiteDatabase db, String videoId) {
        Log.d(TAG, "delete(): videoId=" + videoId);
        int affectedRows = db.delete(TABLE_NAME, COLUMN_CONTENT_ID + "=" + videoId + " ", null);
        return (affectedRows > 0);
    }

    /**
     * Read a video entitlement record from the database with the given video id.
     *
     * @param db        The database.
     * @param videoId The video id of the video favorite to read.
     * @return The video favorite record.
     */
    public VideoEntitlementRecord read(SQLiteDatabase db, String videoId) {

        Cursor cursor = db.rawQuery(SQL_SELECT_ALL_COLUMNS + " WHERE " + COLUMN_CONTENT_ID + "='" +
                                            videoId + "' ", null);

        return (VideoEntitlementRecord) readSingleRecord(cursor);
    }

    //
    // 'Table' abstract methods implementation
    //

    /**
     * Reads a video favorite record from a cursor. Does not close the cursor when finished.
     *
     * @param cursor The cursor containing the data to read.
     * @return The recommendation record.
     */
    @Override
    public  VideoEntitlementRecord readRecordFromCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        int column = 1; // skipping 0 since that's the row id and we don't need it right now.

        VideoEntitlementRecord record = new VideoEntitlementRecord();

        record.setVideoId(cursor.getString(column++));
        record.setCreatedAt(cursor.getString(column++));

        Log.d(TAG, "read record: " + record.toString());

        return record;
    }

    /**
     * Fills the content values with the necessary information to save the recommendation record to
     * the database.
     *
     * @param record The record.
     * @return The content values.
     */
    @Override
    public ContentValues writeContentValues(Record record) {
        VideoEntitlementRecord videoEntitlement = (VideoEntitlementRecord) record;

        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_CONTENT_ID, videoEntitlement.getVideoId());
        contentValues.put(CREATED_AT, videoEntitlement.getCreatedAt());

        return contentValues;
    }

    /**
     * Purges all expired records from the database.
     *
     * @param db The database.
     * @return True if at least one record was deleted; false otherwise.
     */
    @Override
    public boolean purge(SQLiteDatabase db) {
        return false;
    }
}
