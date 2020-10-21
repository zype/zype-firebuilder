package com.mediamelon.smartstreaming;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.util.Log;


public class MMNetworkInformationRetriever extends BroadcastReceiver{
    enum MMNetworkInformationRetrievalPolicy{
        RetrieveNetworkInformationUnknown,
        RetrieveNetworkInformationNone,
        RetrieveNetworkInformationOnce,
        RetrieveNetworkInformationPolling
    }

    enum MMAccessInformationType{
        MMAccessInformationType_Location,
        MMAccessInformationType_Telephony,
        MMAccessInformationType_NetworkState,
        MMAccessInformationType_WifiStrength
    }

    class LocationInformation{
        Double latitude;
        Double longitude;

        @Override
        public String toString(){
            return "Latitude = " + latitude + " Longitude = " + longitude;
        }
    }

    private String TAG = "MMNetworkInformationRetriever";
    static private boolean disableTelephonyAndLocationMetrics = true;
    private static Integer pollingFrequency = -1;
    private static MMNetworkInformationRetrievalPolicy networkInformationRetrievalPolicy =  MMNetworkInformationRetrievalPolicy.RetrieveNetworkInformationUnknown;
    private Timer timer;
    private static MMNetworkInformationRetriever retriever = null;
    private Integer pollCount = new Integer(0);

    private boolean locationFetchDisabled = false;
    private boolean telephonyFetchDisabled = false;
    private boolean wifiSignalReportingDisabled = false;
    private boolean networkStateReportingDisabled = false;
    private boolean networkUpdateIntentRegistered = false;
    private boolean fetchTelephonyMetrics = false;
    private NetworkInfo activeNetworkInformation = null;

    public static MMNetworkInformationRetriever instance(){
        if(retriever == null){
            retriever = new MMNetworkInformationRetriever();
        }
        return retriever;
    }

    static public ArrayList<String> getMissingPermissions(Context context){
        if (Build.VERSION.SDK_INT >= 23) {
            return getMissingPermissionsInternal(context);
        }else{
            return null;
        }
    }

    private void updateCanFetchParams(Context context){
        synchronized(TAG) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    locationFetchDisabled = !canFetchLocationInformation(context);
                    telephonyFetchDisabled = !canFetchTelephonyInformation(context);
                    wifiSignalReportingDisabled = !canFetchWifiStrengthInformation(context);
                    networkStateReportingDisabled = !canFetchNetworkInformation(context);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void initializeRetriever(Context context){
        synchronized(TAG) {
            try {
                if (Build.VERSION.SDK_INT >= 23) {
                    locationFetchDisabled = !canFetchLocationInformation(context);
                    telephonyFetchDisabled = !canFetchTelephonyInformation(context);
                    wifiSignalReportingDisabled = !canFetchWifiStrengthInformation(context);
                    networkStateReportingDisabled = !canFetchNetworkInformation(context);
                } else {
                    locationFetchDisabled = false;
                    telephonyFetchDisabled = false;
                    wifiSignalReportingDisabled = false;
                    networkStateReportingDisabled = false;
                }

                fetchTelephonyMetrics = false;
                activeNetworkInformation = null;
                updateNetworkInformation(context);

                if (networkUpdateIntentRegistered == false) {
                    IntentFilter intentFilter = new IntentFilter();
                    intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
                    context.registerReceiver(this, intentFilter);
                    networkUpdateIntentRegistered = true;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public boolean startRetriever(Context ctx, Integer frequency){
        Log.i(TAG, "StartRetriver In");
        if(disableTelephonyAndLocationMetrics == true){
            return false;
        }
        pollCount = 0;
        boolean retval = false;
        if(ctx != null) {
            updateCanFetchParams(ctx);
            setPollingFrequency(frequency);
            stopPolling();
            retval = retrieveInformation(ctx);
        }
        return  retval;
    }

    public void destroy(Context ctx){
        if (ctx != null && networkUpdateIntentRegistered){
            networkUpdateIntentRegistered = false;
            ctx.unregisterReceiver(this);
        }
    }

    private static MMConnectionInfo getMobileConnType(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return MMConnectionInfo.Cellular_2G;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return MMConnectionInfo.Cellular_3G;
            case TelephonyManager.NETWORK_TYPE_LTE:
                return MMConnectionInfo.Cellular_4G;
            default:
                return MMConnectionInfo.Cellular;
        }
    }

    @TargetApi(23)
    private static boolean canFetchTelephonyInformation(Context context){
        if (disableTelephonyAndLocationMetrics == true){
            return false;
        }
        if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)){
            return true;
        }
        return false;
    }

    @TargetApi(23)
    private static boolean canFetchLocationInformation(Context context){
        if (disableTelephonyAndLocationMetrics == true){
            return false;
        }
        if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)){
            return true;
        }
        return false;
    }

    @TargetApi(23)
    private static boolean canFetchNetworkInformation(Context context){
        if(context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    @TargetApi(23)
    private static boolean canFetchWifiStrengthInformation(Context context){
        if(context.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    @TargetApi(23)
    static public ArrayList<String> getMissingPermissionsInternal(Context context){
        ArrayList<String> permissions = new ArrayList<String>();
        if(disableTelephonyAndLocationMetrics == false) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }

        if (context.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }
        if(context.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        }
        return permissions;
    }

    @TargetApi(23)
    public static boolean hasRequiredPermissions(Context context){
        boolean retval = false;
        if(canFetchPermission(context, MMAccessInformationType.MMAccessInformationType_Location) &&
                canFetchPermission(context, MMAccessInformationType.MMAccessInformationType_Telephony) &&
                canFetchPermission(context, MMAccessInformationType.MMAccessInformationType_NetworkState) &&
                canFetchPermission(context, MMAccessInformationType.MMAccessInformationType_WifiStrength)){
            retval = true;
        }
        return retval;
    }

    private static boolean canFetchPermission(Context context, MMAccessInformationType type){
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= 23){
            switch (type){
                case MMAccessInformationType_Location:{
                    retVal = canFetchLocationInformation(context);
                }
                break;
                case MMAccessInformationType_Telephony:{
                    retVal = canFetchTelephonyInformation(context);
                }
                break;
                case MMAccessInformationType_NetworkState:{
                    retVal = canFetchNetworkInformation(context);
                }
                break;
                case MMAccessInformationType_WifiStrength:{
                    retVal = canFetchNetworkInformation(context);
                }
                break;
            }
        }
        return retVal;
    }

    private MMCellInfo getActiveNetworkConnection(List<MMCellInfo> cellInfos){
        MMCellInfo retval = null;
        if(cellInfos != null && activeNetworkInformation != null) {
            if (cellInfos.size() == 1) {
                return cellInfos.get(0);
            }

            //Check if there is unique type
            ArrayList<Integer> cellInfoIdx = new ArrayList<Integer>();
            int selectedCellInfoIdx = -1;

            String cellularType = activeNetworkInformation.getTypeName().toLowerCase();
            if(cellularType != null){
                for (int i = 0; i < cellInfos.size(); i++) {
                    String cellRadio = cellInfos.get(i).getCellRadio().toLowerCase();
                    Log.v(TAG, "ActiveCellType " + cellularType + " CellRadio " + cellRadio);
                    if(cellularType.contains(cellRadio)){
                        cellInfoIdx.add(i);
                    }
                }
            }

            if (cellInfoIdx.size() == 0){
                //Check based on subtype
                cellularType = activeNetworkInformation.getSubtypeName().toLowerCase();
                if(cellularType != null) {
                    for (int i = 0; i < cellInfos.size(); i++) {
                        String cellRadio = cellInfos.get(i).getCellRadio().toLowerCase();
                        Log.v(TAG, "ActiveCellSubType" + cellularType + " CellRadio " + cellRadio);
                        if (cellularType.contains(cellRadio)) {
                            cellInfoIdx.add(i);
                        }
                    }
                }
            }

            if (cellInfoIdx.size() != 1) {
                cellularType = activeNetworkInformation.getExtraInfo();
                if (cellInfoIdx.size() == 0) {
                    for (int i = 0; i < cellInfos.size(); i++) {
                        String cellRadio = cellInfos.get(i).getCellRadio().toLowerCase();
                        Log.v(TAG, "ActiveExtraInfo : '" + cellularType + "'' CellRadio '" + cellRadio + "'");
                        if (cellularType.contains(cellRadio)) {
                            cellInfoIdx.add(i);
                            selectedCellInfoIdx = i; //:(, lest take first satisfying the extra info
                            break;
                        }
                    }
                }else{
                    //Resolute based on extra info
                    for (int i = 0; i < cellInfoIdx.size(); i++) {
                        String cellRadio = cellInfos.get(cellInfoIdx.get(i)).getCellRadio().toLowerCase();
                        Log.v(TAG, "Resolute ActiveExtraInfo" + cellularType + " CellRadio " + cellRadio);
                        if (cellularType.contains(cellRadio)) {
                            selectedCellInfoIdx = cellInfoIdx.get(i); //:(, let's take first satisfying the extra info
                            break;
                        }
                    }
                }
            }else{
                selectedCellInfoIdx = cellInfoIdx.get(0);
            }

            if(selectedCellInfoIdx != -1) {
                retval = cellInfos.get(selectedCellInfoIdx);
            }else{
                Log.v(TAG, "No Selected Cell");
            }
        }
        return  retval;
    }

    private void updateCellularInformation(List<MMCellInfo> cellInfos){
        if(cellInfos != null) {
            MMCellInfo cellInfo = getActiveNetworkConnection(cellInfos);
            if(cellInfo != null) {
                MMSmartStreaming.getInstance().reportCellularInformation(cellInfo);
            }else{
                Log.w(TAG, "Could not determine active cellular information");
            }
        }
    }

    private void updateLocationInformation(LocationInformation information){
        if (information != null){
            MMSmartStreaming.getInstance().reportLocation(information.latitude, information.longitude);
        }
    }

    private LocationInformation fetchLocationInformation(LocationManager locManager){
        LocationInformation information = null;
        if(locationFetchDisabled == false) {
            try {
                Location location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    information = new LocationInformation();
                    information.latitude = location.getLatitude();
                    information.longitude = location.getLongitude();
                }
            } catch (Exception e) {
                e.printStackTrace();
                locationFetchDisabled = true;
            }
        }
        return information;
    }

    @TargetApi(18)
    private List<MMCellInfo> fetchTelephonyInformation(TelephonyManager tm) {
        List<MMCellInfo> cells = null;
        try {
            final List<android.telephony.CellInfo> observed = tm.getAllCellInfo();
            if (observed == null || observed.isEmpty()) {
                Log.i(TAG, "getAllCellInfo - Could not get cell info...");
                List<NeighboringCellInfo> infos = tm.getNeighboringCellInfo();
                if(infos != null) {
                    for (android.telephony.NeighboringCellInfo cellinf : infos) {
                        Log.i(TAG, "Neighbouring Cell Info - " + cellinf.toString());
                    }
                }else{
                    Log.i(TAG, "getNeighboringCellInfo - Could not get cell info/neighbouring cell info...");
                }
                return Collections.emptyList();
            }

            cells = new ArrayList<MMCellInfo>(observed.size());
            for (android.telephony.CellInfo observedCell : observed) {
                if (!addCellToList(cells, observedCell, tm)) {
                    Log.i(TAG, "Skipped CellInfo of unknown class: " + observedCell.toString());
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            telephonyFetchDisabled = true;
        }
        return cells;
    }

    @TargetApi(18)
    protected boolean addCellToList(List<MMCellInfo> cells,
                                    android.telephony.CellInfo observedCell,
                                    TelephonyManager tm) {
        boolean added = false;
        if (observedCell instanceof CellInfoGsm) {
            CellIdentityGsm ident = ((CellInfoGsm) observedCell).getCellIdentity();
//            if (ident.getMcc() != Integer.MAX_VALUE && ident.getMnc() != Integer.MAX_VALUE) {
            CellSignalStrengthGsm strength = ((CellInfoGsm) observedCell).getCellSignalStrength();
            MMCellInfo cell = new MMCellInfo();
            cell.setGsmCellInfo(ident.getMcc(),
                    ident.getMnc(),
                    ident.getLac(),
                    ident.getCid(),
                    strength.getAsuLevel());
            cells.add(cell);
            added = true;
//            }
        } else if (observedCell instanceof CellInfoCdma) {
            MMCellInfo cell = new MMCellInfo();
            CellIdentityCdma ident = ((CellInfoCdma) observedCell).getCellIdentity();
            CellSignalStrengthCdma strength = ((CellInfoCdma) observedCell).getCellSignalStrength();
            cell.setCdmaCellInfo(ident.getBasestationId(),
                    ident.getNetworkId(),
                    ident.getSystemId(),
                    strength.getAsuLevel());
            cells.add(cell);
            added = true;
        } else if (observedCell instanceof CellInfoLte) {
            CellIdentityLte ident = ((CellInfoLte) observedCell).getCellIdentity();
//            if (ident.getMnc() != Integer.MAX_VALUE && ident.getMcc() != Integer.MAX_VALUE) {
            MMCellInfo cell = new MMCellInfo();
            CellSignalStrengthLte strength = ((CellInfoLte) observedCell).getCellSignalStrength();
            cell.setLteCellInfo(ident.getMcc(),
                    ident.getMnc(),
                    ident.getCi(),
                    ident.getPci(),
                    ident.getTac(),
                    strength.getAsuLevel(),
                    strength.getTimingAdvance());
            cells.add(cell);
            added = true;
//            }
        }else{
            Log.d(TAG, "Unknown Cell Info, Consider is WCDMA ...");
        }

        if (!added && Build.VERSION.SDK_INT >= 18) {
            added = addWCDMACellToList(cells, observedCell, tm);
        }

        return added;
    }

    public void updateNetworkInformation(Context ctx){
        MMConnectionInfo connType = MMConnectionInfo.NotReachable;
        if(networkStateReportingDisabled == false) {
            ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            TelephonyManager teleMan = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            int networkType = teleMan.getNetworkType();
            if (info != null && info.isConnected()) {
                activeNetworkInformation = info;
                Log.d(TAG, "ActiveNetworkInformation" + activeNetworkInformation);
                connType = MMConnectionInfo.Wifi;
                if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                    fetchTelephonyMetrics = true;
                    connType = getMobileConnType(networkType);
                    MMSmartStreaming.getInstance().reportNetworkType(connType);
                } else {
                    fetchTelephonyMetrics = false;
                    MMSmartStreaming.getInstance().reportNetworkType(connType);
                    updateWifiInformation(ctx);
                }
            }else{
                MMSmartStreaming.getInstance().reportNetworkType(connType);
            }
        }
    }

    @Override
    public void onReceive(final Context ctx, final Intent intent) {
        synchronized(TAG) {
            if (intent.getExtras() != null) {
                updateNetworkInformation(ctx);
            }
        }
    }

    public void setPollingFrequency(Integer frequency){
        if (frequency!=null){
            if(frequency == -1){
                networkInformationRetrievalPolicy = MMNetworkInformationRetrievalPolicy.RetrieveNetworkInformationNone;
            }
            else if(frequency == 0){
                networkInformationRetrievalPolicy = MMNetworkInformationRetrievalPolicy.RetrieveNetworkInformationOnce;
            }else{
                networkInformationRetrievalPolicy = MMNetworkInformationRetrievalPolicy.RetrieveNetworkInformationPolling;
                pollingFrequency = frequency;
            }
        }else{
            networkInformationRetrievalPolicy = MMNetworkInformationRetrievalPolicy.RetrieveNetworkInformationNone;
        }
        Log.i(TAG, "Network Info Polling Policy - " + networkInformationRetrievalPolicy);
    }

    private void resetPollingStatsRetrieval(){
        pollingFrequency = -1;
    }

    private void stopPolling(){
        cancelPolling();
        timer = null;
    }

    private void cancelPolling(){
        if(timer != null) {
            timer.cancel();
        }
    }

    @TargetApi(18)
    protected boolean addWCDMACellToList(List<MMCellInfo> cells,
                                         android.telephony.CellInfo observedCell,
                                         TelephonyManager tm) {
        boolean added = false;
        if (Build.VERSION.SDK_INT >= 18 &&
                observedCell instanceof CellInfoWcdma) {
            CellIdentityWcdma ident = ((CellInfoWcdma) observedCell).getCellIdentity();
            if (ident.getMnc() != Integer.MAX_VALUE && ident.getMcc() != Integer.MAX_VALUE) {
                MMCellInfo cell = new MMCellInfo();
                CellSignalStrengthWcdma strength = ((CellInfoWcdma) observedCell).getCellSignalStrength();
                cell.setWcdmaCellInfo(ident.getMcc(),
                        ident.getMnc(),
                        ident.getLac(),
                        ident.getCid(),
                        ident.getPsc(),
                        strength.getAsuLevel());
                cells.add(cell);
                added = true;
            }
        }
        return added;
    }

    private void getAndUpdateTelephonyInformation(Context context){
        if(fetchTelephonyMetrics) {
            try {
                if (Build.VERSION.SDK_INT >= 18) {
                    TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    List<MMCellInfo> cellInfos = fetchTelephonyInformation(tm);
                    if (cellInfos != null && cellInfos.size() > 0) {
                        updateCellularInformation(cellInfos);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void  getAndUpdateLocationInformation(Context context){
        if (locationFetchDisabled == false) {
            try {
                LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                LocationInformation info = fetchLocationInformation(lm);
                if (info != null) {
                    updateLocationInformation(info);
                }
            } catch (Exception e) {
                locationFetchDisabled = true;
            }
        }
    }

    private void startPolling(Context ctx){
        if (timer == null || (locationFetchDisabled && telephonyFetchDisabled)) {
            timer = new Timer();
        }

        final Context context = ctx;
        timer.scheduleAtFixedRate(new TimerTask() {
            synchronized public void run() {
                Log.v(TAG, "Polling Count => " + pollCount++);
                if (telephonyFetchDisabled == false) {
                    getAndUpdateTelephonyInformation(context);
                }

                if (locationFetchDisabled == false) {
                    getAndUpdateLocationInformation(context);
                }
            }
        }, 0, pollingFrequency);
    }

    private boolean retrieveInformation(Context ctx){
        boolean retval = false;
        if (activeNetworkInformation != null) {
            Log.v(TAG, "Active Network Information" + activeNetworkInformation.toString());
            if (networkInformationRetrievalPolicy == MMNetworkInformationRetrievalPolicy.RetrieveNetworkInformationPolling) {
                startPolling(ctx);
            } else if (networkInformationRetrievalPolicy == MMNetworkInformationRetrievalPolicy.RetrieveNetworkInformationOnce) {
                getAndUpdateTelephonyInformation(ctx);
                getAndUpdateLocationInformation(ctx);
            } else if (networkInformationRetrievalPolicy == MMNetworkInformationRetrievalPolicy.RetrieveNetworkInformationNone) {
                Log.w(TAG, "Policy to NOT TO Retrieve network information configured");
            } else {
                Log.w(TAG, "Policy to retrieve network information not configured");
            }
            retval = true;
        }else{
            Log.v(TAG, "retrieveInformation Active Network Information is NULL");
        }
        return retval;
    }

    private void updateWifiInformation(Context ctx){
        WifiManager wifiManager = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        if (wifiSignalReportingDisabled == false && wifiManager != null && wifiManager.getConnectionInfo()!= null) {
            int rssi = wifiManager.getConnectionInfo().getRssi();
            int level = WifiManager.calculateSignalLevel(rssi, 5);
            MMSmartStreaming.getInstance().reportWifiSSID(wifiManager.getConnectionInfo().getSSID());
            MMSmartStreaming.getInstance().reportWifiDataRate(wifiManager.getConnectionInfo().getLinkSpeed());
            MMSmartStreaming.getInstance().reportWifiSignalStrengthPercentage((level * 100.0)/5);
            Log.v(TAG, "Active Wifi Connection " + wifiManager.getConnectionInfo());
        }
    }
}
