package org.jenkinsci.plugins.schedulebuild;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertThrows;

import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import jenkins.model.GlobalConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ScheduleBuildGlobalConfigurationTest {

    @Rule
    public final JenkinsRule j = new JenkinsRule();

    private ScheduleBuildGlobalConfiguration globalConfig = null;

    public ScheduleBuildGlobalConfigurationTest() {}

    @Before
    public void setUp() {
        globalConfig = GlobalConfiguration.all().getInstance(ScheduleBuildGlobalConfiguration.class);
    }

    @Test
    public void configRoundTripTestNoChanges() throws Exception {
        assertThat(globalConfig, is(not(nullValue())));
        assertThat(globalConfig.getDefaultScheduleTime(), matchesPattern("10:00:00\\hPM"));
        assertThat(globalConfig.getTimeZone(), is(TimeZone.getDefault().getID()));

        // Submit the global configuration page with no changes
        j.configRoundtrip();

        ScheduleBuildGlobalConfiguration newGlobalConfig =
                GlobalConfiguration.all().getInstance(ScheduleBuildGlobalConfiguration.class);
        assertThat(newGlobalConfig, is(not(nullValue())));
        assertThat(newGlobalConfig.getDefaultScheduleTime(), matchesPattern("10:00:00\\hPM"));
        assertThat(newGlobalConfig.getTimeZone(), is(TimeZone.getDefault().getID()));
    }

    @Test
    public void configRoundTripTestWithChanges() throws Exception {
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
        assertThat(newGlobalConfig.getDefaultScheduleTime(), matchesPattern("1:23:45\\hPM"));
        assertThat(newGlobalConfig.getTimeZone(), is(newTimeZone));
    }

    @Test
    public void testBadScheduleTime() throws Exception {
        assertThrows(java.text.ParseException.class, () -> {
            globalConfig.setDefaultScheduleTime("34:56:78");
        });
    }

    @Test
    public void testDoCheckTimeZone() throws Exception {
        globalConfig.setTimeZone("invalid time zone");
        TimeZone timeZoneBefore = globalConfig.getTimeZoneObject();
        globalConfig.setTimeZone("Another invalid time zone");
        TimeZone timeZoneAfter = globalConfig.getTimeZoneObject();
        assertThat(timeZoneBefore, is(timeZoneAfter));
    }

    @Test
    public void testGetDefaultScheduleTime() {
        assertThat(globalConfig.getDefaultScheduleTime(), matchesPattern("10:00:00\\hPM"));
    }

    @Test
    public void testSetDefaultScheduleTime() throws Exception {
        String defaultScheduleTime = "12:34:56 PM";
        globalConfig.setDefaultScheduleTime(defaultScheduleTime);
        assertThat(globalConfig.getDefaultScheduleTime(), matchesPattern("12:34:56\\hPM"));
    }

    @Test
    public void testGetTimeZone() {
        assertThat(globalConfig.getTimeZone(), is(ZoneId.systemDefault().toString()));
    }

    @Test
    public void testSetTimeZone() {
        String timeZone = "Europe/Berlin";
        globalConfig.setTimeZone(timeZone);
        assertThat(globalConfig.getTimeZone(), is(timeZone));
    }

    @Test
    public void testGetTimeZoneObject() {
        assertThat(globalConfig.getTimeZoneObject(), is(TimeZone.getDefault()));
    }

    @Test
    public void testGetDefaultScheduleTimeObject() {
        Date expectedDate = new Date(0, 0, 0, 22, 0);
        assertThat(globalConfig.getDefaultScheduleTimeObject(), is(expectedDate));
    }
}
