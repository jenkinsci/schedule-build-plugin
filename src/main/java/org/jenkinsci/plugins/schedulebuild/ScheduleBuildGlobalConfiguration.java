package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;

@Extension
@Symbol("scheduleBuild")
public class ScheduleBuildGlobalConfiguration extends GlobalConfiguration {
    // defaultScheduleTime is a misuse of a Date object.  Used for the
    // time portion (hours, minutes, seconds, etc.) while the date
    // portion is ignored.
    private transient Date defaultScheduleTime;
    private String timeZone;

    private String defaultStartTime;

    private transient LocalTime defaultScheduleLocalTime;

    private static final Logger LOGGER = Logger.getLogger(ScheduleBuildGlobalConfiguration.class.getName());

    private static final String TIME_PATTERN = "HH:mm:ss";

    private static final LocalDate EPOCH = LocalDate.of(1970, 1, 1);
    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final DateTimeFormatter[] FORMATTERS = {
            DateTimeFormatter.ofPattern("H:m:s a"), // Original format required by DateFormat
            DateTimeFormatter.ofPattern("h:m:s a"),
            DateTimeFormatter.ofPattern("H:m a"),
            DateTimeFormatter.ofPattern("h:m a"),
            DateTimeFormatter.ofPattern("H:m:s"),
            DateTimeFormatter.ofPattern("h:m:s"),
            DateTimeFormatter.ofPattern("H:m"),
            DateTimeFormatter.ofPattern("h:m"),
    };


    @DataBoundConstructor
    public ScheduleBuildGlobalConfiguration() {
        this.timeZone = TimeZone.getDefault().getID();
        defaultStartTime = "22:00:00";
        LOGGER.log(Level.INFO, () -> "Default ZoneId: " + ZoneId.systemDefault());
        load();
        defaultScheduleLocalTime = LocalTime.parse(defaultStartTime, getTimeFormatter());
    }

    @Override
    public void load() {
        super.load();
        if (defaultScheduleTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(TIME_PATTERN);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            setDefaultStartTime(sdf.format(this.defaultScheduleTime));
        }
    }

    public String getDefaultStartTime() {
        return defaultStartTime;
    }

    @DataBoundSetter
    public void setDefaultStartTime(String defaultStartTime) {
        defaultScheduleLocalTime = LocalTime.parse(defaultStartTime, getTimeFormatter());
        this.defaultStartTime = defaultStartTime;
    }

    @DataBoundSetter
    public void setDefaultScheduleTime(String defaultScheduleTime) throws ParseException {
        try {
            this.defaultScheduleTime = getTimeFormat().parse(defaultScheduleTime);
        } catch (ParseException parseException) {
            /* Try each of the formatters with the user provided data, return first success */
            /* Java 21 changed DateFormat to not accept strings with only a time component */
            /* DateTimeFormatter parsing allows Java 11, 17, and 21 to accept several time string formats */
            for (DateTimeFormatter formatter : FORMATTERS) {
                try {
                    LocalTime localTime = LocalTime.parse(defaultScheduleTime, formatter);
                    Instant instant = localTime.atDate(EPOCH).atZone(ZONE).toInstant();
                    this.defaultScheduleTime = Date.from(instant);
                    // LOGGER.log(Level.FINEST, "Parsed '" + defaultScheduleTime + "' with formatter " + formatter);
                    return;
                } catch (DateTimeParseException dtex) {
                    LOGGER.log(
                            Level.FINE,
                            "Did not parse '" + defaultScheduleTime + "' with formatter " + formatter,
                            dtex);
                }
            }
            /* Throw the original exception if no match is found */
            throw parseException;
        } finally {
            SimpleDateFormat sdf = new SimpleDateFormat(TIME_PATTERN);
            sdf.setTimeZone(TimeZone.getTimeZone(ZONE));
            setDefaultStartTime(sdf.format(this.defaultScheduleTime));
        }
    }

    public String getTimeZone() {
        return timeZone;
    }

    @DataBoundSetter
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException dte) {
            return ZoneId.systemDefault();
        }
    }

    private DateFormat getTimeFormat() {
        Locale locale = Stapler.getCurrentRequest() != null
                ? Stapler.getCurrentRequest().getLocale()
                : Locale.getDefault();
        return DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
    }

    private DateTimeFormatter getTimeFormatter() {
        return DateTimeFormatter.ofPattern(TIME_PATTERN);
    }

    /**
     * Returns a ZonedDateTime object on the current date in the configured timezone.
     * @return scheduletime
     */
    public ZonedDateTime getDefaultScheduleTimeObject() {
        ZonedDateTime zdt = defaultScheduleLocalTime.atDate(LocalDate.now()).atZone(getZoneId());
        return zdt;
    }

    @RequirePOST
    public FormValidation doCheckDefaultStartTime(@QueryParameter String value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER); // Admin permission required for global config
        try {
            LocalTime.parse(value, getTimeFormatter());
        } catch (DateTimeParseException ex) {
            return FormValidation.error(Messages.ScheduleBuildGlobalConfiguration_ParsingError());
        }
        return FormValidation.ok();
    }

    @RequirePOST
    public FormValidation doCheckTimeZone(@QueryParameter String value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER); // Admin permission required for global config
        TimeZone zone = TimeZone.getTimeZone(value);
        if (StringUtils.equals(zone.getID(), value)) {
            return FormValidation.ok();
        } else {
            return FormValidation.error(Messages.ScheduleBuildGlobalConfiguration_TimeZoneError());
        }
    }

    public ListBoxModel doFillTimeZoneItems() {
        ListBoxModel items = new ListBoxModel();
        for (String id : TimeZone.getAvailableIDs()) {
            if (id.equalsIgnoreCase(timeZone)) {
                items.add(new ListBoxModel.Option(id, id, true));
            } else {
                items.add(id);
            }
        }
        return items;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        // reset before data-binding
        this.defaultScheduleTime = null;
        this.timeZone = null;
        if (json.containsKey("defaultStartTime") && json.containsKey("timeZone")) {
            setDefaultStartTime(json.getString("defaultStartTime"));
            this.timeZone = json.getString("timeZone");
            save();
            return true;
        }
        return false;
    }
}
