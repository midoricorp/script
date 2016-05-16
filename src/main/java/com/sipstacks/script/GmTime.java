package com.sipstacks.script;

import org.json.simple.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by torrey on 16/05/16.
 */
public class GmTime  extends Function implements Cloneable {
    public GmTime() { name="gmtime";}

    public Object eval() throws ScriptParseException {
        super.eval();
        List<Object> objs = getParamObjects();
        Date date;
        Calendar calendar;

        if (objs.size() == 0) {
            date = new Date();
        } else {
            date = new Date(Long.parseLong(objs.get(0).toString())*1000);
        }
        calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(date);

        JSONObject result = new JSONObject();

        result.put("TIMESTAMP", date.getTime()/1000);
        result.put("YEAR",calendar.get(Calendar.YEAR));
        result.put("MONTH",calendar.get(Calendar.MONTH)+1);
        result.put("DAY", calendar.get(Calendar.DAY_OF_MONTH));
        int hour = calendar.get(Calendar.HOUR);
        if(hour == 0) {
            hour=12;
        }
        result.put("HOUR", hour);
        result.put("AM_PM", calendar.get(Calendar.AM_PM)==0?"AM":"PM");
        result.put("HOUR_24", calendar.get(Calendar.HOUR_OF_DAY));
        result.put("MINUTE", calendar.get(Calendar.MINUTE));
        result.put("SECOND", calendar.get(Calendar.SECOND));
        result.put("WEEK_OF_YEAR", calendar.get(Calendar.WEEK_OF_YEAR)-1);
        return result;

    }

    public Function clone() {
        return new GmTime();
    }

}
