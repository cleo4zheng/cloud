package com.cloud.cloudapi.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class DateHelper {
	
	/**
	 * get a Date Object by input String 
	 * @param datestr
	 * @return null if the String is invalid
	 */
	public static Date getDateByString(String datestr){
		SimpleDateFormat sdf= null;
		Date date=null;		
		if (datestr.indexOf("Z")<0){
			sdf= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		}else if(datestr.indexOf(".")>-1){		
			sdf= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		}else{
			sdf= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");			
		}

		try {
//			System.out.println(sdf.toPattern());
			date= sdf.parse(datestr);
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			Logger log = LogManager.getLogger(DateHelper.class);
			log.error(e);
		}		
		return date;
	}
	
   /**
    * 把日期类型(java.util.Date)转化为Long值-即 1970 年 1 月 1 日 00:00:00 GMT）以来的指定毫秒数。
    * @param date
    * @return
    */
   public static long getLongByDate(Date date){	   
	   return date.getTime();
   }
   
   /**
    * 把日期类型字符串转化为Long值-即 1970 年 1 月 1 日 00:00:00 GMT）以来的指定毫秒数。
    * @param datestr
    * @return
    */
   public static long getLongByStrForOs(String datestr){
	   return getLongByDate(getDateByString(datestr));
   }
   

   
    /**
     * date object to int
     * @param date  Calendar
     * @return int value
     */
    public static int DateToInt(Calendar date){
        return (int) (date.getTimeInMillis() / 1000);
    }

    public static long StrToIntDateHour(String time)
    {

        String strtime=time.substring(0,4)+"-"+time.substring(4,6)+"-"+time.substring(6,8)+" "+time.substring(9,11)+":00:00";
        return StrToLongDateFull(strtime);
    }

    /**
     *    datestring to longvalue
     * @param date string yyyy-MM-dd HH:mm:ss
     * @return long value
     */
    public static long StrToLongDateFull(String date){
       long intdate= Timestamp.valueOf(date).getTime() / 1000;
       return intdate;
    }

    /**
     * dateint vlue to datestring day
     * @param dateInt
     * @return datestring yyyy/MM/dd
     */
    public static String intToDateStrDay(int dateInt) {
        Calendar time = Calendar.getInstance();
        Long l = new Long(Integer.toString(dateInt));
        time.setTimeInMillis(l.longValue() * 1000);
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd ");
        String timestring=df.format(time.getTime());
        return timestring;
    }
    /**
     * dateint vlue to datestring day
     * @param dateInt
     * @return datestring yyyy-MM-dd
     */
    public static String intToDateStrDay2(int dateInt) {
        Calendar time = Calendar.getInstance();
        Long l = new Long(Integer.toString(dateInt));
        time.setTimeInMillis(l.longValue() * 1000);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd ");
        String timestring=df.format(time.getTime());
        return timestring;
    }

    /**
     * dateint to datestring secend
     * @param dateInt
     * @return  datestring yyyy-MM-dd HH:mm:ss
     */
     public static String intToDateTimeFull(int dateInt) {
        Calendar time = Calendar.getInstance();
        Long l = new Long(Integer.toString(dateInt));
        time.setTimeInMillis(l.longValue() * 1000);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestring=df.format(time.getTime());
        return timestring;
    }
    /**
     * dateint to datestring secend
     * @param dateInt
     * @return  datestring yyyy-MM-dd HH:mm:ss
     */
     public static String intToDateTimeFull2(int dateInt) {
        Calendar time = Calendar.getInstance();
        Long l = new Long(Integer.toString(dateInt));
        time.setTimeInMillis(l.longValue() * 1000);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestring=df.format(time.getTime());
        return timestring;
    }

    /**
     * inttime to stringtime HH:mm:ss
     * @param dateInt
     * @return shorttime string HH:mm:ss
     */
    public static String intToDateTimeShort(int dateInt) {
        Calendar time = Calendar.getInstance();
        Long l = new Long(Integer.toString(dateInt));
        time.setTimeInMillis(l.longValue() * 1000);
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        String timestring=df.format(time.getTime());
        return timestring;
    }

    /**
     * intdate to Date Object
     * @param dateInt secend
     * @return dateObject
     */
    public static Date intToDateObject(int dateInt){
        Calendar time = Calendar.getInstance();
        Long l = new Long(Integer.toString(dateInt));
        time.setTimeInMillis(l.longValue() * 1000);
        return time.getTime();
    }
	
    public static Timestamp longToTimestamp(long timelong){
       return new Timestamp(timelong);  	
    }
    
    public static String longToStrByFormat(long timelong,String format){
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timelong);
        SimpleDateFormat df = new SimpleDateFormat(format);
        String timestring=df.format(time.getTime());
        return timestring; 	    	
    	
    }
    
    public static String longToStr(long timelong){

        return longToStrByFormat(timelong,"yyyy-MM-dd HH:mm:ss"); 	    	
    	
    }
    
    /**
     * 把分钟转化为毫秒数 1分=60*1000 毫秒
     * @param minute
     * @return
     */
    public static long getMillisecondByMinute(int minute){

    	return minute*60*1000;
    }

    /**
     * 把小时转化为毫秒数 1小时=60*60*100 毫秒
     * @param minute
     * @return
     */
    public static long getMillisecondByHour(int hour){
    	return hour*3600*1000;
    }   
    
    public static String timestampToStrByFormat(Timestamp tstamp,String format){
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(tstamp.getTime());
        SimpleDateFormat df = new SimpleDateFormat(format);
        String timestring=df.format(time.getTime());
        return timestring; 
    }
    
    public static String timestampToStrSeconds(Timestamp tstamp){
        return timestampToStrByFormat(tstamp,"yyyy-MM-dd HH:mm:ss"); 
    }
    
    public static void getCurrentTimeZone(){
    	Calendar cal = Calendar.getInstance();
    	TimeZone timeZone = cal.getTimeZone();
    	System.out.println(timeZone.getID());
    	System.out.println(timeZone.getDisplayName());
    }
    
    /**
     * 把输入时间转为标准UTC0时区的时间
     * @param time 要转化的时间
     * @param utctimezong 要转化的时间的现在时区
     * @return
     */
//    public static long changTimeToUTC0(long time,String utctimezong){   
// 	  return 0; 
//    }   
    
    public static long getNowTimeUTC0(){  
    	long nowtime= System.currentTimeMillis();
		TimeZone timeZone = TimeZone.getDefault();
		int offset=timeZone.getOffset(nowtime);
		return nowtime-offset;
    } 
    
    public static long changLocalTimeToUTC0(long time){   
		TimeZone timeZone = TimeZone.getDefault();
		int offset=timeZone.getOffset(time);
		return time-offset;
    } 
    
    
    public static long getNowTimeSecond(){
    	long nowtime= System.currentTimeMillis();
    	return nowtime/1000;
    }
    

}
