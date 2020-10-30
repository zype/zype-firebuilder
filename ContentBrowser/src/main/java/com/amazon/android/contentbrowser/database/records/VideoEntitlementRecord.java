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
package com.amazon.android.contentbrowser.database.records;


/**
 * Zype, Evgeny Cherkasov
 *
 * This class represents a video entitlement stored in the database.
 **/
public class VideoEntitlementRecord extends Record {

    /**
     * The video id for the video favorite object.
     */
    private String videoId;

    /**
     * Created at.
     */
    private String createdAt;

    /**
     * The video favorite record constructor.
     */
    public VideoEntitlementRecord() {
    }

    /**
     * A video favorite record constructor.
     *
     * @param videoId         The video id.
     * @param createdAt
     */
    public VideoEntitlementRecord(String videoId, String createdAt) {
        this.videoId = videoId;
        this.createdAt = createdAt;
    }

    /**
     * Get the video id.
     *
     * @return The video id.
     */
    public String getVideoId() {
        return videoId;
    }

    /**
     * Set the video id.
     *
     * @param videoId The video id.
     */
    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    /**
     * Get the video favorite id.
     *
     * @return The video favorite id.
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the video favorite id.
     *
     * @param createdAt
     */
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        VideoEntitlementRecord record = (VideoEntitlementRecord) object;

        return !(getVideoId() == null || !getVideoId().equals(record.getVideoId())) &&
                !(getCreatedAt() == null || !getCreatedAt().equals(record.getCreatedAt()));
    }

    @Override
    public String toString() {
        return "VideoEntitlementRecord{" +
                "mContentId='" + videoId + '\'' +
                ", mCreatedAt=" + createdAt +
                '}';
    }
}
