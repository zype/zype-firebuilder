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
package com.amazon.android.contentbrowser;

import com.amazon.android.contentbrowser.database.ContentDatabaseHelper;
import com.amazon.android.contentbrowser.database.RecentRecord;
import com.amazon.android.contentbrowser.helper.AnalyticsHelper;
import com.amazon.android.contentbrowser.helper.AuthHelper;
import com.amazon.android.contentbrowser.helper.ErrorHelper;
import com.amazon.android.contentbrowser.helper.LauncherIntegrationManager;
import com.amazon.android.contentbrowser.helper.PurchaseHelper;
import com.amazon.android.contentbrowser.recommendations.RecommendationManager;
import com.amazon.android.interfaces.ICancellableLoad;
import com.amazon.android.interfaces.IContentBrowser;
import com.amazon.android.model.Action;
import com.amazon.android.model.content.Content;
import com.amazon.android.model.content.ContentContainer;
import com.amazon.android.model.content.constants.ExtraKeys;
import com.amazon.android.model.content.constants.PreferencesConstants;
import com.amazon.android.model.event.ActionUpdateEvent;
import com.amazon.android.navigator.Navigator;
import com.amazon.android.navigator.NavigatorModel;
import com.amazon.android.navigator.UINode;
import com.amazon.android.recipe.Recipe;
import com.amazon.android.search.ISearchAlgo;
import com.amazon.android.search.ISearchResult;
import com.amazon.android.search.SearchManager;
import com.amazon.android.ui.fragments.AlertDialogFragment;
import com.amazon.android.ui.fragments.LogoutSettingsFragment;
import com.amazon.android.ui.fragments.NoticeSettingsFragment;
import com.amazon.android.ui.fragments.SlideShowSettingFragment;
import com.amazon.android.utils.ErrorUtils;
import com.amazon.android.utils.Preferences;
import com.amazon.utils.DateAndTimeHelper;
import com.amazon.utils.StringManipulation;

import org.greenrobot.eventbus.EventBus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.amazon.android.contentbrowser.helper.LauncherIntegrationManager
        .getSourceOfContentPlayRequest;

/* Zype */
import com.zype.fire.api.ZypeSettings;
import com.zype.fire.auth.ZypeAuthentication;

/**
 * This class is the controller of the content browsing solution.
 */
public class ContentBrowser implements IContentBrowser, ICancellableLoad {

    /**
     * Debug TAG.
     */
    private static final String TAG = ContentBrowser.class.getSimpleName();

    /**
     * Debug recipe chain flag.
     */
    private static final boolean DEBUG_RECIPE_CHAIN = false;

    /**
     * Cause a feed error flag for debugging.
     */
    private static final boolean CAUSE_A_FEED_ERROR_FOR_DEBUGGING = false;

    /**
     * Request from launcher boolean key
     */
    private static final String REQUEST_FROM_LAUNCHER = "REQUEST_FROM_LAUNCHER";

    /**
     * Content will update key.
     */
    public static final String CONTENT_WILL_UPDATE = "CONTENT_WILL_UPDATE";

    /**
     * The splash screen name.
     */
    public static final String CONTENT_SPLASH_SCREEN = "CONTENT_SPLASH_SCREEN";

    /**
     * The login screen name.
     */
    public static final String CONTENT_LOGIN_SCREEN = "CONTENT_LOGIN_SCREEN";

    /**
     * The home screen name.
     */
    public static final String CONTENT_HOME_SCREEN = "CONTENT_HOME_SCREEN";

    /**
     * The search screen name.
     */
    public static final String CONTENT_SEARCH_SCREEN = "CONTENT_SEARCH_SCREEN";

    /**
     * The details screen name.
     */
    public static final String CONTENT_DETAILS_SCREEN = "CONTENT_DETAILS_SCREEN";

    /**
     * The submenu screen name.
     */
    public static final String CONTENT_SUBMENU_SCREEN = "CONTENT_SUBMENU_SCREEN";

    /**
     * The recommended content screen name.
     */
    public static final String CONTENT_RECOMMENDED_SCREEN = "CONTENT_RECOMMENDED_SCREEN";

    /**
     * The content renderer screen name.
     */
    public static final String CONTENT_RENDERER_SCREEN = "CONTENT_RENDERER_SCREEN";

    /**
     * The connectivity screen name.
     */
    public static final String CONTENT_CONNECTIVITY_SCREEN = "CONTENT_CONNECTIVITY_SCREEN";

    /**
     * The slide show screen name.
     */
    public static final String CONTENT_SLIDESHOW_SCREEN = "CONTENT_SLIDESHOW_SCREEN";

    /* Zype, Evgeny Cherkasov */
    public static final String SUBSCRIPTION_SCREEN = "SUBSCRIPTION_SCREEN";

    /**
     * Search constant.
     */
    public static final String SEARCH = "Search";

    /**
     * Slide show constant.
     */
    public static final String SLIDE_SHOW = "SlideShow";

    /**
     * Login constant.
     */
    public static final String LOGIN_LOGOUT = "LoginLogout";

    /**
     * Terms constant.
     */
    public static final String TERMS = "Terms";

    /**
     * Slide show setting constant.
     */
    public static final String SLIDESHOW_SETTING = "SlideShowSetting";

    /* Zype, Evgeny Cherkasov */
    public static final String MY_LIBRARY = "MyLibrary";
    public static final String MY_LIBRARY_NEXT_PAGE = "MyLibraryNextPage";

    /**
     * Constant for the "watch now" action.
     */
    public static final int CONTENT_ACTION_WATCH_NOW = 1;

    /**
     * Constant for the "watch from beginning" action.
     */
    public static final int CONTENT_ACTION_WATCH_FROM_BEGINNING = 2;

    /**
     * Constant for the "resume playback" action.
     */
    public static final int CONTENT_ACTION_RESUME = 3;

    /**
     * Constant for the "watch later" action.
     */
    public static final int CONTENT_ACTION_WATCH_LATER = 4;

    /**
     * Constant for the "purchase subscription" action.
     */
    public static final int CONTENT_ACTION_SUBSCRIPTION = 5;

    /**
     * Constant for the "purchase daily pass" action.
     */
    public static final int CONTENT_ACTION_DAILY_PASS = 6;

    /**
     * Constant for the "trial" action.
     */
    public static final int CONTENT_ACTION_TRIAL = 7;

    /**
     * Constant for the "buy" action.
     */
    public static final int CONTENT_ACTION_BUY = 8;

    /**
     * Constant for the "rent" action.
     */
    public static final int CONTENT_ACTION_RENT = 9;

    /**
     * Constant for the "search" action.
     */
    public static final int CONTENT_ACTION_SEARCH = 10;

    /**
     * Constant for the "login" action.
     */
    public static final int CONTENT_ACTION_LOGIN_LOGOUT = 11;

    /**
     * Constant for the "slide show" action.
     */
    public static final int CONTENT_ACTION_SLIDESHOW = 12;

    /**
     * Constant for the "launcher" action.
     */
    public static final int CONTENT_ACTION_CALL_FROM_LAUNCHER = 13;

    /**
     * The maximum number of actions supported.
     */
    public static final int CONTENT_ACTION_MAX = 100;

    /* Zype, Evgeny Cherkasov */
    // Choose plan action
    public static final int CONTENT_ACTION_CHOOSE_PLAN = 50;
    // Watch ad free action
    public static final int CONTENT_ACTION_SWAF = 55;

    /**
     * Search algorithm name.
     */
    private static final String DEFAULT_SEARCH_ALGO_NAME = "basic";

    /**
     * Content reload timeout in seconds.
     */
    private static final int CONTENT_RELOAD_TIMEOUT = 14400; // Equals 4 hours.

    /**
     * Constant for grace time in milliseconds
     */
    public static final long GRACE_TIME_MS = 5000; // 5 seconds.

    /**
     * Application context.
     */
    private final Context mAppContext;

    /**
     * Singleton instance.
     */
    private static ContentBrowser sInstance;

    /**
     * Lock object for singleton get instance.
     */
    private static final Object sLock = new Object();

    /**
     * Event bus reference.
     */
    private final EventBus mEventBus = EventBus.getDefault();

    /**
     * Search manager instance.
     */
    private final SearchManager<ContentContainer, Content> mSearchManager = new SearchManager<>();

    /**
     * Custom search handler reference.
     */
    private ICustomSearchHandler mICustomSearchHandler;

    /**
     * Root content container listener.
     */
    private IRootContentContainerListener mIRootContentContainerListener;

    /**
     * Last selected content.
     */
    private Content mLastSelectedContent;

    /**
     * Last selected content container.
     */
    private ContentContainer mLastSelectedContentContainer;

    /**
     * Navigator instance.
     */
    private final Navigator mNavigator;

    /**
     * Actions list.
     */
    private final List<Action> mWidgetActionsList = new ArrayList<>();

    /**
     * Global content action list.
     */
    private final List<Action> mGlobalContentActionList = new ArrayList<>();

    /**
     * Content action listener.
     */
    private final Map<Integer, List<IContentActionListener>> mContentActionListeners = new
            HashMap<>();

    /**
     * Settings actions list.
     */
    private final List<Action> mSettingsActions = new ArrayList<>();

    /**
     * Powered by logo map.
     */
    private final Map<String, String> mPoweredByLogoUrlMap = new HashMap<>();

    /**
     * Auth helper instance.
     */
    private AuthHelper mAuthHelper;

    /**
     * Purchase helper instance.
     */
    private PurchaseHelper mPurchaseHelper;

    /**
     * LauncherIntegrationManager instance.
     */
    private LauncherIntegrationManager mLauncherIntegrationManager;

    /**
     * Flag for whether or not the user is subscribed.
     */
    private boolean mSubscribed = false;

    /**
     * Flag for whether or not in-app purchasing is disabled.
     */
    private boolean mIAPDisabled = false;

    /**
     * When set to true, this flag will override the subscription flag for all the content.
     */
    private boolean mOverrideAllContentsSubscriptionFlag = false;

    /**
     * Composite subscription instance; single use only!!!.
     */
    private CompositeSubscription mCompositeSubscription = new CompositeSubscription();

    /**
     * boolean to read launcher integration status.
     */
    private final boolean mLauncherIntegrationEnabled;

    /**
     * loginLogout action, content browser needs to keep the state updated of this action.
     */
    private Action mLoginAction;

    /**
     * Content loader instance.
     */
    private ContentLoader mContentLoader;

    /**
     * Recommendation manager instance.
     */
    private RecommendationManager mRecommendationManager;

    /* Zype, Evgeny Cherkasov */
    private boolean userLoggedIn = false;

    /**
     * Returns AuthHelper instance.
     *
     * @return authHelper instance
     */
    public AuthHelper getAuthHelper() {

        return mAuthHelper;
    }

    /**
     * Returns if the loading request is cancelled or not.
     * For this class it will never be cancelled.
     *
     * @return True if loading is cancelled
     */
    public boolean isLoadingCancelled() {

        return false;
    }

    /**
     * Sets up the login action. The login action is only added to the settings row if a screen
     * requires verification to access as noted in the Navigator.json configuration file.
     */
    private void setupLogoutAction() {

        mLoginAction = createLogoutButtonSettingsAction();

        /* Zype, Evgeny Cherkasov */
//        if (Navigator.isScreenAccessVerificationRequired(mNavigator.getNavigatorModel())) {
//            addSettingsAction(mLoginAction);
//        }
        updateLoginAction();
    }

    /* Zype, Evgeny Cherkasov */
    private void updateLoginAction() {
        if (mLoginAction != null) {
            for (Action action : mSettingsActions) {
                if (action.getId() == mLoginAction.getId()) {
                    mSettingsActions.remove(action);
                    break;
                }
            }
            if (Navigator.isScreenAccessVerificationRequired(mNavigator.getNavigatorModel())
                    && (ZypeSettings.UNIVERSAL_SUBSCRIPTION_ENABLED || ZypeSettings.UNIVERSAL_TVOD
                        || userLoggedIn)) {
                addSettingsAction(mLoginAction);
            }
        }
    }

    private void setupMyLibraryAction() {
        if (ZypeSettings.UNIVERSAL_TVOD) {
            addSettingsAction(createMyLibrarySettingsAction());
        }
    }

    /**
     * Content action listener interface.
     */
    public interface IContentActionListener {

        /**
         * Called when an action happens on a content.
         *
         * @param activity Activity.
         * @param content  Content.
         * @param actionId Action id.
         */
        void onContentAction(Activity activity, Content content, int actionId);
    }

    /**
     * Custom search handler interface.
     */
    public interface ICustomSearchHandler {

        /**
         * On search requested callback.
         *
         * @param query         Query string.
         * @param iSearchResult Search result listener.
         */
        void onSearchRequested(String query, ISearchResult iSearchResult);
    }

    /**
     * Root content container listener.
     */
    public interface IRootContentContainerListener {

        /**
         * Root content container populated callback.
         *
         * @param contentContainer Root content container reference.
         */
        void onRootContentContainerPopulated(ContentContainer contentContainer);
    }

    /**
     * Screen switch listener interface.
     */
    public interface IScreenSwitchListener {

        /**
         * On screen switch callback.
         *
         * @param extra Extra bundle.
         */
        void onScreenSwitch(Bundle extra);
    }

    /**
     * Constructor.
     *
     * @param activity The activity that is active when ContentBrowser is created.
     */
    private ContentBrowser(Activity activity) {

        mAppContext = activity.getApplicationContext();
        mNavigator = new Navigator(activity);
        /* Zype, Evgeny Cherkasov */
//        mSubscribed = Preferences.getBoolean(PurchaseHelper.CONFIG_PURCHASE_VERIFIED);
        updateUserSubscribed();

        mContentLoader = ContentLoader.getInstance(mAppContext);

        mIAPDisabled = mAppContext.getResources().getBoolean(R.bool.is_iap_disabled);
        /* Zype, Evgeny Cherkasov */
        if (!ZypeSettings.NATIVE_SUBSCRIPTION_ENABLED && !ZypeSettings.NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED) {
            mIAPDisabled = true;
        }

        mLauncherIntegrationEnabled =
                mAppContext.getResources().getBoolean(R.bool.is_launcher_integration_enabled);

        mOverrideAllContentsSubscriptionFlag =
                mAppContext.getResources()
                           .getBoolean(R.bool.override_all_contents_subscription_flag);

        addWidgetsAction(createSearchAction());
        //addWidgetsAction(createSlideShowAction());
        addSettingsAction(createTermsOfUseSettingsAction());
        //addSettingsAction(createSlideShowSettingAction());
        setupLogoutAction();
        setupMyLibraryAction();

        mSearchManager.addSearchAlgo(DEFAULT_SEARCH_ALGO_NAME, new ISearchAlgo<Content>() {
            @Override
            public boolean onCompare(String query, Content content) {

                return content.searchInFields(query, new String[]{
                        Content.TITLE_FIELD_NAME,
                        Content.DESCRIPTION_FIELD_NAME
                });
            }
        });
        /* Zype, Evgeny Cherkasov */
        setCustomSearchHandler(new ZypeSearchManager(Recipe.newInstance(mAppContext, "recipes/ZypeSearchContentsRecipe.json")));

        mNavigator.setINavigationListener(new Navigator.INavigationListener() {

            @Override
            public void onSetTheme(Activity activity) {

            }

            @Override
            public void onScreenCreate(Activity activity, String screenName) {

                Log.d(TAG, " onScreenCreate for screen " + screenName + " activity " + activity +
                        " intent " + (activity != null ? activity.getIntent() : null));
                if (!mContentLoader.isContentLoaded() && (screenName == null
                        || !screenName.equals(CONTENT_SPLASH_SCREEN))) {
                    mNavigator.startActivity(CONTENT_SPLASH_SCREEN, intent -> {
                        // Make sure we clear activity stack.
                        updateIntentToClearActivityStack(intent);
                    });
                    if (activity != null) {
                        activity.finish();
                    }
                    mContentLoader.setContentReloadRequired(false);
                    mContentLoader.setContentLoaded(false);
                    Log.e(TAG, "Immature app, switching to splash");
                    runGlobalRecipes(activity, ContentBrowser.this);
                }
                else {
                    if (screenName != null) {
//                        if (screenName.equals(CONTENT_HOME_SCREEN)) {
//                            loadContentForSubcontainers(activity, ContentBrowser.this);
//                        }
                        if (screenName.equals(CONTENT_SUBMENU_SCREEN)) {
//                            loadContentForSubcontainers(activity, ContentBrowser.this, getLastSelectedContentContainer());
                            runGlobalRecipesForLastSelected(activity, ContentBrowser.this);
                        }
                    }
                }
            }

            @Override
            public void onScreenGotFocus(Activity activity, String screenName) {

                Log.d(TAG, " onScreenGotFocus for screen " + screenName + " activity " + activity +
                        " intent " + (activity != null ? activity.getIntent() : null));

                if (screenName.equals(CONTENT_HOME_SCREEN)) {
                    if (mContentLoader.isContentReloadRequired() ||
                            !mContentLoader.isContentLoaded()) {
                        mNavigator.startActivity(CONTENT_SPLASH_SCREEN, intent -> {
                            intent.putExtra(CONTENT_WILL_UPDATE, true);
                            // Make sure we clear activity stack.
                            updateIntentToClearActivityStack(intent);
                        });
                        if (activity != null) {
                            activity.finish();
                        }
                        mContentLoader.setContentReloadRequired(false);
                        mContentLoader.setContentLoaded(false);
                        Log.d(TAG, "Content reload required, switching to splash");
                        runGlobalRecipes(activity, ContentBrowser.this);
                    }
                    else if (activity != null && activity.getIntent().hasExtra
                            (REQUEST_FROM_LAUNCHER) &&
                            activity.getIntent().getBooleanExtra(REQUEST_FROM_LAUNCHER, false)) {
                        activity.getIntent().putExtra(REQUEST_FROM_LAUNCHER, false);
                        switchToRendererScreen(activity.getIntent());
                    }
                }
                else if (screenName.equals(CONTENT_SPLASH_SCREEN)) {
                    Log.d(TAG, "runGlobalRecipes due to CONTENT_SPLASH_SCREEN focus");
                    //runGlobalRecipes(activity, ContentBrowser.this);
                }
            }

            @Override
            public void onScreenLostFocus(Activity activity, String screenName) {

                Log.d(TAG, "onScreenLostFocus:" + screenName);
                if (mAuthHelper != null) {
                    mAuthHelper.cancelAllRequests();
                }
            }

            @Override
            public void onApplicationGoesToBackground() {

                Log.d(TAG, "onApplicationGoesToBackground:");
                if (mCompositeSubscription.hasSubscriptions()) {
                    Log.d(TAG, "mCompositeSubscription.unsubscribe");
                    mCompositeSubscription.unsubscribe();
                    // CompositeSubscription is a single use, create a new one for next round.
                    mCompositeSubscription = null;
                    mCompositeSubscription = new CompositeSubscription();
                }
                else {
                    Log.d(TAG, "onApplicationGoesToBackground has no subscriptions!!!");
                }
            }
        });
    }

    /**
     * Listener method to listen for authentication updates, it sets the status of
     * loginLogoutAction action used by BrowseActivity
     *
     * @param authenticationStatusUpdateEvent Event for update in authentication status.
     */
    public void onAuthenticationStatusUpdateEvent(AuthHelper.AuthenticationStatusUpdateEvent
                                                          authenticationStatusUpdateEvent) {

        if (mLoginAction != null) {
            mLoginAction.setState(authenticationStatusUpdateEvent.isUserAuthenticated() ?
                                          LogoutSettingsFragment.TYPE_LOGOUT :
                                          LogoutSettingsFragment.TYPE_LOGIN);
        }
        /* Zype, Evgeny Cherkasov */
        // Update user logged in and subscription flags
        // TODO: Consider other way to get subscription count preference to avoid dependency of ZypeAuthComponent
        userLoggedIn = authenticationStatusUpdateEvent.isUserAuthenticated();
        updateUserSubscribed();

        updateLoginAction();
    }

    /**
     * Get instance, singleton method.
     *
     * @param activity The activity.
     * @return Content browser singleton instance.
     */
    public static ContentBrowser getInstance(Activity activity) {

        synchronized (sLock) {
            if (sInstance == null) {
                sInstance = new ContentBrowser(activity);
            }
            return sInstance;
        }
    }

    /**
     * Gets called after all modules are loaded.
     */
    public void onAllModulesLoaded() {

        mAuthHelper = new AuthHelper(mAppContext, this);
        mAuthHelper.setupMvpdList();

        mPurchaseHelper = new PurchaseHelper(mAppContext, this);
        // Launcher integration requires the content authorization system initialized before this
        // manager is initialized. Otherwise it will incorrectly set that authorization is not
        // required for content playing. Hence initializing this after initializing AuthHelper
        // and PurchaseHelper.
        if (mLauncherIntegrationEnabled) {
            mLauncherIntegrationManager = new LauncherIntegrationManager(mAppContext, this);
        }

        mRecommendationManager = new RecommendationManager(mAppContext);
        // First reading of the database upon app launch. Created off of main thread.
        mRecommendationManager.cleanDatabase();
    }

    /**
     * Get navigator.
     *
     * @return Navigator.
     */
    public Navigator getNavigator() {

        return mNavigator;
    }

    /**
     * Set root content container listener.
     *
     * @param listener Root content container listener.
     * @return Content browser instance.
     */
    public ContentBrowser setRootContentContainerListener(IRootContentContainerListener listener) {

        mIRootContentContainerListener = listener;
        return this;
    }

    /**
     * Set custom search handler.
     *
     * @param customSearchHandler Custom search handler.
     * @return Content browser instance.
     */
    public ContentBrowser setCustomSearchHandler(ICustomSearchHandler customSearchHandler) {

        mICustomSearchHandler = customSearchHandler;
        return this;
    }

    /**
     * Add action to global action list.
     *
     * @param action Action.
     * @return Content browser instance.
     */
    public ContentBrowser addActionToGlobalContentActionList(Action action) {

        mGlobalContentActionList.add(action);
        return this;
    }

    /**
     * Add content action listener.
     *
     * @param actionId               Action id.
     * @param iContentActionListener Content action listener.
     */
    public void addContentActionListener(int actionId, IContentActionListener
            iContentActionListener) {

        List<IContentActionListener> iContentActionListenersList =
                mContentActionListeners.get(actionId);

        if (iContentActionListenersList == null) {
            iContentActionListenersList = new ArrayList<>();
            mContentActionListeners.put(actionId, iContentActionListenersList);
        }

        iContentActionListenersList.add(iContentActionListener);
    }

    /**
     * Get root content container.
     *
     * @return Root content container.
     */
    public ContentContainer getRootContentContainer() {

        return mContentLoader.getRootContentContainer();
    }

    /**
     * Set last selected content.
     *
     * @param content Content.
     * @return Content browser instance.
     */
    public ContentBrowser setLastSelectedContent(Content content) {

        mLastSelectedContent = content;
        return this;
    }

    /**
     * Get last selected content.
     *
     * @return Last selected content.
     */
    public Content getLastSelectedContent() {

        return mLastSelectedContent;
    }

    /**
     * Set last selected content container.
     *
     * @param contentContainer Last selected content container.
     * @return Content browser instance.
     */
    public ContentBrowser setLastSelectedContentContainer(ContentContainer contentContainer) {

        mLastSelectedContentContainer = contentContainer;
        return this;
    }

    /**
     * Get last selected content container.
     *
     * @return Last selected content container.
     */
    public ContentContainer getLastSelectedContentContainer() {

        return mLastSelectedContentContainer;
    }

    /**
     * Get the path for the light font.
     *
     * @return Light font path.
     */
    public String getLightFontPath() {

        return mNavigator.getNavigatorModel().getBranding().lightFont;
    }

    /**
     * Get the path for the bold font.
     *
     * @return Bold font path.
     */
    public String getBoldFontPath() {

        return mNavigator.getNavigatorModel().getBranding().boldFont;
    }

    /**
     * Get the path for the regular font.
     *
     * @return Regular font path.
     */
    public String getRegularFontPath() {

        return mNavigator.getNavigatorModel().getBranding().regularFont;
    }

    /**
     * Get the flag for showing related content.
     *
     * @return True if related content should be shown; false otherwise.
     */
    public boolean isShowRelatedContent() {

        return mNavigator.getNavigatorModel().getConfig().showRelatedContent;
    }

    /**
     * Get the flag for showing content from the same category if similar tags resulted in no
     * related content.
     *
     * @return True if the category's content should be used as default related content; false
     * otherwise.
     */
    public boolean isUseCategoryAsDefaultRelatedContent() {

        return mNavigator.getNavigatorModel().getConfig().useCategoryAsDefaultRelatedContent;
    }

    /**
     * Get powered by logo url by name.
     *
     * @param name Powered by logo name.
     * @return Powered by logo url.
     */
    public String getPoweredByLogoUrlByName(String name) {

        return mPoweredByLogoUrlMap.get(name);
    }

    /**
     * Add powered by logo url.
     *
     * @param name Powered by logo name.
     * @param url  Powered by logo ur.
     */
    public void addPoweredByLogoUrlByName(String name, String url) {

        mPoweredByLogoUrlMap.put(name, url);
    }

    /**
     * Get settings actions.
     *
     * @return List of settings actions.
     */
    public List<Action> getSettingsActions() {

        return mSettingsActions;
    }

    /**
     * Add settings action.
     *
     * @param settingsAction Settings action.
     */
    private void addSettingsAction(Action settingsAction) {

        mSettingsActions.add(settingsAction);
    }

    /**
     * Get terms of use {@link Action}.
     *
     * @return terms of use action.
     */
    private Action createTermsOfUseSettingsAction() {
        // Create the Terms of Use settings action.
        return new Action().setAction(TERMS).setIconResourceId(R.drawable.ic_terms_text)
                           .setLabel1(mAppContext.getString(R.string.terms_title));
    }

    /**
     * Create loginLogoutAction Action with initial state set as login.
     *
     * @return loginLogoutAction action.
     */
    private Action createLogoutButtonSettingsAction() {
        // Create the logout button settings action.
        return new Action().setAction(LOGIN_LOGOUT)
                           .setId(ContentBrowser.CONTENT_ACTION_LOGIN_LOGOUT)
                           .setLabel1(LogoutSettingsFragment.TYPE_LOGOUT,
                                      mAppContext.getString(R.string.logout_label))
                           .setIconResourceId(LogoutSettingsFragment.TYPE_LOGOUT, R
                                   .drawable.ic_login_logout)
                           .setLabel1(LogoutSettingsFragment.TYPE_LOGIN,
                                      mAppContext.getString(R.string.login_label))
                           .setIconResourceId(LogoutSettingsFragment.TYPE_LOGIN, R
                                   .drawable.ic_login_logout)
                           .setState(LogoutSettingsFragment.TYPE_LOGIN);
    }

    /**
     * Create My Library Action.
     *
     * @return action.
     */
    private Action createMyLibrarySettingsAction() {
        return new Action().setAction(MY_LIBRARY)
                // TODO: Change action icon
                .setIconResourceId(R.drawable.ic_video_library_white_48dp)
                .setLabel1(mAppContext.getString(R.string.my_library_label));
    }

    /**
     * Add action to widget action list.
     *
     * @param action The action to add.
     */
    public void addWidgetsAction(Action action) {

        mWidgetActionsList.add(action);
    }

    /**
     * Creates search action.
     *
     * @return The search action.
     */
    private Action createSearchAction() {

        Action search = new Action(CONTENT_ACTION_SEARCH, SEARCH, R.drawable
                .lb_ic_in_app_search);
        search.setId(ContentBrowser.CONTENT_ACTION_SEARCH);
        search.setAction(SEARCH);
        return search;
    }

    /**
     * Creates slide show action.
     *
     * @return The slide show action.
     */
    private Action createSlideShowAction() {

        Action slideShow = new Action(CONTENT_ACTION_SLIDESHOW, SLIDE_SHOW, R.drawable.lb_ic_play);
        slideShow.setId(ContentBrowser.CONTENT_ACTION_SLIDESHOW);
        slideShow.setAction(SLIDE_SHOW);
        return slideShow;
    }

    /**
     * Create slide show setting action.
     *
     * @return The slide show setting action.
     */
    private Action createSlideShowSettingAction() {

        return new Action().setAction(SLIDESHOW_SETTING)
                           .setLabel1(mAppContext.getString(R.string.slideshow_title))
                           .setIconResourceId(R.drawable.ic_terms_text);
    }

    /**
     * This method returns the list of actions that are being used.
     *
     * @return A list of actions used for the action widget adapter.
     */
    public ArrayList<Action> getWidgetActionsList() {

        return (ArrayList<Action>) mWidgetActionsList;
    }

    /**
     * Get recommended list of a content as content container.
     * If there are no items with similar tags, this method returns items from the same category.
     *
     * TODO: DEVTECH-2635
     *
     * @param content Content.
     * @return Recommended contents as a content container.
     */
    public ContentContainer getRecommendedListOfAContentAsAContainer(Content content) {

        ContentContainer recommendedContentContainer =
                new ContentContainer(mAppContext.getString(R.string.recommended_contents_header));

        for (Content c : mContentLoader.getRootContentContainer()) {
            if (content.hasSimilarTags(c) && !StringManipulation.areStringsEqual(c.getId(), content
                    .getId())) {
                recommendedContentContainer.addContent(c);
            }
        }

        // Use items from the same category as recommended contents
        // if there are no contents with similar tags and the config setting is set to true.
        if (recommendedContentContainer.getContents().isEmpty() &&
                isUseCategoryAsDefaultRelatedContent()) {

            ContentContainer parentContainer = getContainerForContent(content);

            if (parentContainer != null) {

//                for (Content relatedContent : parentContainer.getContents()) {
//                    if (!StringManipulation.areStringsEqual(content.getId(), relatedContent.getId
//                            ())) {
//                        recommendedContentContainer.addContent(relatedContent);
                if (!content.isSubscriptionRequired()) {
                    /* Zype, Evgeny Cherkasov */
                    // Check is user logged in and has subscription.
                    if (userLoggedIn) {
                        // User is logged in and has subscription. Add all videos
                        if (isUserSubscribed()) {
                            for (Content relatedContent : parentContainer.getContents()) {
                                if (!StringManipulation.areStringsEqual(content.getId(), relatedContent.getId())) {
                                    recommendedContentContainer.addContent(relatedContent);
                                }
                            }
                        }
                        else {
                            // User is logged in but has no subscription. Add onlu not subscription videos
                            for (Content relatedContent : parentContainer.getContents()) {
                                if (!StringManipulation.areStringsEqual(content.getId(), relatedContent.getId()) && !relatedContent.isSubscriptionRequired()) {
                                    recommendedContentContainer.addContent(relatedContent);
                                }
                            }
                        }
                    }
                    else {
                        // User id not logged in. Add only not subscription videos
                        for (Content relatedContent : parentContainer.getContents()) {
                            if (!StringManipulation.areStringsEqual(content.getId(), relatedContent.getId()) && !relatedContent.isSubscriptionRequired()) {
                                recommendedContentContainer.addContent(relatedContent);
                            }
                        }
                    }
                }
                else {
                    // If current video is on subscription it mean we already checked user credentials
                    // and can add all content from the category
                    for (Content relatedContent : parentContainer.getContents()) {
                        if (!StringManipulation.areStringsEqual(content.getId(), relatedContent.getId())) {
                            recommendedContentContainer.addContent(relatedContent);
                        }
                    }
                }

            }
            else {
                Log.w(TAG, "The content's container could not be found! " + content.toString());
            }
        }
        return recommendedContentContainer;
    }

    /**
     * Get the parent content container for the given content.
     *
     * @param content The content to use to find the container.
     * @return The content container of the given content.
     */
    private ContentContainer getContainerForContent(Content content) {

        // Container that contains the current content
        ContentContainer parentContainer = null;

        // Stack of all content containers from root container.
        Stack<ContentContainer> contentContainerStack = new Stack<>();

        contentContainerStack.push(mContentLoader.getRootContentContainer());

        while (!contentContainerStack.isEmpty()) {
            // Get a sub container.
            ContentContainer contentContainer = contentContainerStack.pop();

            /* Zype, Evgeny Cherkasov */
            // The video may be included in multiple playlists. So we should check the video
            // playlistId rather than videoId
//            for (Content c : contentContainer.getContents()) {
//                if (StringManipulation.areStringsEqual(c.getId(), content.getId())) {
//                    parentContainer = contentContainer;
//                }
//            }
            String playlistId = (String) contentContainer.getExtraStringValue("keyDataType");
            if (playlistId != null && playlistId.equals(content.getExtraValue("playlistId"))) {
                parentContainer = contentContainer;
                break;
            }

            // Add all the sub containers.
            if (contentContainer.hasSubContainers()) {
                for (ContentContainer cc : contentContainer.getContentContainers()) {
                    contentContainerStack.push(cc);
                }
            }
        }
        return parentContainer;
    }

//    /* Zype, Evgeny Cherkasov */
//    public ContentContainer getContainerForContentContainer(ContentContainer contentContainer) {
//
//        // Container that contains the current content container
//        ContentContainer parentContainer = null;
//
//        // Stack of all content containers from root container.
//        Stack<ContentContainer> contentContainerStack = new Stack<>();
//
//        contentContainerStack.push(mContentLoader.getRootContentContainer());
//
//        while (!contentContainerStack.isEmpty()) {
//            // Get a sub container.
//            ContentContainer subContainer = contentContainerStack.pop();
//
//            for (ContentContainer cc : subContainer.getContentContainers()) {
//
//                if (cc.getName().equals(contentContainer.getName())) {
//                    parentContainer = subContainer;
//                }
//            }
//
//            if (parentContainer != null) {
//                break;
//            }
//
//            // Add all the sub containers.
//            if (subContainer.hasSubContainers()) {
//                for (ContentContainer cc : subContainer.getContentContainers()) {
//                    contentContainerStack.push(cc);
//                }
//            }
//        }
//        return parentContainer;
//    }

    /**
     * Search content.
     *
     * @param query         Query string.
     * @param iSearchResult Search result listener.
     */
    public void search(String query, ISearchResult iSearchResult) {

        if (mICustomSearchHandler != null) {
            mICustomSearchHandler.onSearchRequested(query, iSearchResult);
        }
        else {
            mSearchManager.syncSearch(DEFAULT_SEARCH_ALGO_NAME,
                                      query,
                                      iSearchResult,
                                      mContentLoader.getRootContentContainer());
        }
    }

    /**
     * Setting action selected.
     *
     * @param activity       Activity.
     * @param settingsAction Selected setting action.
     */
    public void settingsActionTriggered(Activity activity, Action settingsAction) {

        Log.d(TAG, "settingsActionTriggered Selected");

        switch (settingsAction.getAction()) {
            case LOGIN_LOGOUT:
                loginLogoutActionTriggered(activity, settingsAction);
                break;
            case TERMS:
                new NoticeSettingsFragment()
                        .createFragment(activity,
                                        activity.getFragmentManager(),
                                        settingsAction);
                break;
            case SLIDESHOW_SETTING:
                slideShowSettingActionTriggered(activity, settingsAction);
                break;
            /* Zype, Evgeny Cherkasov */
            case MY_LIBRARY:
                myLibraryActionTriggered(activity);
                break;
            default:
                Log.e(TAG, "Unsupported action " + settingsAction);
                break;
        }
    }

    /**
     * Method to trigger the SlideShowSettingFragment on clicking slideshowSetting Action.
     *
     * @param activity       The activity on which fragment needs to be added.
     * @param settingsAction The action instance.
     */
    private void slideShowSettingActionTriggered(Activity activity, Action settingsAction) {

        new SlideShowSettingFragment().createFragment(activity,
                                                      activity.getFragmentManager(),
                                                      settingsAction);
    }

    /**
     * Method to trigger the LogoutSettingsFragment on clicking loginLogout Action.
     *
     * @param activity       The activity on which fragment needs to be added.
     * @param settingsAction The action instance.
     */
    private void loginLogoutActionTriggered(Activity activity, Action settingsAction) {

        mAuthHelper
                .isAuthenticated()
                .subscribe(isAuthenticatedResultBundle -> {
                    if (isAuthenticatedResultBundle.getBoolean(AuthHelper.RESULT)) {
                        settingsAction.setState(LogoutSettingsFragment.TYPE_LOGOUT);
                        new LogoutSettingsFragment()
                                .createFragment(activity,
                                                activity.getFragmentManager(),
                                                settingsAction);
                    }
                    else {
                        settingsAction.setState(LogoutSettingsFragment.TYPE_LOGIN);
                        mAuthHelper.authenticateWithActivity().subscribe(resultBundle -> {
                            if (resultBundle != null &&
                                    !resultBundle.getBoolean(AuthHelper.RESULT)) {
                                getNavigator().runOnUpcomingActivity(() -> mAuthHelper
                                        .handleErrorBundle(resultBundle));
                            }
                        });
                    }
                });
    }

    /* Zype, Evgeny Cherkasov */
    private void myLibraryActionTriggered(Activity activity) {
        ContentContainer contentContainer = getRootContentContainer()
                .findContentContainerByName(ZypeSettings.ROOT_MY_LIBRARY_PLAYLIST_ID);
        if (contentContainer != null) {
            // Set next page to 1 for initial loading
            contentContainer.setExtraValue(ExtraKeys.NEXT_PAGE, 1);
            // If user is logged in then open browsing screen for My Library. Otherwise switch to
            // login screen.
            mAuthHelper.isAuthenticated()
                    .subscribe(isAuthenticatedResultBundle -> {
                        boolean result = isAuthenticatedResultBundle.getBoolean(AuthHelper.RESULT);
                        if (result) {
                            ContentLoader.ILoadContentForContentContainer listener = new ContentLoader.ILoadContentForContentContainer() {
                                @Override
                                public void onContentsLoaded() {
//                                    setLastSelectedContentContainer(contentContainer);
//                                    switchToScreen(ContentBrowser.CONTENT_SUBMENU_SCREEN);
                                }
                            };
                            // TODO: Add mCompositeSubscription parameter from ContentBrowser
                            mContentLoader.loadContentForMyLibraryContentContainer(contentContainer, activity, listener);

                            setLastSelectedContentContainer(contentContainer);
                            switchToScreen(ContentBrowser.CONTENT_SUBMENU_SCREEN);
                        }
                        else {
                            mAuthHelper.handleAuthChain(extra -> mNavigator.startActivity(CONTENT_HOME_SCREEN, intent -> {
                            }));
                        }
                    });
        }
    }

    /**
     * Get content action list.
     *
     * @param content Content.
     * @return List of action for provided content.
     */
    public List<Action> getContentActionList(Content content) {

        List<Action> contentActionList = new ArrayList<>();

        boolean isSubscriptionNotRequired = !content.isSubscriptionRequired();
        if (isSubscriptionNotRequired && mOverrideAllContentsSubscriptionFlag) {
            isSubscriptionNotRequired = false;
        }

        /* Zype, Evgeny Cherkasov */
//        if (mSubscribed || isSubscriptionNotRequired || mIAPDisabled) {
        if (isUserSubscribed() || isSubscriptionNotRequired || mIAPDisabled) {


            // Check if the content is meant for live watching. Live content requires only a
            // watch now button.
            boolean liveContent = content.getExtraValue(Recipe.LIVE_FEED_TAG) != null &&
                    Boolean.valueOf(content.getExtraValue(Recipe.LIVE_FEED_TAG).toString());

            ContentDatabaseHelper database = ContentDatabaseHelper.getInstance(mAppContext);

            // Check database for stored playback position of content.
            if (!liveContent && database != null && database.recentRecordExists(content.getId())) {
                RecentRecord record = database.getRecent(content.getId());

                // Add "Resume" button if content playback is not complete.
                if (record != null && !record.isPlaybackComplete()) {
                    contentActionList.add(
                            new Action().setId(CONTENT_ACTION_RESUME)
                                        .setLabel1(mAppContext.getResources()
                                                              .getString(R.string.resume_1))
                                        .setLabel2(mAppContext.getResources()
                                                              .getString(R.string.resume_2)));
                }
                // Add "Watch From Beginning" button to start content over.
                contentActionList.add(
                        new Action().setId(CONTENT_ACTION_WATCH_FROM_BEGINNING)
                                    .setLabel1(mAppContext.getResources()
                                                          .getString(
                                                                  R.string.watch_from_beginning_1))
                                    .setLabel2(mAppContext.getResources()
                                                          .getString(
                                                                  R.string.watch_from_beginning_2)));
                /* Zype, Evgeny Cherkasov */
                if (ZypeSettings.SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED
                        && !ZypeSettings.NATIVE_SUBSCRIPTION_ENABLED && !userLoggedIn) {
                    contentActionList.add(new Action().setId(CONTENT_ACTION_SWAF)
                            .setLabel1(mAppContext.getResources().getString(R.string.action_swaf_1))
                            .setLabel2(mAppContext.getResources().getString(R.string.action_swaf_2)));
                }
            }


            else {
                contentActionList.add(new Action().setId(CONTENT_ACTION_WATCH_NOW)
                        .setLabel1(mAppContext.getResources().getString(R.string.watch_now_1))
                        .setLabel2(mAppContext.getResources().getString(R.string.watch_now_2)));
                /* Zype, Evgeny Cherkasov */
                if (ZypeSettings.SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED
                        && !ZypeSettings.NATIVE_SUBSCRIPTION_ENABLED && !userLoggedIn) {
                    contentActionList.add(new Action().setId(CONTENT_ACTION_SWAF)
                            .setLabel1(mAppContext.getResources().getString(R.string.action_swaf_1))
                            .setLabel2(mAppContext.getResources().getString(R.string.action_swaf_2)));
                }
            }
        }
        else {
            /* Zype, Evgeny Cherkasov */
//            contentActionList.add(new Action()
//                                          .setId(CONTENT_ACTION_SUBSCRIPTION)
//                                          .setLabel1(mAppContext.getResources()
//                                                                .getString(R.string.premium_1))
//                                          .setLabel2(mAppContext.getResources()
//                                                                .getString(R.string.premium_2)));
//
//            contentActionList.add(new Action()
//                                          .setId(CONTENT_ACTION_DAILY_PASS)
//                                          .setLabel1(mAppContext.getResources()
//                                                                .getString(R.string.daily_pass_1))
//                                          .setLabel2(mAppContext.getResources()
//                                                                .getString(R.string.daily_pass_2)));
            if (ZypeSettings.NATIVE_SUBSCRIPTION_ENABLED
                    || ZypeSettings.NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED) {
                contentActionList.add(new Action()
                        .setId(CONTENT_ACTION_CHOOSE_PLAN)
                        .setLabel1(mAppContext.getResources().getString(R.string.action_subscription_1))
                        .setLabel2(mAppContext.getResources().getString(R.string.action_subscription_2)));
            }
            else {
                contentActionList.add(
                        new Action().setId(CONTENT_ACTION_WATCH_NOW)
                                .setLabel1(mAppContext.getResources()
                                        .getString(R.string.watch_now_1))
                                .setLabel2(mAppContext.getResources()
                                        .getString(R.string.watch_now_2)));
            }
        }

        contentActionList.addAll(mGlobalContentActionList);

        return contentActionList;
    }

    /**
     * Set subscribed flag.
     *
     * @param flag Subscribed flag.
     */
    public void setSubscribed(boolean flag) {

        mSubscribed = flag;
    }

    /**
     * Update content actions.
     */
    public void updateContentActions() {

        mEventBus.post(new ActionUpdateEvent(true));
    }

    /**
     * Handle on activity result.
     *
     * @param activity    Activity.
     * @param requestCode Request code.
     * @param resultCode  Result code.
     * @param data        Intent.
     */
    public void handleOnActivityResult(Activity activity, int requestCode, int resultCode,
                                       Intent data) {

        Log.d(TAG, "handleOnActivityResult " + requestCode);

        switch (requestCode) {
            case AuthHelper.AUTH_ON_ACTIVITY_RESULT_REQUEST_CODE:
                mAuthHelper.handleOnActivityResult(this, activity, requestCode, resultCode, data);
                break;
        }
    }

    /**
     * Verify screen switch.
     *
     * @param screenName            Screen name
     * @param iScreenSwitchListener Screen switch listener.
     */
    public void verifyScreenSwitch(String screenName,
                                    IScreenSwitchListener iScreenSwitchListener) {

        UINode uiNode = (UINode) mNavigator.getNodeObjectByScreenName(screenName);
        Log.d(TAG, "VerifyScreenSwitch called in:" + screenName);
        Log.d(TAG, "isVerifyScreenAccess needed:" + uiNode.isVerifyScreenAccess());
        if (uiNode.isVerifyScreenAccess()) {

            if (!mAuthHelper.getIAuthentication().isAuthenticationCanBeDoneLater()) {
                mAuthHelper.handleAuthChain(iScreenSwitchListener::onScreenSwitch);
            }
            else {
                // Never show login later alert
//                boolean loginLater = Preferences.getBoolean(AuthHelper.LOGIN_LATER_PREFERENCES_KEY);
                boolean loginLater = true;
                if (!loginLater && mAuthHelper.getIAuthentication()
                                              .isAuthenticationCanBeDoneLater()) {

                    mAuthHelper.isAuthenticated().subscribe(extras -> {
                        if (extras.getBoolean(AuthHelper.RESULT)) {
                            mAuthHelper.handleAuthChain(
                                    iScreenSwitchListener::onScreenSwitch);
                        }
                        else {
                            AlertDialogFragment.createAndShowAlertDialogFragment(
                                    mNavigator.getActiveActivity(),
                                    mAppContext.getString(R.string.optional_login_dialog_title),
                                    mAppContext.getString(R.string.optional_login_dialog_message),
                                    mAppContext.getString(R.string.now),
                                    mAppContext.getString(R.string.later),
                                    new AlertDialogFragment.IAlertDialogListener() {

                                        @Override
                                        public void onDialogPositiveButton(
                                                AlertDialogFragment alertDialogFragment) {

                                            mAuthHelper.handleAuthChain(
                                                    iScreenSwitchListener::onScreenSwitch);
                                        }

                                        @Override
                                        public void onDialogNegativeButton
                                                (AlertDialogFragment alertDialogFragment) {

                                            Preferences.setBoolean(
                                                    AuthHelper.LOGIN_LATER_PREFERENCES_KEY, true);
                                            iScreenSwitchListener.onScreenSwitch(null);
                                        }
                                    });
                        }
                    });
                }
                else {
                    iScreenSwitchListener.onScreenSwitch(null);
                }
            }
        }
        else {
            iScreenSwitchListener.onScreenSwitch(null);
        }
    }

    /**
     * Switch to screen by name.
     *
     * @param screenName Screen name.
     */
    public void switchToScreen(String screenName) {

        switchToScreen(screenName, (Navigator.ActivitySwitchListener) null);
    }

    /**
     * Switch to screen by name with listener.
     *
     * @param screenName             Screen name.
     * @param activitySwitchListener Activity switch listener.
     */
    public void switchToScreen(String screenName, Navigator.ActivitySwitchListener
            activitySwitchListener) {

        verifyScreenSwitch(screenName, extra ->
                mNavigator.startActivity(screenName, activitySwitchListener)
        );
    }

    /**
     * Switch to screen by name with bundle.
     *
     * @param screenName Screen name.
     * @param bundle     Bundle.
     */
    public void switchToScreen(String screenName, Bundle bundle) {

        verifyScreenSwitch(screenName, extra ->
                mNavigator.startActivity(screenName, bundle)
        );
    }

    /**
     * Action triggered.
     *
     * @param activity Activity.
     * @param action   Action.
     */
    public void actionTriggered(Activity activity, Action action) {

        actionTriggered(activity, action, null);
    }

    /**
     * Action triggered.
     *
     * @param activity Activity.
     * @param action   Action.
     * @param extras   Extras bundle.
     */
    public void actionTriggered(Activity activity, Action action, Bundle extras) {

        switch ((int) action.getId()) {
            case ContentBrowser.CONTENT_ACTION_SEARCH: {
                Log.d(TAG, "actionTriggered -> CONTENT_ACTION_SEARCH");
                switchToScreen(CONTENT_SEARCH_SCREEN);
            }
            break;
            case ContentBrowser.CONTENT_ACTION_SLIDESHOW: {
                Log.d(TAG, "actionTriggered -> CONTENT_ACTION_SLIDESHOW");
                switchToScreen(CONTENT_SLIDESHOW_SCREEN);
            }
            break;
            case ContentBrowser.CONTENT_ACTION_LOGIN_LOGOUT: {
                Log.d(TAG, "actionTriggered -> CONTENT_ACTION_LOGIN_LOGOUT");
                loginLogoutActionTriggered(activity, action);
            }
            break;
        }
    }

    /**
     * Switch to renderer screen.
     *
     * @param content  Content.
     * @param actionId Action id.
     */
    public void switchToRendererScreen(Content content, int actionId) {

        switchToScreen(ContentBrowser.CONTENT_RENDERER_SCREEN, intent -> {
            intent.putExtra(Content.class.getSimpleName(), content);

            // Reset saved seek position if watching content from beginning.
            if (actionId == CONTENT_ACTION_WATCH_FROM_BEGINNING) {
                ContentDatabaseHelper database = ContentDatabaseHelper.getInstance(mAppContext);
                if (database == null) {
                    Log.e(TAG, "Error retrieving database. Recent not saved.");
                    return;
                }
                database.addRecent(content.getId(), 0, false,
                                   DateAndTimeHelper.getCurrentDate().getTime());
            }
        });
    }

    /**
     * Switch to renderer screen.
     *
     * @param inputIntent Input intent for launching renderer screen.
     */
    public void switchToRendererScreen(Intent inputIntent) {

        switchToScreen(ContentBrowser.CONTENT_RENDERER_SCREEN, intent -> {
            intent.putExtras(inputIntent.getExtras());
        });
    }

    /**
     * Handle renderer screen switch.
     *
     * @param activity Activity.
     * @param content  Content.
     * @param actionId Action id.
     */
    public void handleRendererScreenSwitch(Activity activity, Content content, int actionId,
                                           boolean showErrorDialog) {

        /* Zype, Evgeny Cherkasov */
        // Check if subscription video available to user
        if (content.isSubscriptionRequired()
                || (ZypeSettings.SUBSCRIBE_TO_WATCH_AD_FREE_ENABLED && actionId == CONTENT_ACTION_SWAF)) {
            mAuthHelper.isAuthenticated()
                    .subscribe(isAuthenticatedResultBundle -> {
                        boolean result = isAuthenticatedResultBundle.getBoolean(AuthHelper.RESULT);
                        if (result) {
                            if (isUserSubscribed() || actionId == CONTENT_ACTION_SWAF) {
                                switchToRendererScreen(content, actionId);
                            }
                            else {
                                AlertDialogFragment.createAndShowAlertDialogFragment(mNavigator.getActiveActivity(),
                                        mAppContext.getResources().getString(R.string.subscription_alert_title),
                                        mAppContext.getResources().getString(R.string.subscription_alert_message),
                                        null,
                                        mAppContext.getString(R.string.ok),
                                        new AlertDialogFragment.IAlertDialogListener() {
                                            @Override
                                            public void onDialogPositiveButton(AlertDialogFragment alertDialogFragment) {
                                            }

                                            @Override
                                            public void onDialogNegativeButton(AlertDialogFragment alertDialogFragment) {
                                                alertDialogFragment.dismiss();
                                            }
                                        }
                                );
                            }
                        }
                        else {
                            // With Native Subscription feature enabled user can has a subscription
                            // without being logged in as Zype consumer
                            if (isUserSubscribed() && ZypeSettings.NATIVE_SUBSCRIPTION_ENABLED) {
                                switchToRendererScreen(content, actionId);
                            }
                            // In other cases switch to login screen
                            else {
                                mAuthHelper.handleAuthChain(extra -> mNavigator.startActivity(CONTENT_DETAILS_SCREEN, intent -> {
                                    intent.putExtra(Content.class.getSimpleName(), content);
                                }));
                            }
                        }
                    });
            return;
        }

        /* Zype, Evgeny Cherkasov */
//        if (mIAPDisabled) {
        if (mIAPDisabled || ZypeSettings.NATIVE_SUBSCRIPTION_ENABLED) {
            switchToRendererScreen(content, actionId);
        }
        else {
            Log.d(TAG, "validating purchase while handleRendererScreenSwitch");
            mPurchaseHelper
                    .isSubscriptionValidObservable()
                    .subscribe(resultBundle -> {
                        if (resultBundle.getBoolean(PurchaseHelper.RESULT) &&
                                resultBundle.getBoolean(PurchaseHelper.RESULT_VALIDITY)) {
                            // Switch to renderer screen.
                            switchToRendererScreen(content, actionId);
                        }
                        else if (resultBundle.getBoolean(PurchaseHelper.RESULT) &&
                                !resultBundle.getBoolean(PurchaseHelper.RESULT_VALIDITY)) {

                            if (showErrorDialog) {
                                AlertDialogFragment.createAndShowAlertDialogFragment(
                                        mNavigator.getActiveActivity(),
                                        mAppContext.getString(R.string.iap_error_dialog_title),
                                        mAppContext.getString(R.string.subscription_expired),
                                        null,
                                        mAppContext.getString(R.string.ok),
                                        new AlertDialogFragment.IAlertDialogListener() {

                                            @Override
                                            public void onDialogPositiveButton
                                                    (AlertDialogFragment alertDialogFragment) {

                                            }

                                            @Override
                                            public void onDialogNegativeButton
                                                    (AlertDialogFragment alertDialogFragment) {

                                                alertDialogFragment.dismiss();
                                            }
                                        });
                            }
                            else {
                                Log.e(TAG, "Purchase expired while handleRendererScreenSwitch");
                                ContentBrowser.getInstance(activity).setLastSelectedContent(content)
                                              .switchToScreen(ContentBrowser
                                                                      .CONTENT_DETAILS_SCREEN);
                            }
                            updateContentActions();
                        }
                        else {
                            // IAP errors are handled by IAP sdk.
                            Log.e(TAG, "IAP error!!!");
                        }
                    }, throwable -> {
                        // IAP errors are handled by IAP sdk.
                        Log.e(TAG, "IAP error!!!", throwable);
                    });
        }
    }

    /**
     * Action triggered.
     *
     * @param activity Activity.
     * @param content  Content.
     * @param actionId Action id.
     */
    public void actionTriggered(Activity activity, Content content, int actionId) {

        List<IContentActionListener> iContentActionListenersList =
                mContentActionListeners.get(actionId);

        if (iContentActionListenersList != null && iContentActionListenersList.size() > 0) {
            for (IContentActionListener listener : iContentActionListenersList) {
                listener.onContentAction(activity, content, actionId);
            }
        }

        AnalyticsHelper.trackContentDetailsAction(content, actionId);
        switch (actionId) {
            case CONTENT_ACTION_WATCH_NOW:
            case CONTENT_ACTION_WATCH_FROM_BEGINNING:
            case CONTENT_ACTION_RESUME:
                handleRendererScreenSwitch(activity, content, actionId, true);
                break;
            case CONTENT_ACTION_SUBSCRIPTION:
            case CONTENT_ACTION_DAILY_PASS:
                mPurchaseHelper.handleAction(activity, content, actionId);
                break;
            /* Zype, Evgeny Cherkasov */
            case CONTENT_ACTION_CHOOSE_PLAN:
                mPurchaseHelper.handleAction(activity, content, actionId);
                break;
            case CONTENT_ACTION_SWAF:
                handleRendererScreenSwitch(activity, content, actionId, true);
                break;
        }
    }

//    /**
//     * Get categories observable.
//     *
//     * @param root                             Content container.
//     * @param dataLoaderRecipeForCategories    Data loader recipe for getting categories.
//     * @param dynamicParserRecipeForCategories Dynamic parser recipe for getting categories.
//     * @return RX Observable.
//     */
//    private Observable<Object> getCategoriesObservable(ContentContainer root,
//                                                       Recipe dataLoaderRecipeForCategories,
//                                                       Recipe dynamicParserRecipeForCategories) {
//
//        /* Zype, Evgeny Cherkasov */
//        // Set parent playlist id in receipt params to fetch only its child playlists
//        String[] params;
//        if (root.getName().equals("Root")) {
//            params = new String[] { ZypeSettings.ROOT_PLAYLIST_ID };
//        }
//        else {
//            params = new String[] { (String) root.getExtraStringValue("keyDataType") };
//        }
//
//        return mDataLoadManager.cookRecipeObservable(
//                dataLoaderRecipeForCategories,
//                null,
//                null,
//                null).map(
//                feedDataForCategories -> {
//                    if (DEBUG_RECIPE_CHAIN) {
//                        Log.d(TAG, "Feed download complete");
//                    }
//
//                    if (CAUSE_A_FEED_ERROR_FOR_DEBUGGING) {
//                        return Observable.error(new Exception());
//                    }
//                    return feedDataForCategories;
//                }).concatMap(
//                feedDataForCategories -> mDynamicParser.cookRecipeObservable
//                        (dynamicParserRecipeForCategories,
//                                feedDataForCategories,
//                                null,
//                                params)).map(
//                contentContainerAsObject -> {
//                    ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;
//
//                    ContentContainer alreadyAvailableContentContainer =
//                            root.findContentContainerByName(contentContainer.getName());
//
//                    if (alreadyAvailableContentContainer == null) {
//                        root.addContentContainer(contentContainer);
//                        alreadyAvailableContentContainer = contentContainer;
//                    }
//
//                    if (DEBUG_RECIPE_CHAIN) {
//                        Log.d(TAG, "Dynamic parser got an container");
//                    }
//                    return alreadyAvailableContentContainer;
//                })
//                /* Zype, Evgeny Cherkasov */
//                // Get all nested playlists for each playlist in root
//                .concatMap(contentContainer -> getSubCategoriesObservable(contentContainer, dataLoaderRecipeForCategories, dynamicParserRecipeForCategories));
//    }

//    /* Zype, Evgeny Cherkasov */
//    private Observable<Object> getSubCategoriesObservable(ContentContainer parentContentContainer,
//                                                          Recipe dataLoaderRecipeForCategories,
//                                                          Recipe dynamicParserRecipeForCategories) {
//        parentContentContainer.getContentContainers().clear();
//        if ((Integer) parentContentContainer.getExtraStringValue("playlistItemCount") > 0) {
//            // If playlist contains videos just return itself and ignore nested playlists
//            return Observable.just(parentContentContainer);
//        }
//        else {
//            return Observable.concat(
//                    Observable.just(parentContentContainer),
//                    mDataLoadManager.cookRecipeObservable(dataLoaderRecipeForCategories, null, null, null)
//                            .map(feedDataForCategories -> {
//                                if (CAUSE_A_FEED_ERROR_FOR_DEBUGGING) {
//                                    return Observable.error(new Exception());
//                                }
//                                return feedDataForCategories;
//                            })
//                            .concatMap(feedDataForCategories -> {
//                                String[] params = new String[]{(String) parentContentContainer.getExtraStringValue("keyDataType")};
//                                return mDynamicParser.cookRecipeObservable(dynamicParserRecipeForCategories, feedDataForCategories, null, params);
////                                        .concatMap(contentSubContainer -> getSubCategoriesObservable(contentSubContainer, dataLoaderRecipeForCategories, dynamicParserRecipeForCategories));
//                            })
//                            .filter(contentSubContainerAsObject -> contentSubContainerAsObject != null)
//                            .map(contentSubContainerAsObject -> {
////                                            if (contentSubContainerAsObject == null) {
////                                                return contentContainer;
////                                            }
//                                ContentContainer contentSubContainer = (ContentContainer) contentSubContainerAsObject;
//                                if (DEBUG_RECIPE_CHAIN) {
//                                    Log.d(TAG, "getSubCategoriesObservable(): " + contentSubContainer.getName());
//                                }
//                                parentContentContainer.getContentContainers().add(contentSubContainer);
//                                if ((Integer) contentSubContainer.getExtraStringValue("playlistItemCount") > 0) {
////                                    return contentSubContainer;
//                                    return parentContentContainer;
//                                }
//                                else {
//                                    return parentContentContainer;
//                                }
//                            })
//                            .distinct()
//            );
//        }
//    }

//    /**
//     * Get contents observable.
//     *
//     * @param observable                     Rx Observable chain to continue on.
//     * @param dataLoaderRecipeForContents    Data loader recipe for getting contents.
//     * @param dynamicParserRecipeForContents Dynamic parser  recipe for getting contents.
//     * @return RX Observable.
//     */
//    private Observable<Object> getContentsObservable(Observable<Object> observable,
//                                                     Recipe dataLoaderRecipeForContents,
//                                                     Recipe dynamicParserRecipeForContents) {
//
//        return observable.concatMap(contentContainerAsObject -> {
//            ContentContainer contentContainer = (ContentContainer) contentContainerAsObject;
//            if (DEBUG_RECIPE_CHAIN) {
//                Log.d(TAG, "ContentContainer:" + contentContainer.getName());
//            }
//            return mDataLoadManager.cookRecipeObservable(
//                    dataLoaderRecipeForContents,
//                    null,
//                    null,
//                    null).map(
//                    feedDataForContent -> {
//                        if (DEBUG_RECIPE_CHAIN) {
//                            Log.d(TAG, "Feed for container complete");
//                        }
//                        return Pair.create(contentContainerAsObject, feedDataForContent);
//                    });
//        }).concatMap(objectPair -> {
//            ContentContainer contentContainer = (ContentContainer) objectPair.first;
//            /* Zype, Evgeny Cherkasov */
//            // Clear content list to avoid duplicate contents for nested playlist (subcategory)
//            contentContainer.getContents().clear();
//            String feed = (String) objectPair.second;
//
//            String[] params = new String[]{(String) contentContainer
//                    .getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG)
//            };
//
//            return mDynamicParser.cookRecipeObservable(
//                    dynamicParserRecipeForContents,
//                    feed,
//                    null,
//                    params).map(contentAsObject -> {
//                if (DEBUG_RECIPE_CHAIN) {
//                    Log.d(TAG, "Parser got an content");
//                }
//                Content content = (Content) contentAsObject;
//                if (content != null) {
//                    contentContainer.addContent(content);
//                }
//                return Pair.create(contentContainer, contentAsObject);
//            });
//        });
//    }

//    /**
//     * Get content chain observable.
//     *
//     * @param hardCodedCategoryName            Hard coded category name.
//     * @param dataLoaderRecipeForCategories    Data loader recipe for getting categories.
//     * @param dataLoaderRecipeForContents      Data loader recipe for getting contents.
//     * @param dynamicParserRecipeForCategories Dynamic parser recipe for getting categories.
//     * @param dynamicParserRecipeForContents   Dynamic parser  recipe for getting contents.
//     * @param root                             Content container.
//     * @return RX Observable.
//     */
//    private Observable<Object> getContentChainObservable(String hardCodedCategoryName,
//                                                         Recipe dataLoaderRecipeForCategories,
//                                                         Recipe dataLoaderRecipeForContents,
//                                                         Recipe dynamicParserRecipeForCategories,
//                                                         Recipe dynamicParserRecipeForContents,
//                                                         ContentContainer root) {
//
//        Observable<Object> observable;
//
//        if (hardCodedCategoryName == null) {
//            observable = getCategoriesObservable(root, dataLoaderRecipeForCategories,
//                    dynamicParserRecipeForCategories);
//        }
//        else {
//            observable = Observable.just(hardCodedCategoryName)
//                    .map(s -> {
//                        ContentContainer contentContainer =
//                                new ContentContainer(hardCodedCategoryName);
//                        root.addContentContainer(contentContainer);
//                        return contentContainer;
//                    });
//        }
//
//        return getContentsObservable(observable, dataLoaderRecipeForContents,
//                dynamicParserRecipeForContents);
//    }

//    /**
//     * Run global recipes at index.
//     *
//     * @param index Index.
//     * @param root  Content container.
//     * @return RX Observable.
//     */
//    private Observable<Object> runGlobalRecipeAtIndex(int index, ContentContainer root) {
//
//
//        NavigatorModel.GlobalRecipes recipe = mNavigator.getNavigatorModel().getGlobalRecipes()
//                .get(index);
//
//        Recipe dataLoaderRecipeForCategories = recipe.getCategories().dataLoaderRecipe;
//        Recipe dataLoaderRecipeForContents = recipe.getContents().dataLoaderRecipe;
//
//        Recipe dynamicParserRecipeForCategories = recipe.getCategories().dynamicParserRecipe;
//        Recipe dynamicParserRecipeForContents = recipe.getContents().dynamicParserRecipe;
//
//        // Add any extra configurations that the parser recipe needs from the navigator recipe.
//        if (recipe.getRecipeConfig() != null) {
//            // Add if the recipe is for live feed data.
//            dynamicParserRecipeForContents.getMap().put(Recipe.LIVE_FEED_TAG,
//                    recipe.getRecipeConfig().liveContent);
//        }
//
//        String hardCodedCategoryName = recipe.getCategories().name;
//
//        return getContentChainObservable(hardCodedCategoryName,
//                dataLoaderRecipeForCategories,
//                dataLoaderRecipeForContents,
//                dynamicParserRecipeForCategories,
//                dynamicParserRecipeForContents,
//                root);
//    }

    /**
     * Run global recipes.
     */
    public void runGlobalRecipes(Activity activity, ICancellableLoad cancellable) {

        final ContentContainer root = new ContentContainer("Root");
        Subscription subscription =
                Observable.range(0, mNavigator.getNavigatorModel().getGlobalRecipes().size())
                          // Do this first to make sure were running in new thread right a way.
                          .subscribeOn(Schedulers.newThread())
                          .concatMap(index -> mContentLoader.runGlobalRecipeAtIndex(index, root))
                          .onBackpressureBuffer() // This must be right after concatMap.
                          .doOnNext(o -> {
                              if (DEBUG_RECIPE_CHAIN) {
                                  Log.d(TAG, "doOnNext");
                              }
                          })
                          // This should be last so the rest is running on a separate thread.
                          .observeOn(AndroidSchedulers.mainThread())
                          .subscribe(objectPair -> {
                              if (DEBUG_RECIPE_CHAIN) {
                                  Log.d(TAG, "subscriber onNext called");
                              }
                          }, throwable -> {
                              Log.e(TAG, "Recipe chain failed:", throwable);
                              ErrorHelper.injectErrorFragment(
                                      mNavigator.getActiveActivity(),
                                      ErrorUtils.ERROR_CATEGORY.FEED_ERROR,
                                      (errorDialogFragment, errorButtonType,
                                       errorCategory) -> {
                                          if (errorButtonType ==
                                                  ErrorUtils.ERROR_BUTTON_TYPE.EXIT_APP) {
                                              mNavigator.getActiveActivity().finishAffinity();
                                          }
                                      });

                          }, () -> {
                              Log.v(TAG, "Recipe chain completed");
                              // Remove empty sub containers.
                              root.removeEmptySubContainers();

                              mContentLoader.setRootContentContainer(root);
                              if (mIRootContentContainerListener != null) {
                                  mIRootContentContainerListener.onRootContentContainerPopulated
                                          (mContentLoader.getRootContentContainer());
                              }
                              mContentLoader.setContentReloadRequired(false);
                              mContentLoader.setContentLoaded(true);
                              if (cancellable != null && cancellable.isLoadingCancelled()) {
                                  Log.d(TAG, "Content load complete but app has been cancelled, " +
                                          "returning from here");
                                  return;
                              }
                              if (mLauncherIntegrationManager != null && activity != null &&
                                      LauncherIntegrationManager
                                              .isCallFromLauncher(activity.getIntent())) {

                                  Log.d(TAG, "Call from launcher with intent " +
                                          activity.getIntent());
                                  String contentId = null;
                                  try {
                                      contentId = LauncherIntegrationManager
                                              .getContentIdToPlay(mAppContext,
                                                                  activity.getIntent());

                                      Content content =
                                              getRootContentContainer().findContentById(contentId);
                                      if (content == null) {
                                          mRecommendationManager.dismissRecommendation(contentId);
                                          throw new IllegalArgumentException("No content exist " +
                                                                                     "for " +
                                                                                     "contentId "
                                                                                     + contentId);
                                      }
                                      AnalyticsHelper.trackLauncherRequest(contentId, content,
                                             getSourceOfContentPlayRequest(activity.getIntent()));
                                      Intent intent = new Intent();
                                      intent.putExtra(Content.class.getSimpleName(), content);
                                      intent.putExtra(REQUEST_FROM_LAUNCHER, true);
                                      intent.putExtra(PreferencesConstants.CONTENT_ID,
                                                      content.getId());
                                      switchToHomeScreen(intent);

                                  }
                                  catch (Exception e) {
                                      Log.e(TAG, e.getLocalizedMessage(), e);
                                      AnalyticsHelper.trackLauncherRequest(contentId, null,
                                             getSourceOfContentPlayRequest(activity.getIntent()));
                                      AlertDialogFragment.createAndShowAlertDialogFragment
                                              (mNavigator.getActiveActivity(),
                                               "Error",
                                               "The selected content is no longer available",
                                               null,
                                               mAppContext.getString(R.string.ok),
                                               new AlertDialogFragment.IAlertDialogListener() {

                                                   @Override
                                                   public void onDialogPositiveButton
                                                           (AlertDialogFragment
                                                                    alertDialogFragment) {

                                                   }

                                                   @Override
                                                   public void onDialogNegativeButton
                                                           (AlertDialogFragment
                                                                    alertDialogFragment) {

                                                       alertDialogFragment.dismiss();
                                                       if (cancellable != null &&
                                                               cancellable.isLoadingCancelled()) {
                                                           Log.d(TAG, "switchToHomeScreen after " +
                                                                   "launcher integration " +
                                                                   "exception cancelled");
                                                           return;
                                                       }
                                                       switchToHomeScreen();
                                                   }
                                               });
                                  }
                              }
                              else {
                                  if (cancellable != null &&
                                          cancellable.isLoadingCancelled()) {
                                      Log.d(TAG, "switchToHomeScreen after Splash cancelled");
                                      return;
                                  }

                                  // Send recommendations if authentication is not required, or if
                                  // the user is logged in.
                                  if (!Navigator.isScreenAccessVerificationRequired(
                                          mNavigator.getNavigatorModel()) ||
                                          Preferences.getBoolean(
                                                  LauncherIntegrationManager
                                                          .PREFERENCE_KEY_USER_AUTHENTICATED)) {
                                      mRecommendationManager.cleanDatabase();
                                      mRecommendationManager
                                              .updateGlobalRecommendations(mAppContext);
                                  }
                                  switchToHomeScreen();
                              }
                          });

        mCompositeSubscription.add(subscription);
    }

    /* Zype, Evgeny Cherkasov */
    public void runGlobalRecipesForLastSelected(Activity activity, ICancellableLoad cancellable) {
        final ContentContainer root = getLastSelectedContentContainer();
        Subscription subscription =
                Observable.range(0, mNavigator.getNavigatorModel().getGlobalRecipes().size())
                        // Do this first to make sure were running in new thread right a way.
                        .subscribeOn(Schedulers.newThread())
                        .concatMap(index -> {
                            NavigatorModel.GlobalRecipes recipe = mNavigator.getNavigatorModel().getGlobalRecipes().get(index);
                            Recipe recipeDynamicParserVideos = Recipe.newInstance(mAppContext, "recipes/ZypeSearchContentsRecipe.json");
                            return mContentLoader.runZypeGlobalRecipeAtIndex(recipe, recipeDynamicParserVideos, index, root);
                        })
                        .onBackpressureBuffer() // This must be right after concatMap.
                        .doOnNext(o -> {
                            if (DEBUG_RECIPE_CHAIN) {
                                Log.d(TAG, "doOnNext");
                            }
                        })
                        // This should be last so the rest is running on a separate thread.
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(objectPair -> {
                            if (DEBUG_RECIPE_CHAIN) {
                                Log.d(TAG, "subscriber onNext called");
                            }
                        }, throwable -> {
                            Log.e(TAG, "Recipe chain failed:", throwable);
                            ErrorHelper.injectErrorFragment(
                                    mNavigator.getActiveActivity(),
                                    ErrorUtils.ERROR_CATEGORY.FEED_ERROR,
                                    (errorDialogFragment, errorButtonType,
                                     errorCategory) -> {
                                        if (errorButtonType ==
                                                ErrorUtils.ERROR_BUTTON_TYPE.EXIT_APP) {
                                            mNavigator.getActiveActivity().finishAffinity();
                                        }
                                    });

                        }, () -> {
                            Log.v(TAG, "Recipe chain completed");
                            // Remove empty sub containers.
                            if (!root.getExtraStringValue(Recipe.KEY_DATA_TYPE_TAG).equals(ZypeSettings.ROOT_MY_LIBRARY_PLAYLIST_ID)) {
                                root.removeEmptySubContainers();
                            }

//                            mContentLoader.setRootContentContainer(root);
                            if (mIRootContentContainerListener != null) {
                                mIRootContentContainerListener.onRootContentContainerPopulated
                                        (mContentLoader.getRootContentContainer());
                            }
                            mContentLoader.setContentReloadRequired(false);
                            mContentLoader.setContentLoaded(true);
                            if (cancellable != null && cancellable.isLoadingCancelled()) {
                                Log.d(TAG, "Content load complete but app has been cancelled, " +
                                        "returning from here");
                                return;
                            }
                            if (mLauncherIntegrationManager != null && activity != null &&
                                    LauncherIntegrationManager
                                            .isCallFromLauncher(activity.getIntent())) {

                                Log.d(TAG, "Call from launcher with intent " +
                                        activity.getIntent());
                                String contentId = null;
                                try {
                                    contentId = LauncherIntegrationManager
                                            .getContentIdToPlay(mAppContext,
                                                    activity.getIntent());

                                    Content content =
                                            getRootContentContainer().findContentById(contentId);
                                    if (content == null) {
                                        mRecommendationManager.dismissRecommendation(contentId);
                                        throw new IllegalArgumentException("No content exist " +
                                                "for " +
                                                "contentId "
                                                + contentId);
                                    }
                                    AnalyticsHelper.trackLauncherRequest(contentId, content,
                                            getSourceOfContentPlayRequest(activity.getIntent()));
                                    Intent intent = new Intent();
                                    intent.putExtra(Content.class.getSimpleName(), content);
                                    intent.putExtra(REQUEST_FROM_LAUNCHER, true);
                                    intent.putExtra(PreferencesConstants.CONTENT_ID,
                                            content.getId());
                                    switchToHomeScreen(intent);

                                }
                                catch (Exception e) {
                                    Log.e(TAG, e.getLocalizedMessage(), e);
                                    AnalyticsHelper.trackLauncherRequest(contentId, null,
                                            getSourceOfContentPlayRequest(activity.getIntent()));
                                    AlertDialogFragment.createAndShowAlertDialogFragment
                                            (mNavigator.getActiveActivity(),
                                                    "Error",
                                                    "The selected content is no longer available",
                                                    null,
                                                    mAppContext.getString(R.string.ok),
                                                    new AlertDialogFragment.IAlertDialogListener() {

                                                        @Override
                                                        public void onDialogPositiveButton
                                                                (AlertDialogFragment
                                                                         alertDialogFragment) {

                                                        }

                                                        @Override
                                                        public void onDialogNegativeButton
                                                                (AlertDialogFragment
                                                                         alertDialogFragment) {

                                                            alertDialogFragment.dismiss();
                                                            if (cancellable != null &&
                                                                    cancellable.isLoadingCancelled()) {
                                                                Log.d(TAG, "switchToHomeScreen after " +
                                                                        "launcher integration " +
                                                                        "exception cancelled");
                                                                return;
                                                            }
                                                            switchToHomeScreen();
                                                        }
                                                    });
                                }
                            }
//                            if (mLauncherIntegrationManager != null && activity != null &&
//                                    LauncherIntegrationManager
//                                            .isCallFromLauncher(activity.getIntent())) {
//
//                                Log.i(TAG, "Call from launcher with intent " +
//                                        activity.getIntent());
//
//                                try {
//                                    long contentId = LauncherIntegrationManager
//                                            .getContentIdToPlay(mAppContext, activity.getIntent());
//
//                                    Content content =
//                                            getRootContentContainer().findContentById(contentId);
//                                    if (content == null) {
//                                        throw new IllegalArgumentException("No content exist for " +
//                                                "contentId " +
//                                                contentId);
//                                    }
//                                    handleRendererScreenSwitch(mNavigator.getActiveActivity(),
//                                            content,
//                                            CONTENT_ACTION_WATCH_NOW, false);
//
//                                }
//                                catch (Exception e) {
//                                    Log.e(TAG, e.getLocalizedMessage(), e);
//                                    AlertDialogFragment.createAndShowAlertDialogFragment
//                                            (mNavigator.getActiveActivity(),
//                                                    "Error",
//                                                    "The desired content does not exist",
//                                                    null,
//                                                    mAppContext.getString(R.string.ok),
//                                                    new AlertDialogFragment.IAlertDialogListener() {
//
//                                                        @Override
//                                                        public void onDialogPositiveButton
//                                                                (AlertDialogFragment alertDialogFragment) {
//
//                                                        }
//
//                                                        @Override
//                                                        public void onDialogNegativeButton
//                                                                (AlertDialogFragment alertDialogFragment) {
//
//                                                            alertDialogFragment.dismiss();
//                                                            if (cancellable != null &&
//                                                                    cancellable.isLoadingCancelled()) {
//                                                                Log.d(TAG, "switchToHomeScreen after " +
//                                                                        "Splash cancelled");
//                                                                return;
//                                                            }
//                                                            switchToHomeScreen();
//                                                        }
//                                                    });
//                                }
//                            }
                            else {
                                if (cancellable != null &&
                                        cancellable.isLoadingCancelled()) {
                                    Log.d(TAG, "switchToHomeScreen after Splash cancelled");
                                    return;
                                }

//                                // Send recommendations if authentication is not required, or if
//                                // the user is logged in.
//                                if (!Navigator.isScreenAccessVerificationRequired(
//                                        mNavigator.getNavigatorModel()) ||
//                                        Preferences.getBoolean(
//                                                LauncherIntegrationManager
//                                                        .PREFERENCE_KEY_USER_AUTHENTICATED)) {
//                                    mRecommendationManager.cleanDatabase();
//                                    mRecommendationManager
//                                            .updateGlobalRecommendations(mAppContext);
//                                }
//                                switchToHomeScreen();
                                // TODO: Consider to use event bus instead of broadcast
                                // This broadcast is handled in ZypePlaylistContentBrowseFragment to update content
                                LocalBroadcastManager.getInstance(mNavigator.getActiveActivity()).sendBroadcast(new Intent("DataUpdated"));
                            }
                        });

        mCompositeSubscription.add(subscription);
    }

    //

    /**
     * Switches to home screen.
     *
     * @param inputIntent input intent to be passed to home screen.
     */
    public void switchToHomeScreen(Intent inputIntent) {

        switchToScreen(CONTENT_HOME_SCREEN, intent -> {
            // Make sure we clear activity stack.
            updateIntentToClearActivityStack(intent);
            intent.putExtras(inputIntent.getExtras());
        });
    }

    /**
     * Switches to home screen.
     */
    public void switchToHomeScreen() {

        switchToScreen(CONTENT_HOME_SCREEN, intent -> {
            // Make sure we clear activity stack.
            updateIntentToClearActivityStack(intent);
        });
    }

    /**
     * Add required flags to the intent to clear the activity stack.
     *
     * @param intent Intent to be updated.
     */
    private void updateIntentToClearActivityStack(Intent intent) {

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * Method which returns true if IAP is enabled, false otherwise.
     *
     * @return true if IAP is enabled, false otherwise.
     */
    public boolean isIapDisabled() {

        return mIAPDisabled;
    }

    /**
     * Method which returns true if user authentication is mandatory, false otherwise.
     *
     * @return true if user authentication is mandatory, false otherwise.
     */
    public boolean isUserAuthenticationMandatory() {

        if (mAuthHelper == null || mAuthHelper.getIAuthentication() == null) {
            return false;
        }
        else if (mAuthHelper.getIAuthentication().isAuthenticationCanBeDoneLater()) {
            return false;
        }
        return true;
    }

    /**
     * Get the recommendation manager instance.
     *
     * @return The recommendation manager.
     */
    public RecommendationManager getRecommendationManager() {

        return mRecommendationManager;
    }

    /* Zype, Evgeny Cherkasov */
    public boolean isUserLoggedIn() {
        return userLoggedIn;
    }

    public boolean isUserSubscribed() {
        return mSubscribed;
    }

    public void updateUserSubscribed() {
        boolean hasNativeSubscription = Preferences.getBoolean(PurchaseHelper.CONFIG_PURCHASE_VERIFIED);
        boolean hasZypeSubscription = Preferences.getLong(ZypeAuthentication.PREFERENCE_CONSUMER_SUBSCRIPTION_COUNT) > 0;
        // For testing Native to Universal case with Amazon App Tester
        // uncomment following line since Zype service does not
        // validate App Tester purchase receipt and does not create Zype subscription
        // TODO: This line must be commented for release build
//            hasZypeSubscription = true;
        if (ZypeSettings.UNIVERSAL_SUBSCRIPTION_ENABLED
                || ZypeSettings.NATIVE_TO_UNIVERSAL_SUBSCRIPTION_ENABLED) {
            setSubscribed(hasZypeSubscription);
        }
        else if (ZypeSettings.NATIVE_SUBSCRIPTION_ENABLED) {
            setSubscribed(hasNativeSubscription);
        }
    }

    public void updateSubscriptionSku(String sku) {
        mPurchaseHelper.setSubscriptionSKU(sku);
    }

    public void switchToSubscriptionScreen(Bundle extras) {
        switchToScreen(SUBSCRIPTION_SCREEN, intent -> {
            intent.putExtras(extras);
        });
    }

    public PurchaseHelper getPurchaseHelper() {
        return mPurchaseHelper;
    }
}
