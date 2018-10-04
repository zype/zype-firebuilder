# TVOD

This document outlines step-by-step instructions for setting up purchase video entitlement flow (TVOD) in your Amazon Fire TV app powered by Zype platform.

## Requirements and Prerequisites

- Amazon Developer Portal

    An [Amazon Developer Account](https://developer.amazon.com/why-amazon) will be needed.

- Android Studio

    Zype AFTV Template project configured to your Zype account

- Technical Contact

    IT or developer support strongly recommended. Completing app submission and publishing requires working with Amazon App Store, app bundles and IDE.

## Amazon Developer Portal

#### Create Your App
1. Log in to your Amazon Developer Portal and either select your app, or create one. You can follow this simple [Amazon documentation](https://developer.amazon.com/docs/fire-app-builder/amazon-in-app-purchase-component.html#createapp) if you need to create one.

#### Set up in-app products for your videos
2. On the app page select `In-App Items` tab. Then click the `Add Single IAP` button and choose `Add an Entitlement`.

3. For entitlement IAP enter product `Title` and `SKU`.
    The SKU must be the same you defined in the `marketplays_ids` field of the corresponding video in the Zype platform.

4. Repeat the process for each video you would like to allow to purchase.

## Android Studio project
5. Open your Android Studio project and switch to the Android view.

#### Turn on TVOD and Marketplace connect
6. Expand the `ZypeAPI` folder and go to `java/com.zype.fire.api(template)` folder. Then open the `ZypeSettings.java` file.

7. Set following constants as stated below::
    
    ```
    public static final boolean NATIVE_TVOD_ENABLED = true;
    public static final boolean UNIVERSAL_TVOD_ENABLED = true;
    ```
    
    _Note: In-App Purchasing Component is enabled in the app by default. You can use [this documentation](https://developer.amazon.com/docs/fire-app-builder/amazon-in-app-purchase-component.html#enableiap) if you need to verify it is enabled._
    
## Testing
Now that you’ve set up your in-app items and configured your app, it’s time to test out the integration and see how IAP interacts with your media.

Follow [this guide](https://developer.amazon.com/docs/fire-app-builder/amazon-in-app-purchase-component.html#apptester) to test in-app purchase feature in your app.
