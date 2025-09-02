package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.model.User;
import hudson.model.UserProperty;
import hudson.model.UserPropertyDescriptor;
import java.time.ZoneId;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * User property to store schedule build timezone preferences.
 * Allows users to choose between using Jenkins core personal timezone or global
 * timezone setting.
 */
public class ScheduleBuildUserProperty extends UserProperty {

    private boolean usePersonalTimeZone;

    @DataBoundConstructor
    public ScheduleBuildUserProperty() {
        // Default to false, which means use global setting
        this.usePersonalTimeZone = false;
    }

    public ScheduleBuildUserProperty(boolean usePersonalTimeZone) {
        this.usePersonalTimeZone = usePersonalTimeZone;
    }

    /**
     * Check if the user wants to use their personal timezone from Jenkins core.
     *
     * @return true if using personal timezone, false if using global setting
     */
    public boolean isUsePersonalTimeZone() {
        return usePersonalTimeZone;
    }

    @DataBoundSetter
    public void setUsePersonalTimeZone(boolean usePersonalTimeZone) {
        this.usePersonalTimeZone = usePersonalTimeZone;
        // UserProperty is automatically saved when modified through the UI
    }

    /**
     * Get the ZoneId for the user's preferred timezone.
     * Uses system default timezone if personal timezone is enabled, otherwise falls
     * back to global setting.
     *
     * @return the ZoneId to use for schedule builds
     */
    public ZoneId getZoneId() {
        if (usePersonalTimeZone) {
            // Use system default timezone as a proxy for user's personal timezone
            // This respects the user's system timezone setting
            return ZoneId.systemDefault();
        }
        // Fall back to global setting, or system default if global config is not
        // available
        try {
            return ScheduleBuildGlobalConfiguration.get().getZoneId();
        } catch (IllegalStateException e) {
            // Global configuration not available (e.g., during testing)
            return ZoneId.systemDefault();
        }
    }

    /**
     * Check if the user has enabled personal timezone preference.
     *
     * @return true if user wants to use personal timezone, false otherwise
     */
    public boolean hasCustomTimeZone() {
        return usePersonalTimeZone;
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
    }
}
