package org.jenkinsci.plugins.schedulebuild;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleProject;
import java.io.IOException;
import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ScheduleBuildActionTest {

    @Rule public JenkinsRule j = new JenkinsRule();
    private ScheduleBuildAction scheduleBuildAction;
    private FreeStyleProject project;

    public ScheduleBuildActionTest() {}

    @Before
    public void setUp() throws IOException {
        project = j.createFreeStyleProject();
        scheduleBuildAction = new ScheduleBuildAction(project);
    }

    @Test
    public void testGetOwner() {
        assertThat(scheduleBuildAction.getOwner(), is(project));
    }

    @Test
    public void testGetIconFileName() throws Exception {
        assertThat(scheduleBuildAction.getIconFileName(), is(nullValue()));
        project.disable();
        assertThat(scheduleBuildAction.getIconFileName(), is(nullValue()));
    }

    @Test
    public void testGetIconClassName() throws Exception {
        assertThat(
                scheduleBuildAction.getIconClassName(),
                is("symbol-calendar-outline plugin-ionicons-api"));
        project.disable();
        assertThat(scheduleBuildAction.getIconClassName(), is(nullValue()));
    }

    @Test
    public void testGetDisplayName() throws Exception {
        assertThat(scheduleBuildAction.getDisplayName(), is("Schedule Build"));
        project.disable();
        assertThat(scheduleBuildAction.getDisplayName(), is(nullValue()));
    }

    @Test
    public void testGetUrlName() {
        assertThat(scheduleBuildAction.getUrlName(), is("schedule"));
    }

    @Test
    public void testGetTarget() {
        assertThat(scheduleBuildAction.getTarget(), is(scheduleBuildAction));
    }

    @Test
    public void testGetIconPath() {
        assertThat(scheduleBuildAction.getIconPath(), endsWith("/plugin/schedule-build/"));
    }

    @Test
    public void testGetQuietPeriodInSeconds() {
        assertThat(scheduleBuildAction.getQuietPeriodInSeconds(), is(0L));
    }

    @Test
    public void testIsJobParameterized() {
        assertFalse(scheduleBuildAction.isJobParameterized());
    }

    @Test
    public void testSchedule() throws Exception {
        assertTrue(scheduleBuildAction.schedule(null, null));
    }

    @Test
    public void testGetDefaultDate() throws Exception {
        assertThat(scheduleBuildAction.getDefaultDate(), endsWith(" 10:00:00 PM"));
    }

    @Test
    public void testGetDefaultDateObject() throws Exception {
        Date now = new Date();
        Date defaultDate = scheduleBuildAction.getDefaultDateObject();
        assertThat("Default build date is not after current time", defaultDate.after(now));
    }
}
