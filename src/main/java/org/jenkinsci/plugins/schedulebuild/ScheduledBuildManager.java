package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;

public class ScheduledBuildManager {

    private static final Logger LOGGER = Logger.getLogger(ScheduledBuildManager.class.getName());

    private static final Set<ScheduledBuild> scheduledBuilds = new TreeSet<>();
    private static boolean loaded = false;

    private ScheduledBuildManager() {}

    public static void addScheduledBuild(ScheduledBuild scheduledBuild) {
        synchronized (scheduledBuilds) {
            load();
            scheduledBuilds.add(scheduledBuild);
            save();
        }
    }

    public static void removeScheduledBuild(ScheduledBuild scheduledBuild) {
        synchronized (scheduledBuilds) {
            load();
            scheduledBuilds.remove(scheduledBuild);
            save();
        }
    }

    public static Set<ScheduledBuild> getScheduledBuilds() {
        synchronized (scheduledBuilds) {
            load();
            return new TreeSet<>(scheduledBuilds);
        }
    }

    private static void removeDeletedJobs(String jobName) {
        synchronized (scheduledBuilds) {
            load();
            scheduledBuilds.removeIf(sb -> sb.getJob().equals(jobName));
            save();
        }
    }

    private static void renameJob(String oldName, String newName) {
        synchronized (scheduledBuilds) {
            load();
            for (ScheduledBuild scheduledBuild : scheduledBuilds) {
                if (scheduledBuild.getJob().equals(oldName)) {
                    scheduledBuild.setJob(newName);
                }
            }
            save();
        }
    }

    public static boolean hasPlannedBuildsForJob(String jobName) {
        synchronized (scheduledBuilds) {
            load();
            return scheduledBuilds.stream().anyMatch(r -> r.getJob().equals(jobName));
        }
    }

    public static List<ScheduledBuild> getPlannedBuildsForJob(String jobName) {
        synchronized (scheduledBuilds) {
            load();
            return scheduledBuilds.stream()
                    .filter(r -> r.getJob().equals(jobName))
                    .toList();
        }
    }

    static void clear() {
        synchronized (scheduledBuilds) {
            load();
            scheduledBuilds.clear();
            save();
        }
    }

    private static void save() {
        XmlFile file = new XmlFile(getFile());
        try {
            file.write(scheduledBuilds);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save scheduled builds to " + file, e);
        }
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        File file = getFile();
        if (!file.exists()) {
            return;
        }
        try {
            XmlFile xmlFile = new XmlFile(file);
            scheduledBuilds.addAll((Set<ScheduledBuild>) xmlFile.read());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load scheduled builds from " + file, e);
        }
    }

    private static File getFile() {
        return new File(Jenkins.get().getRootDir(), "scheduledBuilds.xml");
    }

    @Extension
    public static class ScheduleListener extends ItemListener {

        @Override
        public void onDeleted(Item item) {
            removeDeletedJobs(item.getFullName());
        }

        @Override
        public void onLocationChanged(Item item, String oldFullName, String newFullName) {
            renameJob(oldFullName, newFullName);
        }
    }
}
