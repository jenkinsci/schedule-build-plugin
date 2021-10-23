package org.jenkinsci.plugins.schedulebuild;

import java.util.TimeZone;

import jenkins.model.GlobalConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class ScheduleBuildGlobalConfigurationTest {

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    public ScheduleBuildGlobalConfigurationTest() {
    }

    @Test
    public void configRoundTripTestNoChanges() throws Exception {
        ScheduleBuildGlobalConfiguration globalConfig =
            GlobalConfiguration.all().getInstance(ScheduleBuildGlobalConfiguration.class);
        assertThat(globalConfig, is(not(nullValue())));
        assertThat(globalConfig.getDefaultScheduleTime(), is("10:00:00 PM"));
        assertThat(globalConfig.getTimeZone(), is(TimeZone.getDefault().getID()));

        // Submit the global configuration page with no changes
        j.configRoundtrip();

        ScheduleBuildGlobalConfiguration newGlobalConfig =
            GlobalConfiguration.all().getInstance(ScheduleBuildGlobalConfiguration.class);
        assertThat(newGlobalConfig, is(not(nullValue())));
        assertThat(newGlobalConfig.getDefaultScheduleTime(), is("10:00:00 PM"));
        assertThat(newGlobalConfig.getTimeZone(), is(TimeZone.getDefault().getID()));
    }

    @Test
    public void configRoundTripTestWithChanges() throws Exception {
        ScheduleBuildGlobalConfiguration globalConfig =
            GlobalConfiguration.all().getInstance(ScheduleBuildGlobalConfiguration.class);

        // Adjust global configuration values
        String newScheduleTime = "1:23:45 PM";
        String newTimeZone = "Europe/Rome";
        globalConfig.setDefaultScheduleTime(newScheduleTime);
        globalConfig.setTimeZone(newTimeZone);

        // Submit the global configuration page, will not change adjusted values
        j.configRoundtrip();

        ScheduleBuildGlobalConfiguration newGlobalConfig =
            GlobalConfiguration.all().getInstance(ScheduleBuildGlobalConfiguration.class);
        assertThat(newGlobalConfig, is(not(nullValue())));
        assertThat(newGlobalConfig.getDefaultScheduleTime(), is(newScheduleTime));
        assertThat(newGlobalConfig.getTimeZone(), is(newTimeZone));
    }
}
