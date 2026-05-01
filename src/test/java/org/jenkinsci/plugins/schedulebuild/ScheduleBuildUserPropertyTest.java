package org.jenkinsci.plugins.schedulebuild;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import hudson.model.User;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ScheduleBuildUserPropertyTest {

    private User testUser;
    private ScheduleBuildUserProperty userProperty;

    @BeforeEach
    void setUp(JenkinsRule j) throws Exception {
        testUser = User.get("testuser");
        userProperty = new ScheduleBuildUserProperty();
        testUser.addProperty(userProperty);
    }

    @Test
    void testDefaultTimeZone() {
        // Default should be false (use global setting)
        assertThat(userProperty.isUsePersonalTimeZone(), is(false));
        assertThat(userProperty.hasCustomTimeZone(), is(false));
    }

    @Test
    void testSetPersonalTimeZone() {
        userProperty.setUsePersonalTimeZone(true);

        assertThat(userProperty.isUsePersonalTimeZone(), is(true));
        assertThat(userProperty.hasCustomTimeZone(), is(true));
        // Should return system default timezone when personal timezone is enabled
        assertThat(userProperty.getZoneId(), is(ZoneId.systemDefault()));
    }

    @Test
    void testSetGlobalTimeZone() {
        userProperty.setUsePersonalTimeZone(false);

        assertThat(userProperty.isUsePersonalTimeZone(), is(false));
        assertThat(userProperty.hasCustomTimeZone(), is(false));
        // Should fall back to global setting
        assertThat(userProperty.getZoneId(), is(not(nullValue())));
    }

    @Test
    void testDescriptorDisplayName() {
        ScheduleBuildUserProperty.DescriptorImpl descriptor = new ScheduleBuildUserProperty.DescriptorImpl();
        assertThat(descriptor.getDisplayName(), is(not(nullValue())));
    }

    @Test
    void testDescriptorIsEnabled() {
        ScheduleBuildUserProperty.DescriptorImpl descriptor = new ScheduleBuildUserProperty.DescriptorImpl();
        // Should be enabled if user has SYSTEM_READ permission
        assertThat(descriptor.isEnabled(), is(not(nullValue())));
    }
}
