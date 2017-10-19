# Native Subscription

This document outlines step-by-step instructions for setting up native subscription for your Amazon Fire TV app powered by Zype's Endpoint API service and app production software and SDK template.

## Requirements and Prerequisites

#### Technical Contact
IT or developer support strongly recommended. Completing app submission and publishing requires working with Amazon App Store, app bundles and IDE.

#### Amazon Developer Portal
An [Amazon Developer Account](https://developer.amazon.com/why-amazon) will be needed.

## Amazon Developer Portal

#### Create Your App
1. Log in to your Amazon Developer Portal and either select your app, or create one. You can follow this simple [Amazon documentation](https://developer.amazon.com/docs/fire-app-builder/amazon-in-app-purchase-component.html#createapp) if you need to create one.

#### Subscription Options
2. After you hit create, look towards the top and go to `In-App Items`. Then click the `Add Subscription` button.

    <a href="https://drive.google.com/file/d/0B_Ab_j5EmMA4cDJaMk82MWk2aFE/view?usp=sharing"><img src="https://drive.google.com/file/d/0B_Ab_j5EmMA4cDJaMk82MWk2aFE/view?usp=sharing" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

3. Create subscription item following [this guide](https://developer.amazon.com/docs/fire-app-builder/amazon-in-app-purchase-component.html#creategopremiumitem). Take a note of subscription item SKU and subscription period SKU. You will need them further.

    <a href="https://drive.google.com/file/d/0B_Ab_j5EmMA4V1hkempUY1BXcFE/view?usp=sharing"><img src="https://drive.google.com/file/d/0B_Ab_j5EmMA4V1hkempUY1BXcFE/view?usp=sharing" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

    <a href="https://drive.google.com/file/d/0B_Ab_j5EmMA4S2pYLXZtdE9WTFU/view?usp=sharing"><img src="https://drive.google.com/file/d/0B_Ab_j5EmMA4S2pYLXZtdE9WTFU/view?usp=sharing" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

4. Repeat the process for any other subscription options you would like to offer - yearly, weekly, etc.

    _Note: Each subscription item should have only one subscription period. Fore example, if you have monthly and annually subscription options you have to add one subscription item with single monthly period and another subscription item with annually period._

## Android Studio project
5. Open your Android Studio project and switch to the Android view.

#### Turn on Native subscription feature
6. Expand the `ZypeAPI` folder and go to `java/com.zype.fire.api(template)` folder. Then open the `ZypeSettings.java` file.

7. Set one of the following constants (which is applicable to your use case) to `true`:
    
    ```
    public static final boolean NATIVE_SUBSCRIPTION_ENABLED = true;
    public static final boolean NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED = false;
    ```
    
    _Note: In-App Purchasing Component is enabled in the app by default. You can use [this documentation](https://developer.amazon.com/docs/fire-app-builder/amazon-in-app-purchase-component.html#enableiap) if you need to verify it is enabled._
    
#### Map In-App items
8. Expand the `PurchaseInterface` folder and go to `assets`. Then open the `skuslist.json` file.

    <a href="https://drive.google.com/file/d/0B_Ab_j5EmMA4TUNjM29xbHJTVzg/view?usp=sharing"><img src="https://drive.google.com/file/d/0B_Ab_j5EmMA4TUNjM29xbHJTVzg/view?usp=sharing" style="width: 500px; max-width: 100%; height: auto" title="Click for the larger version." /></a>

9. In the `skulist.json` file add item to `skuList` array for each your subscription option. Set `sku` attribute value to your subscription item's SKU and set `purchaseSku` attribute value to your subscription period SKU.
 
    Your `skuList` array should look like this:
    
    ```{
      "skusList": [
        {
          "sku": "com.zype.aftv.demo.month",
          "productType": "SUBSCRIBE",
          "purchaseSku": "com.zype.aftv.demo.month.month",
          "id": "aftvmonthly"
        },
        {
          "sku": "com.zype.aftv.demo.year",
          "productType": "SUBSCRIBE",
          "purchaseSku": "com.zype.aftv.demo.year.year",
          "id": "aftvyearly"
        },
        {
          "sku": "RentUnPurchased",
          "productType": "RENT",
          "purchaseSku": "RentUnPurchased"
        }
      ],
      "actions": {
        "CONTENT_ACTION_DAILY_PASS": "RentUnPurchased",
        "CONTENT_ACTION_SUBSCRIPTION": "com.zype.aftv.demo.month"
      }
    }
    ```

## Testing
Now that you’ve set up your in-app items and configured your app, it’s time to test out the integration and see how IAP interacts with your media.

Follow [this guide](https://developer.amazon.com/docs/fire-app-builder/amazon-in-app-purchase-component.html#apptester) to test native subscription feature in your app.
