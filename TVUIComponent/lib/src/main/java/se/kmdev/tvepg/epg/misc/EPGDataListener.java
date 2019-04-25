package se.kmdev.tvepg.epg.misc;

import se.kmdev.tvepg.epg.EPG;
import se.kmdev.tvepg.epg.EPGData;

/**
 * Created by MVRM on 10/04/2017.
 */

public class EPGDataListener {

    private EPG epg;

    public EPGDataListener(EPG epg){

        this.epg = epg;
    }

    public void processData(EPGData data) {
        epg.setEPGData(data);
        epg.recalculateAndRedraw(null, false);
    }

}
