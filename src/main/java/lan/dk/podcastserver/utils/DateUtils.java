package lan.dk.podcastserver.utils;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DateUtils {

    public static Timestamp rfc2822DateToTimeStamp(String pubDate) throws ParseException {
        Date javaDate = null;
        try {
            String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            javaDate = format.parse(pubDate);
            Timestamp timeStampDate = new Timestamp(javaDate.getTime());
            return timeStampDate;
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }
    }

    public static Timestamp canalPlusDateToTimeStamp(String date, String heure) throws ParseException {
        Date javaDate = null;
        try {
            String pattern = "dd/MM/yyyy-HH:mm:ss";
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.FRANCE);
            javaDate = format.parse(date + "-" + heure);
            Timestamp timeStampDate = new Timestamp(javaDate.getTime());
            return timeStampDate;
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }
    }

    public static Timestamp youtubeDateToTimeStamp(String pubDate) throws ParseException{
        Date javaDate = null;
        try {
            String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; //2013-12-20T22:30:01.000Z
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            javaDate = format.parse(pubDate);
            Timestamp timeStampDate = new Timestamp(javaDate.getTime());
            return timeStampDate;
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }
    }

    public static Timestamp folderDateToTimestamp(String pubDate) throws ParseException {
        Date javaDate = null;
        try {
            String pattern = "yyyy-MM-dd"; //2013-12-20
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            javaDate = format.parse(pubDate);
            Timestamp timeStampDate = new Timestamp(javaDate.getTime());
            return timeStampDate;
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }
    }

    public static String TimeStampToRFC2822 (Timestamp timestamp) {
        return (timestamp != null)
                ? new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(timestamp)
                : new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(new Timestamp(new Date().getTime()));
    }

    public static Timestamp beInSportDateToTimeStamp(String beInSportDate) throws ParseException {
        // Format : Feb 17 2014, 10:26
        Date javaDate = null;
        try {
            String pattern = "MMM dd yyyy, HH:mm";
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            javaDate = format.parse(beInSportDate);
            Timestamp timeStampDate = new Timestamp(javaDate.getTime());
            return timeStampDate;
        } catch (ParseException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }
    }

    public static Timestamp jeuxVideoFrToTimeStamp(String text) throws ParseException {
        // Format : 21/02/2014
        Date javaDate = null;
        try {
            String pattern = "dd/MM/yyyy";
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            javaDate = format.parse(text);
            Timestamp timeStampDate = new Timestamp(javaDate.getTime());
            return timeStampDate;
        } catch (ParseException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }
    }

    public static Timestamp parleysToTimeStamp(String text) throws ParseException {
        // Format : Thu Jun 26 06:34:41 UTC 2014
        Date javaDate = null;
        try {
            String pattern = "EEE MMM dd HH:mm:ss z yyyy";
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            javaDate = format.parse(text);
            return new Timestamp(javaDate.getTime());
        } catch (ParseException e) {
            //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }
    }

    public static Date findDateNDateAgo(Integer ago) {
        Date today = new Date();
        Calendar cal = new GregorianCalendar();
        cal.setTime(today);
        cal.add(Calendar.DAY_OF_MONTH, -ago);
        return cal.getTime();
    }
}
