package org.jenkinsci.plugins.schedulebuild;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.util.FormValidation;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ScheduleBuildGlobalConfigurationTest {

    private JenkinsRule j;

    private ScheduleBuildGlobalConfiguration globalConfig;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
        globalConfig = ScheduleBuildGlobalConfiguration.get();
    }

    @Test
    void configRoundTripTestNoChanges() throws Exception {
        assertThat(globalConfig, is(not(nullValue())));
        assertThat(globalConfig.getDefaultStartTime(), is("22:00:00"));
        assertThat(globalConfig.getTimeZone(), is(TimeZone.getDefault().getID()));

        // Submit the global configuration page with no changes
        j.configRoundtrip();

        ScheduleBuildGlobalConfiguration newGlobalConfig = ScheduleBuildGlobalConfiguration.get();
        assertThat(newGlobalConfig, is(not(nullValue())));
        assertThat(newGlobalConfig.getDefaultStartTime(), is("22:00:00"));
        assertThat(newGlobalConfig.getTimeZone(), is(TimeZone.getDefault().getID()));
    }

    @Test
    void configRoundTripTestWithChanges() throws Exception {
        // Adjust global configuration values
        String newScheduleTime = "1:23:45 PM";
        String newTimeZone = "Europe/Rome";
        globalConfig.setDefaultStartTime(newScheduleTime);
        globalConfig.setTimeZone(newTimeZone);

        // Submit the global configuration page, will not change adjusted values
        j.configRoundtrip();

        ScheduleBuildGlobalConfiguration newGlobalConfig = ScheduleBuildGlobalConfiguration.get();
        assertThat(newGlobalConfig, is(not(nullValue())));
        assertThat(newGlobalConfig.getDefaultStartTime(), is("13:23:45"));
        assertThat(newGlobalConfig.getTimeZone(), is(newTimeZone));
    }

    @Test
    void configRoundTripTestWithChangesSimpleTime() throws Exception {
        // Adjust global configuration values
        String newScheduleTime = "2:34";
        String newTimeZone = "Europe/Rome";
        globalConfig.setDefaultStartTime(newScheduleTime);
        globalConfig.setTimeZone(newTimeZone);

        // Submit the global configuration page, will not change adjusted values
        j.configRoundtrip();

        ScheduleBuildGlobalConfiguration newGlobalConfig = ScheduleBuildGlobalConfiguration.get();
        assertThat(newGlobalConfig, is(not(nullValue())));
        assertThat(newGlobalConfig.getDefaultStartTime(), is("02:34:00"));
        assertThat(newGlobalConfig.getTimeZone(), is(newTimeZone));
    }

    @Test
    void testBadScheduleTime() {
        assertThrows(DateTimeParseException.class, () -> globalConfig.setDefaultStartTime("34:56:78"));
    }

    @Test
    void testDoCheckTimeZone() {
        globalConfig.setTimeZone("invalid time zone");
        ZoneId timeZoneBefore = globalConfig.getZoneId();
        globalConfig.setTimeZone("Another invalid time zone");
        ZoneId timeZoneAfter = globalConfig.getZoneId();
        assertThat(timeZoneBefore, is(timeZoneAfter));
    }

    @Test
    void testGetDefaultStartTime() {
        assertThat(globalConfig.getDefaultStartTime(), is("22:00:00"));
    }

    @Test
    void testSetDefaultStartTime() {
        String defaultScheduleTime = "12:34:56";
        globalConfig.setDefaultStartTime(defaultScheduleTime);
        assertThat(globalConfig.getDefaultStartTime(), is("12:34:56"));
    }

    @Test
    void testDoCheckDefaultStartTimeBad() {
        FormValidation validation = globalConfig.doCheckDefaultStartTime("25:34:56");
        assertThat(validation.kind, is(FormValidation.Kind.ERROR));
        assertThat(validation.getMessage(), containsString("Not a valid build time"));
    }

    @Test
    void testGetTimeZone() {
        assertThat(globalConfig.getTimeZone(), is(ZoneId.systemDefault().toString()));
    }

    @Test
    void testSetTimeZone() {
        String timeZone = "Europe/Berlin";
        globalConfig.setTimeZone(timeZone);
        assertThat(globalConfig.getTimeZone(), is(timeZone));
    }

    @Test
    void testGetTimeZoneObject() {
        assertThat(globalConfig.getZoneId(), is(ZoneId.systemDefault()));
    }

    @Test
    void testGetDefaultScheduleTimeObject() {
        ZonedDateTime zdt = globalConfig.getDefaultScheduleTimeObject();
        assertThat(zdt.getHour(), is(22));
        assertThat(zdt.getMinute(), is(0));
        assertThat(zdt.getSecond(), is(0));
    }
}
