package org.jenkinsci.plugins.schedulebuild;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;

import hudson.model.User;
import hudson.util.FormValidation;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ScheduleBuildUserPropertyTest {

    private JenkinsRule j;
    private User testUser;
    private ScheduleBuildUserProperty userProperty;

    @BeforeEach
    void setUp(JenkinsRule j) throws Exception {
        this.j = j;
        testUser = User.get("testuser");
        userProperty = new ScheduleBuildUserProperty();
        testUser.addProperty(userProperty);
    }

    @Test
    void testDefaultTimeZone() {
        // Default should be null (use global setting)
        assertThat(userProperty.getTimeZone(), is(nullValue()));
        assertThat(userProperty.hasCustomTimeZone(), is(false));
    }

    @Test
    void testSetCustomTimeZone() {
        String customTimeZone = "Europe/Berlin";
        userProperty.setTimeZone(customTimeZone);
        
        assertThat(userProperty.getTimeZone(), is(customTimeZone));
        assertThat(userProperty.hasCustomTimeZone(), is(true));
        assertThat(userProperty.getZoneId(), is(ZoneId.of(customTimeZone)));
    }

    @Test
    void testSetEmptyTimeZone() {
        userProperty.setTimeZone("");
        
        assertThat(userProperty.getTimeZone(), is(""));
        assertThat(userProperty.hasCustomTimeZone(), is(false));
        // Should fall back to global setting
        assertThat(userProperty.getZoneId(), is(not(nullValue())));
    }

    @Test
    void testSetNullTimeZone() {
        userProperty.setTimeZone(null);
        
        assertThat(userProperty.getTimeZone(), is(nullValue()));
        assertThat(userProperty.hasCustomTimeZone(), is(false));
        // Should fall back to global setting
        assertThat(userProperty.getZoneId(), is(not(nullValue())));
    }

    @Test
    void testInvalidTimeZone() {
        String invalidTimeZone = "Invalid/Timezone";
        userProperty.setTimeZone(invalidTimeZone);
        
        // Should fall back to global setting even with invalid timezone
        assertThat(userProperty.getZoneId(), is(not(nullValue())));
    }

    @Test
    void testDescriptorValidation() {
        ScheduleBuildUserProperty.DescriptorImpl descriptor = new ScheduleBuildUserProperty.DescriptorImpl();
        
        // Valid timezone
        FormValidation validResult = descriptor.doCheckTimeZone("Europe/Berlin");
        assertThat(validResult.kind, is(FormValidation.Kind.OK));
        
        // Empty timezone (use global setting)
        FormValidation emptyResult = descriptor.doCheckTimeZone("");
        assertThat(emptyResult.kind, is(FormValidation.Kind.OK));
        
        // Invalid timezone
        FormValidation invalidResult = descriptor.doCheckTimeZone("Invalid/Timezone");
        assertThat(invalidResult.kind, is(FormValidation.Kind.ERROR));
    }

    @Test
    void testDescriptorTimeZoneItems() {
        ScheduleBuildUserProperty.DescriptorImpl descriptor = new ScheduleBuildUserProperty.DescriptorImpl();
        var items = descriptor.doFillTimeZoneItems();
        
        assertThat(items, is(not(nullValue())));
        assertThat(items.size(), is(greaterThan(0)));
        
        // First item should be "Use Global Setting"
        assertThat(items.get(0).name, is("Use Global Setting"));
        assertThat(items.get(0).value, is(""));
        
        // Should contain common timezones
        boolean hasEuropeBerlin = false;
        boolean hasUTC = false;
        for (var item : items) {
            if ("Europe/Berlin".equals(item.value)) {
                hasEuropeBerlin = true;
            }
            if ("UTC".equals(item.value)) {
                hasUTC = true;
            }
        }
        assertThat("Should contain Europe/Berlin", hasEuropeBerlin, is(true));
        assertThat("Should contain UTC", hasUTC, is(true));
    }
}
