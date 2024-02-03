package org.jenkinsci.plugins.schedulebuild;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
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

    private static final DateTimeFormatter[] FORMATTERS = {
        DateTimeFormatter.ofPattern("H:m[:s]"), DateTimeFormatter.ofPattern("h:m[:s] a", Locale.US),
    };

    @DataBoundConstructor
    public ScheduleBuildGlobalConfiguration() {
        this.timeZone = TimeZone.getDefault().getID();
        defaultStartTime = "22:00:00";
        load();
        defaultScheduleLocalTime = LocalTime.parse(defaultStartTime, getTimeFormatter());
    }

    @Override
    @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "Written by xstream")
    public void load() {
        super.load();
        if (defaultScheduleTime != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(TIME_PATTERN);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // xstream serializes with UTC
            setDefaultStartTime(sdf.format(this.defaultScheduleTime));
        }
    }

    public String getDefaultScheduleTime() {
        return getDefaultStartTime();
    }

    public String getDefaultStartTime() {
        return defaultStartTime;
    }

    @DataBoundSetter
    public void setDefaultStartTime(String defaultStartTime) {
        defaultScheduleLocalTime = parseTime(defaultStartTime);
        this.defaultStartTime = defaultScheduleLocalTime.format(getTimeFormatter());
        save();
    }

    @DataBoundSetter
    public void setDefaultScheduleTime(String defaultScheduleTime) throws ParseException {
        setDefaultStartTime(defaultScheduleTime);
    }

    private LocalTime parseTime(String time) {
        /* Try each of the formatters with the user provided data, return first success */
        /* Java 21 changed DateFormat to not accept strings with only a time component */
        /* DateTimeFormatter parsing allows Java 11, 17, and 21 to accept several time string formats */
        DateTimeParseException exception = null;
        final String ftime = time.trim();
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalTime.parse(ftime, formatter);
            } catch (DateTimeParseException dtex) {
                exception = dtex;
                LOGGER.log(Level.FINE, dtex, () -> "Did not parse '" + ftime + "' with formatter " + formatter);
            }
        }
        throw exception;
    }

    public String getTimeZone() {
        return timeZone;
    }

    @DataBoundSetter
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
        save();
    }

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException dte) {
            return ZoneId.systemDefault();
        }
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
            parseTime(value);
        } catch (DateTimeParseException ex) {
            return FormValidation.error(Messages.ScheduleBuildGlobalConfiguration_ParsingError());
        }
        return FormValidation.ok();
    }

    @RequirePOST
    public FormValidation doCheckTimeZone(@QueryParameter String value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER); // Admin permission required for global config
        ZoneId zone = ZoneId.of(value);
        if (StringUtils.equals(zone.getId(), value)) {
            return FormValidation.ok();
        } else {
            return FormValidation.error(Messages.ScheduleBuildGlobalConfiguration_TimeZoneError());
        }
    }

    public ListBoxModel doFillTimeZoneItems() {
        ListBoxModel items = new ListBoxModel();
        Set<String> zoneIds = new TreeSet<>(ZoneId.getAvailableZoneIds());
        for (String id : zoneIds) {
            if (id.equalsIgnoreCase(timeZone)) {
                items.add(new ListBoxModel.Option(id, id, true));
            } else {
                items.add(id);
            }
        }
        return items;
    }
}
