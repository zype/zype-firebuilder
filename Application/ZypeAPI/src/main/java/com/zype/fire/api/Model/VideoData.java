package com.zype.fire.api.Model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Evgeny Cherkasov on 25.05.2017.
 */

public class VideoData {
    @SerializedName("_id")
    @Expose
    public String Id;

    @Expose
    public boolean active;

    @Expose
    public String country;

    @SerializedName("created_at")
    @Expose
    public String createdAt;

    @Expose
    public String description;

    @SerializedName("discovery_url")
    @Expose
    public String discoveryUrl;

    @Expose
    public int duration;

    @Expose
    public Integer episode;

    @SerializedName("expire_at")
    @Expose
    public String expireAt;

    @Expose
    public boolean featured;

    @SerializedName("foreign_id")
    @Expose
    public String foreignId;

    @Expose
    public List<Image> images = new ArrayList<>();

    @Expose
    public List<String> keywords = new ArrayList<>();

    @SerializedName("marketplace_ids")
    @Expose
    public MarketplaceIds marketplaceIds;

    @SerializedName("on_air")
    @Expose
    public boolean onAir;

    @SerializedName("published_at")
    @Expose
    public String publishedAt;

    @Expose
    public int rating;

    @SerializedName("related_playlist_ids")
    @Expose
    public List<String> relatedPlaylistIds = new ArrayList<>();

    @SerializedName("request_count")
    @Expose
    public int requestCount;

    @Expose
    public String season;

    @SerializedName("series_id")
    @Expose
    public String seriesId;

    @SerializedName("short_description")
    @Expose
    public String shortDescription;

    @SerializedName("site_id")
    @Expose
    public String siteId;

    @SerializedName("start_at")
    @Expose
    public String startAt;

    @Expose
    public String status;

    @Expose
    public String title;

    @Expose
    public boolean transcoded;

    @SerializedName("updated_at")
    @Expose
    public String updatedAt;

    @SerializedName("zobject_ids")
    @Expose
    public List<String> zobjectIds = new ArrayList<>();

    @Expose
    public List<Thumbnail> thumbnails = new ArrayList<>();

    @SerializedName("hulu_id")
    @Expose
    public String huluId;

    @SerializedName("youtube_id")
    @Expose
    public String youtubeId;

    @SerializedName("crunchyroll_id")
    @Expose
    public String crunchyrollId;

    @SerializedName("vimeo_id")
    @Expose
    public String vimeoId;

    @SerializedName("subscription_required")
    @Expose
    public boolean subscriptionRequired;

    @SerializedName("pass_required")
    @Expose
    public boolean passRequired;

    @SerializedName("purchase_price")
    @Expose
    public float purchasePrice;

    @SerializedName("purchase_required")
    @Expose
    public boolean purchaseRequired;

    @SerializedName("rental_duration")
    @Expose
    public int rentalDuration;

    @SerializedName("rental_price")
    @Expose
    public float rentalPrice;

    @SerializedName("rental_required")
    @Expose
    public boolean rentalRequired;

    @SerializedName("mature_content")
    @Expose
    public boolean matureContent;

    @SerializedName("registration_required")
    @Expose
    public boolean registrationRequired;

    public String playlistId;
    public String playerUrl;
    public String videoFavoriteId;

    @SerializedName("preview_ids")
    @Expose
    public List<String> previewIds = new ArrayList<>();
}
