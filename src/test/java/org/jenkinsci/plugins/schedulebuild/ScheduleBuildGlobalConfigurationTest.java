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
        // Test that setting valid timezones works correctly
        globalConfig.setTimeZone("Europe/Berlin");
        ZoneId timeZoneBefore = globalConfig.getZoneId();
        assertThat(timeZoneBefore.toString(), is("Europe/Berlin"));

        globalConfig.setTimeZone("America/New_York");
        ZoneId timeZoneAfter = globalConfig.getZoneId();
        assertThat(timeZoneAfter.toString(), is("America/New_York"));
        assertThat(timeZoneBefore, is(not(timeZoneAfter)));
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

    @Test
    void testSetTimeZoneClearsCacheWhenSame() {
        // Set initial timezone
        String timezone = "Europe/Berlin";
        globalConfig.setTimeZone(timezone);

        // Get ZoneId to populate cache
        ZoneId firstZoneId = globalConfig.getZoneId();
        assertThat(firstZoneId.toString(), is(timezone));

        // Set same timezone again - cache should be cleared regardless
        globalConfig.setTimeZone(timezone);

        // Get ZoneId again - should work correctly even though cache was cleared
        ZoneId secondZoneId = globalConfig.getZoneId();
        assertThat(secondZoneId.toString(), is(timezone));
        assertThat(secondZoneId, is(firstZoneId));
    }

    @Test
    void testSetTimeZoneClearsCacheWhenDifferent() {
        // Set initial timezone
        String initialTimezone = "Europe/Berlin";
        globalConfig.setTimeZone(initialTimezone);

        // Get ZoneId to populate cache
        ZoneId firstZoneId = globalConfig.getZoneId();
        assertThat(firstZoneId.toString(), is(initialTimezone));

        // Set different timezone - cache should be cleared
        String newTimezone = "America/New_York";
        globalConfig.setTimeZone(newTimezone);

        // Get ZoneId - should return new timezone
        ZoneId secondZoneId = globalConfig.getZoneId();
        assertThat(secondZoneId.toString(), is(newTimezone));
        assertThat(secondZoneId, is(not(firstZoneId)));
    }

    @Test
    void testSetTimeZoneClearsCacheOnValidChange() {
        // Set valid timezone first
        String validTimezone = "Europe/Berlin";
        globalConfig.setTimeZone(validTimezone);

        // Get ZoneId to populate cache
        ZoneId firstZoneId = globalConfig.getZoneId();
        assertThat(firstZoneId.toString(), is(validTimezone));

        // Set different valid timezone - cache should be cleared
        String newValidTimezone = "America/New_York";
        globalConfig.setTimeZone(newValidTimezone);

        // Get ZoneId - should return new timezone
        ZoneId secondZoneId = globalConfig.getZoneId();
        assertThat(secondZoneId.toString(), is(newValidTimezone));
        assertThat(secondZoneId, is(not(firstZoneId)));
    }

    @Test
    void testDoCheckTimeZoneWithNull() {
        // Form validation should return error for null timezone
        FormValidation result = globalConfig.doCheckTimeZone(null);
        assertThat(result.kind, is(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(), containsString("Timezone cannot be null, empty, or whitespace"));
    }

    @Test
    void testDoCheckTimeZoneWithEmptyString() {
        // Form validation should return error for empty timezone
        FormValidation result = globalConfig.doCheckTimeZone("");
        assertThat(result.kind, is(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(), containsString("Timezone cannot be null, empty, or whitespace"));
    }

    @Test
    void testDoCheckTimeZoneWithWhitespace() {
        // Form validation should return error for whitespace-only timezone
        FormValidation result = globalConfig.doCheckTimeZone("   ");
        assertThat(result.kind, is(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(), containsString("Timezone cannot be null, empty, or whitespace"));
    }

    @Test
    void testDoCheckTimeZoneWithInvalidTimezone() {
        // Form validation should return error for invalid timezone
        String invalidTimezone = "Invalid/Timezone";
        FormValidation result = globalConfig.doCheckTimeZone(invalidTimezone);
        assertThat(result.kind, is(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(), containsString("Invalid timezone: " + invalidTimezone));
    }

    @Test
    void testDoCheckTimeZoneWithValidTimezone() {
        // Form validation should return OK for valid timezone
        FormValidation result = globalConfig.doCheckTimeZone("Europe/Berlin");
        assertThat(result.kind, is(FormValidation.Kind.OK));
    }

    @Test
    void testSetTimeZoneTrimsWhitespace() {
        // Setting timezone with leading/trailing whitespace should trim it
        String timezoneWithWhitespace = "  Europe/Berlin  ";
        globalConfig.setTimeZone(timezoneWithWhitespace);

        // Verify it was trimmed
        assertThat(globalConfig.getTimeZone(), is("Europe/Berlin"));

        // Verify it works correctly
        ZoneId zoneId = globalConfig.getZoneId();
        assertThat(zoneId.toString(), is("Europe/Berlin"));
    }

    @Test
    void testGetZoneIdCacheHit() {
        // Set a timezone to populate cache
        String timezone = "Europe/Berlin";
        globalConfig.setTimeZone(timezone);

        // First call should populate cache
        ZoneId firstResult = globalConfig.getZoneId();
        assertThat(firstResult.toString(), is(timezone));

        // Second call should hit cache (line 139 coverage)
        ZoneId secondResult = globalConfig.getZoneId();
        assertThat(secondResult, is(firstResult));
        assertThat(secondResult.toString(), is(timezone));
    }

    @Test
    void testGetZoneIdCacheMissWhenZoneIdNull() {
        // Ensure we start with null cache by setting timezone
        globalConfig.setTimeZone("Europe/Berlin");

        // Clear cache by setting new timezone
        globalConfig.setTimeZone("America/New_York");

        // This should miss cache because cachedZoneId was cleared
        ZoneId result = globalConfig.getZoneId();
        assertThat(result.toString(), is("America/New_York"));
    }

    @Test
    void testGetZoneIdCacheMissWhenTimeZoneStringsDiffer() {
        // Set initial timezone and get ZoneId to populate cache
        globalConfig.setTimeZone("Europe/Berlin");
        ZoneId firstResult = globalConfig.getZoneId();

        // Manually change the timeZone field to create a mismatch scenario
        // This simulates the case where cachedZoneId != null but timeZone != cachedTimeZoneString
        globalConfig.setTimeZone("America/New_York");

        // This should miss cache because timeZone != cachedTimeZoneString
        ZoneId secondResult = globalConfig.getZoneId();
        assertThat(secondResult.toString(), is("America/New_York"));
        assertThat(secondResult, is(not(firstResult)));
    }
}
