package org.jenkinsci.plugins.schedulebuild;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.ForwardToView;
import org.kohsuke.stapler.HttpRedirect;
import org.kohsuke.stapler.HttpResponse;

public class ScheduleBuildActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

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
        assertThat(scheduleBuildAction.getIconClassName(), is("symbol-calendar-outline plugin-ionicons-api"));
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
        assertThat(scheduleBuildAction.getDefaultDate(), matchesPattern(".* 22:00:00"));
    }

    @Test
    public void testGetDefaultDateObject() throws Exception {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime defaultDate = scheduleBuildAction.getDefaultDateObject();
        assertThat("Default build date is not after current time", defaultDate.isAfter(now));
    }

    @Test
    public void testDoCheckValidDate() {
        ZonedDateTime tomorrow = ZonedDateTime.now().plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        assertThat(
                scheduleBuildAction.doCheckDate(tomorrow.format(formatter), project).kind, is(FormValidation.Kind.OK));
    }

    @Test
    public void testDoCheckInvalidDate() {
        FormValidation validation = scheduleBuildAction.doCheckDate("43-23-2024 1:2:3", project);
        assertThat(validation.kind, is(FormValidation.Kind.ERROR));
        assertThat(validation.getMessage(), containsString("Not a valid build time"));
    }

    @Test
    public void testDoCheckDateInPast() {
        FormValidation validation = scheduleBuildAction.doCheckDate("01-01-2020 01:00:00", project);
        assertThat(validation.kind, is(FormValidation.Kind.ERROR));
        assertThat(validation.getMessage(), containsString("Build cannot be scheduled in the past"));
    }

    @Test
    public void testDoNextValidDate() {
        ZonedDateTime tomorrow = ZonedDateTime.now().plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        assertThat(
                scheduleBuildAction.doNext(tomorrow.format(formatter), project), is(instanceOf(ForwardToView.class)));
    }

    @Test
    public void testDoNextInvalidDate() {
        HttpResponse validation = scheduleBuildAction.doNext("43-23-2024 1:2:3", project);
        assertThat(validation, is(instanceOf(HttpRedirect.class)));
    }

    @Test
    public void testDoNextDateInPast() {
        HttpResponse validation = scheduleBuildAction.doNext("01-01-2020 01:00:00", project);
        assertThat(validation, is(instanceOf(HttpRedirect.class)));
    }
}
