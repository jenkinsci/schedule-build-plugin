package org.jenkinsci.plugins.schedulebuild;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;

import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ScheduleBuildActionTest {

    private ScheduleBuildAction scheduleBuildAction;
    private FreeStyleProject project;

    @BeforeEach
    void setUp(JenkinsRule r) throws IOException {
        project = r.createFreeStyleProject();
        scheduleBuildAction = new ScheduleBuildAction(project);
    }

    @Test
    void testGetOwner() {
        assertThat(scheduleBuildAction.getOwner(), is(project));
    }

    @Test
    void testGetIconFileName() throws Exception {
        assertThat(scheduleBuildAction.getIconFileName(), is(nullValue()));
        project.disable();
        assertThat(scheduleBuildAction.getIconFileName(), is(nullValue()));
    }

    @Test
    void testGetIconClassName() throws Exception {
        assertThat(scheduleBuildAction.getIconClassName(), is("symbol-calendar-outline plugin-ionicons-api"));
    }

    @Test
    void testGetDisplayName() throws Exception {
        assertThat(scheduleBuildAction.getDisplayName(), is("Schedule Build"));
    }

    @Test
    void testGetUrlName() {
        assertThat(scheduleBuildAction.getUrlName(), is("schedule"));
    }

    @Test
    void testGetQuietPeriodInSeconds() {
        assertThat(scheduleBuildAction.getQuietPeriodInSeconds(), is(0L));
    }

    @Test
    void testGetDefaultDate() {
        assertThat(scheduleBuildAction.getDefaultDate(), matchesPattern(".* 22:00:00"));
    }

    @Test
    void testGetDefaultDateObject() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime defaultDate = scheduleBuildAction.getDefaultDateObject();
        assertThat("Default build date is not after current time", defaultDate.isAfter(now));
    }

    @Test
    void testDoCheckValidDate() {
        ZonedDateTime tomorrow = ZonedDateTime.now().plusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        assertThat(
                scheduleBuildAction.doCheckDate(tomorrow.format(formatter), project).kind, is(FormValidation.Kind.OK));
    }

    @Test
    void testDoCheckInvalidDate() {
        FormValidation validation = scheduleBuildAction.doCheckDate("43-23-2024 1:2:3", project);
        assertThat(validation.kind, is(FormValidation.Kind.ERROR));
        assertThat(validation.getMessage(), containsString("Not a valid build time"));
    }

    @Test
    void testDoCheckDateInPast() {
        FormValidation validation = scheduleBuildAction.doCheckDate("01-01-2020 01:00:00", project);
        assertThat(validation.kind, is(FormValidation.Kind.ERROR));
        assertThat(validation.getMessage(), containsString("Build cannot be scheduled in the past"));
    }
}
