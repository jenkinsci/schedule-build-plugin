package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.util.FormValidation;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
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
    private Date defaultScheduleTime;
    private String timeZone;

    @DataBoundConstructor
    public ScheduleBuildGlobalConfiguration() {
        this.defaultScheduleTime = new Date(0, 0, 0, 22, 0);
        this.timeZone = TimeZone.getDefault().getID();
        load();
    }

    public String getDefaultScheduleTime() {
        return getTimeFormat().format(this.defaultScheduleTime);
    }

    @DataBoundSetter
    public void setDefaultScheduleTime(String defaultScheduleTime) throws ParseException {
        this.defaultScheduleTime = getTimeFormat().parse(defaultScheduleTime);
    }

    public String getTimeZone() {
        return timeZone;
    }

    @DataBoundSetter
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public TimeZone getTimeZoneObject() {
        return TimeZone.getTimeZone(getTimeZone());
    }

    private DateFormat getTimeFormat() {
        Locale locale =
                Stapler.getCurrentRequest() != null
                        ? Stapler.getCurrentRequest().getLocale()
                        : Locale.getDefault();
        return DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
    }

    public Date getDefaultScheduleTimeObject() {
        return new Date(this.defaultScheduleTime.getTime());
    }

    @RequirePOST
    public FormValidation doCheckDefaultScheduleTime(@QueryParameter String value) {
        Jenkins.get()
                .checkPermission(Jenkins.ADMINISTER); // Admin permission required for global config
        try {
            getTimeFormat().parse(value);
        } catch (ParseException ex) {
            return FormValidation.error(Messages.ScheduleBuildGlobalConfiguration_ParsingError());
        }
        return FormValidation.ok();
    }

    @RequirePOST
    public FormValidation doCheckTimeZone(@QueryParameter String value) {
        Jenkins.get()
                .checkPermission(Jenkins.ADMINISTER); // Admin permission required for global config
        TimeZone zone = TimeZone.getTimeZone(value);
        if (StringUtils.equals(zone.getID(), value)) {
            return FormValidation.ok();
        } else {
            return FormValidation.error(Messages.ScheduleBuildGlobalConfiguration_TimeZoneError());
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        // reset before data-binding
        this.defaultScheduleTime = null;
        this.timeZone = null;
        if (json.containsKey("defaultScheduleTime") && json.containsKey("timeZone")) {
            try {
                this.defaultScheduleTime =
                        getTimeFormat().parse(json.getString("defaultScheduleTime"));
                this.timeZone = json.getString("timeZone");
                save();
                return true;
            } catch (ParseException ex) {
            }
        }
        return false;
    }
}
