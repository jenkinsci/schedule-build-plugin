package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;

/**
 * User property to store schedule build timezone preferences.
 * Allows users to override the global timezone setting.
 */
public class ScheduleBuildUserProperty extends UserProperty {

    private static final Logger LOGGER = Logger.getLogger(ScheduleBuildUserProperty.class.getName());

    private String timeZone;

    @DataBoundConstructor
    public ScheduleBuildUserProperty() {
        // Default to null, which means use global setting
        this.timeZone = null;
    }

    public ScheduleBuildUserProperty(String timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Get the user's preferred timezone for schedule builds.
     * @return the timezone ID, or null if using global setting
     */
    public String getTimeZone() {
        return timeZone;
    }

    @DataBoundSetter
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
        // UserProperty is automatically saved when modified through the UI
    }

    /**
     * Get the ZoneId for the user's preferred timezone.
     * Falls back to global setting if user hasn't set a preference.
     * @return the ZoneId to use for schedule builds
     */
    public ZoneId getZoneId() {
        if (timeZone != null && !timeZone.trim().isEmpty()) {
            try {
                return ZoneId.of(timeZone);
            } catch (DateTimeException dte) {
                LOGGER.warning("Invalid timezone '" + timeZone + "' for user " + user.getId()
                        + ", falling back to global setting");
            }
        }
        // Fall back to global setting
        return ScheduleBuildGlobalConfiguration.get().getZoneId();
    }

    /**
     * Check if the user has set a custom timezone preference.
     * @return true if user has set a custom timezone, false otherwise
     */
    public boolean hasCustomTimeZone() {
        return timeZone != null && !timeZone.trim().isEmpty();
    }

    @Extension
    @Symbol("scheduleBuild")
    public static class DescriptorImpl extends UserPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.ScheduleBuildUserProperty_DisplayName();
        }

        @Override
        public boolean isEnabled() {
            return Jenkins.get().hasPermission(Jenkins.SYSTEM_READ);
        }

        @Override
        public UserProperty newInstance(User user) {
            return new ScheduleBuildUserProperty();
        }

        @RequirePOST
        public FormValidation doCheckTimeZone(@QueryParameter String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.ok(); // Empty is valid (use global setting)
            }

            try {
                ZoneId zone = ZoneId.of(value);
                if (StringUtils.equals(zone.getId(), value)) {
                    return FormValidation.ok();
                } else {
                    return FormValidation.error(Messages.ScheduleBuildUserProperty_TimeZoneError());
                }
            } catch (DateTimeException dte) {
                return FormValidation.error(Messages.ScheduleBuildUserProperty_TimeZoneError());
            }
        }

        @POST
        public ListBoxModel doFillTimeZoneItems() {
            ListBoxModel items = new ListBoxModel();

            // Add option to use global setting
            items.add(Messages.ScheduleBuildUserProperty_UseGlobalSetting(), "");

            // Add all available timezones
            Set<String> zoneIds = new TreeSet<>(ZoneId.getAvailableZoneIds());
            for (String id : zoneIds) {
                items.add(id);
            }

            return items;
        }
    }
}
