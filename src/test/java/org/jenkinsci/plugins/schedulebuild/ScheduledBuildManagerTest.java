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
public class ScheduledBuildManagerTest {

    FreeStyleProject project;
    MockFolder f;

    @BeforeEach
    void setUp(JenkinsRule r) throws IOException {
        project = r.createFreeStyleProject();
        f = r.createFolder("parent-folder");
        List<ParameterValue> params = new ArrayList<>();
        ScheduledBuildManager.clear();
        ScheduledBuild build = new ScheduledBuild(
                "123",
                project.getFullName(),
                ZonedDateTime.now().plusHours(3),
                params,
                true,
                new ScheduledBuildCause());
        ScheduledBuildManager.addScheduledBuild(build);
    }

    @Test
    void testHasPlannedBuilds() {
        assertTrue(ScheduledBuildManager.hasPlannedBuildsForJob(project.getFullName()));
    }

    @Test
    void testGetPlannedBuilds() {
        List<ScheduledBuild> builds = ScheduledBuildManager.getPlannedBuildsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
    }

    @Test
    void testRemovePlannedBuild() {
        List<ScheduledBuild> builds = ScheduledBuildManager.getPlannedBuildsForJob(project.getFullName());
        assertEquals(1, builds.size());
        ScheduledBuildManager.removeScheduledBuild(builds.get(0));
        builds = ScheduledBuildManager.getPlannedBuildsForJob(project.getFullName());
        assertEquals(0, builds.size());
    }

    @Test
    void testRenameJob() throws IOException {
        List<ScheduledBuild> builds = ScheduledBuildManager.getPlannedBuildsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
        project.renameTo("new-name");
        builds = ScheduledBuildManager.getPlannedBuildsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
    }

    @Test
    void testDeleteJob() throws IOException, InterruptedException {
        List<ScheduledBuild> builds = ScheduledBuildManager.getPlannedBuildsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
        project.delete();
        builds = ScheduledBuildManager.getPlannedBuildsForJob(project.getFullName());
        assertEquals(0, builds.size());
    }

    @Test
    void testMoveJob() throws IOException {
        List<ScheduledBuild> builds = ScheduledBuildManager.getPlannedBuildsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
        Items.move(project, f);
        builds = ScheduledBuildManager.getPlannedBuildsForJob(project.getFullName());
        assertEquals(1, builds.size());
        assertEquals(project.getFullName(), builds.get(0).getJob());
    }
}
