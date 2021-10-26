package org.jenkinsci.plugins.schedulebuild;

import hudson.model.FreeStyleProject;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScheduleBuildActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    private ScheduleBuildAction scheduleBuildAction;
    private FreeStyleProject project;

    public ScheduleBuildActionTest() {
    }

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
        assertThat(scheduleBuildAction.getIconFileName(), is("/plugin/schedule-build/images/schedule.svg"));
        project.disable();
        assertThat(scheduleBuildAction.getIconFileName(), is(nullValue()));
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
}
