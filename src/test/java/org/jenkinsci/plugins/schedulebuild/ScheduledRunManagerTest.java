package org.jenkinsci.plugins.schedulebuild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import hudson.model.Items;
import hudson.model.ParameterValue;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class ScheduledRunManagerTest {

    FreeStyleProject project;
    MockFolder f;

    @BeforeEach
    void setUp(JenkinsRule r) throws IOException {
        project = r.createFreeStyleProject();
        f = r.createFolder("parent-folder");
        ScheduledRunManager.clearAll();
        List<ParameterValue> params = new ArrayList<>();
        ScheduledRun run = new ScheduledRun(
                "123",
                project.getFullName(),
                ZonedDateTime.now().plusHours(3),
                params,
                true,
                new ScheduledBuildCause());
        ScheduledRunManager.addScheduledRun(run);
    }

    @Test
    void testHasPlannedRuns() {
        assertTrue(ScheduledRunManager.hasPlannedRunsForJob(project.getFullName()));
    }

    @Test
    void testGetPlannedRuns() {
        List<ScheduledRun> builds = ScheduledRunManager.getPlannedRunsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
    }

    @Test
    void testRemovePlannedRuns() {
        List<ScheduledRun> builds = ScheduledRunManager.getPlannedRunsForJob(project.getFullName());
        assertEquals(1, builds.size());
        ScheduledRunManager.removeScheduledRun(builds.get(0));
        builds = ScheduledRunManager.getPlannedRunsForJob(project.getFullName());
        assertEquals(0, builds.size());
    }

    @Test
    void testRenameJob() throws IOException {
        List<ScheduledRun> builds = ScheduledRunManager.getPlannedRunsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
        project.renameTo("new-name");
        builds = ScheduledRunManager.getPlannedRunsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
    }

    @Test
    void testDeleteJob() throws IOException, InterruptedException {
        List<ScheduledRun> builds = ScheduledRunManager.getPlannedRunsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
        project.delete();
        builds = ScheduledRunManager.getPlannedRunsForJob(project.getFullName());
        assertEquals(0, builds.size());
    }

    @Test
    void testMoveJob() throws IOException {
        List<ScheduledRun> builds = ScheduledRunManager.getPlannedRunsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
        Items.move(project, f);
        builds = ScheduledRunManager.getPlannedRunsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
    }
}
