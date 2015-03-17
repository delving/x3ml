/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gr.forth;

/**
 *
 * @author konsolak
 */
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilsTime {

    private static Pattern pattern;
    private static Matcher matcher;
    private static final String swedishMonths[] = {"Januari", "Februari", "Mars", "April", "Maj", "Juni", "Juli", "Augusti", "September", "Oktober", "November", "December"};
    private static final String englishMonths[] = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    private static final String englishMonthsAbbr[] = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private static final Date ONE_BCE = new Date(-62167392000000L);
    private static final Date ONE_CE = new Date(-62135769600000L);

    private static final String DATE_PATTERNS[] = {
        "(0?[1-9]|[12][0-9]|3[01])([-/.])(0?[1-9]|1[012])\\2(-?\\d{1,4})([\\s]([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])(:[0-5][0-9])?)?", /*dd/mm/(-)yy | dd-mm-(-)yy | dd.mm.(-)yy* (optional time hh:mm:?sec)*/
        "(0?[1-9]|[12][0-9]|3[01])([-/.\\s])(Januari|Februari|Mars|April|Maj|Juni|Juli|Augusti|September|Oktober|November|December)\\2(-?\\d{1,4})([\\s]([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])(:[0-5][0-9])?)?", /*dd (month in swedish) yy (optional time hh:mm:?sec)*/
        "(0?[1-9]|[12][0-9]|3[01])([-/.\\s])(January|February|March|April|May|June|July|August|September|October|November|December)\\2(-?\\d{1,4})([\\s]([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])(:[0-5][0-9])?)?", /*dd (month in english) yy (optional time hh:mm:?sec)*/
        "(0?[1-9]|[12][0-9]|3[01])([-/.\\s])(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\2(-?\\d{1,4})([\\s]([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])(:[0-5][0-9])?)?", /*dd (month three letters in english) yy (optional time hh:mm:?sec)*/
        "(Januari|Februari|Mars|April|Maj|Juni|Juli|Augusti|September|Oktober|November|December)([-/.\\s])(-?\\d{1,4})", /*(month in swedish) yy*/
        "(January|February|March|April|May|June|July|August|September|October|November|December)([-/.\\s])(-?\\d{1,4})", /*(month in english) yy*/
        "(-?\\d{1,4})([-/.])(0?[1-9]|1[012])\\2(0?[1-9]|[12][0-9]|3[01])([\\s]([0-9]|0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])(:[0-5][0-9])?)?$", /*(-)yy-mm-dd |(-)yy.mm.dd | (-)yy/mm/dd  (optional time hh:mm:?sec)*/
        "(-?\\d{1,4})", /*(-)year */
        "(-?\\d{1,4})([-/.])(0?[1-9]|1[012])" /*(-)year/mm */};

    /**
     * Validate date format with regular expression
     *
     * @param date date address for validation
     * @return true valid date fromat, false invalid date format
     */
    public static Date validate(String date, String bound) {
        Date formatDate = null;
        String month = "";
        String day = "";
        int year = 0;
        int hour = -1;
        int min = -1;
        int sec = -1;

        boolean isValid = false;
        for (int i = 0; i < DATE_PATTERNS.length; i++) {
            String DATE_PATTERN = DATE_PATTERNS[i];
            pattern = Pattern.compile(DATE_PATTERN);
            matcher = pattern.matcher(date);

            if (matcher.matches()) {
                matcher.reset();

                if (matcher.find()) {
                    isValid = true;
                    String g1 = "";
                    String g2 = "";
                    String g3 = "";
                    String g4 = "";

                    if (matcher.group(1) != null) {
                        g1 = matcher.group(1);
                    }
                    try {
                        if (matcher.group(3) != null) {
                            g2 = matcher.group(3);
                        }

                        if (matcher.group(4) != null) {
                            g3 = matcher.group(4);
                        }
                        if (matcher.group(5) != null) {
                            g4 = matcher.group(5);
                            g4 = g4.replaceAll("\\s+", "");
                            hour = Integer.parseInt(g4.split(":")[0]);
                            min = Integer.parseInt(g4.split(":")[1]);
                            if (g4.split(":").length == 3) {
                                sec = Integer.parseInt(g4.split(":")[2]);
                            }
                        }
                    } catch (java.lang.IndexOutOfBoundsException e) {

                    }

                    if (i <= 3) {
                        year = Integer.parseInt(g3);
                        month = g2;
                        day = g1;

                    } else if (i >= 4 && i <= 5) {
                        year = Integer.parseInt(g2);
                        month = g1;
                    } else {
                        if (matcher.group(1) != null) {
                            year = Integer.parseInt(g1);
                            month = g2;
                            day = g3;
                        }
                    }

                    if (day.equals("31")
                            && (month.equals("4") || month.equals("6") || month.equals("9")
                            || month.equals("11") || month.equals("04") || month.equals("06")
                            || month.equals("09"))) {
                        isValid = false; // only 1,3,5,7,8,10,12 has 31 days
                    } else if (month.equals("2") || month.equals("02")) {
                        //leap year
                        if (year % 4 == 0) {
                            if (day.equals("30") || day.equals("31")) {
                                isValid = false;
                            }
                        } else {
                            if (day.equals("29") || day.equals("30") || day.equals("31")) {
                                isValid = false;
                            }
                        }
                    }
                    if (isValid) {
                        break;
                    }
                } else {
                    isValid = false;
                }
            }
        }

        if (isValid) {

            int index = Arrays.asList(swedishMonths).indexOf(month);
            if (index == -1) {
                index = Arrays.asList(englishMonths).indexOf(month);
                if (index == -1) {
                    index = Arrays.asList(englishMonthsAbbr).indexOf(month);
                }
            }

            if (index != -1) {
                month = "0" + String.valueOf(index + 1);
            }
            if (bound.equals("Lower")) {
                formatDate = getLowerDate(year, month, day, hour, min, sec);
            } else {
                formatDate = getUpperDate(year, month, day, hour, min, sec);
            }
        }
        return formatDate;
    }

    private static Date getUpperDate(int year, String month, String day, int hour, int min, int sec) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        if (day.equals("") && !month.equals("")) {
            cal.set(Calendar.MONTH, Integer.parseInt(month) - 1);
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.DAY_OF_MONTH, 1);// This is necessary to get proper results
            cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
        } else {

            if (month.equals("") && day.equals("")) {
                month = "12";
                day = "31";

            }
            cal.set(Calendar.MONTH, Integer.parseInt(month) - 1);
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));// This is necessary to get proper results
        }
        if (hour != -1) {
            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.set(Calendar.MINUTE, min);
            if (sec != -1) {
                cal.set(Calendar.SECOND, sec);
            } else {
                cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
            }
        } else {
            cal.set(Calendar.HOUR_OF_DAY, cal.getMaximum(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, cal.getMaximum(Calendar.MINUTE));
            cal.set(Calendar.SECOND, cal.getMaximum(Calendar.SECOND));
        }
        return cal.getTime();
    }

    private static Date getLowerDate(int year, String month, String day, int hour, int min, int sec) {

        if (month.equals("") && day.equals("")) {
            month = "01";
            day = "01";
        } else if (day.equals("")) {
            day = "01";
        }
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, Integer.parseInt(month) - 1); //months start from zero!
        c.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
        if (hour != -1) {
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, min);
            if (sec != -1) {
                c.set(Calendar.SECOND, sec);
            } else {
                c.set(Calendar.SECOND, c.getActualMinimum(Calendar.SECOND));
            }
        } else {
            c.set(Calendar.HOUR_OF_DAY, c.getActualMinimum(Calendar.HOUR_OF_DAY));
            c.set(Calendar.MINUTE, c.getActualMinimum(Calendar.MINUTE));
            c.set(Calendar.SECOND, c.getActualMinimum(Calendar.SECOND));
        }
        return c.getTime();
    }

    public static String convertDateToString(Date date, boolean millis) {
        if (date == null) {
            return null;
        } else {
            DateFormat df;
            if (millis) {
                df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            } else {
                df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            }
            df.setTimeZone(TimeZone.getTimeZone("GMT"));

            if (date.before(ONE_CE)) {
                StringBuilder sb = new StringBuilder(df.format(date));
                sb.insert(0, "-");
                return sb.toString();
            } else {
                return df.format(date);
            }
        }
    }

    public static String convertStringoXSDString(Date date) {
        if (date == null) {
            return null;
        }
        StringBuilder lexicalForm;
        String dateTime = convertDateToString(date, false);
        int len = dateTime.length() - 1;
        if (dateTime.indexOf('.', len - 4) != -1) {
            while (dateTime.charAt(len - 1) == '0') {
                len--; // fractional seconds may not with '0'.
            }
            if (dateTime.charAt(len - 1) == '.') {
                len--;
            }
            lexicalForm = new StringBuilder(dateTime.substring(0, len));
            lexicalForm.append('Z');
        } else {
            lexicalForm = new StringBuilder(dateTime);
        }

        if (date.before(ONE_CE)) {
            DateFormat df = new SimpleDateFormat("yyyy");
            df.setTimeZone(TimeZone.getTimeZone("GMT"));
            StringBuilder year
                    = new StringBuilder(String.valueOf(Integer.parseInt(df
                                            .format(date)) - 1));
            while (year.length() < 4) {
                year.insert(0, '0');
            }
            lexicalForm
                    .replace(0, lexicalForm.indexOf("-", 4), year.toString());
            if (date.before(ONE_BCE)) {
                lexicalForm.insert(0, "-");
            }
        }
        return lexicalForm.toString();
    }

}
