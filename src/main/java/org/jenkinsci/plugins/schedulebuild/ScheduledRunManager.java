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

public class ScheduledRunManager {

    private static final Logger LOGGER = Logger.getLogger(ScheduledRunManager.class.getName());

    private static final Set<ScheduledRun> scheduledRuns = new TreeSet<>();
    private static boolean loaded = false;

    private ScheduledRunManager() {}

    public static void addScheduledRun(ScheduledRun scheduledRun) {
        synchronized (scheduledRuns) {
            load();
            scheduledRuns.add(scheduledRun);
            save();
        }
    }

    public static void removeScheduledRun(ScheduledRun scheduledRun) {
        synchronized (scheduledRuns) {
            load();
            scheduledRuns.remove(scheduledRun);
            save();
        }
    }

    public static Set<ScheduledRun> getScheduledRuns() {
        synchronized (scheduledRuns) {
            load();
            return new TreeSet<>(scheduledRuns);
        }
    }

    private static void removeDeletedJobs(String jobName) {
        synchronized (scheduledRuns) {
            load();
            scheduledRuns.removeIf(scheduledRun -> scheduledRun.getJob().equals(jobName));
            save();
        }
    }

    private static void renameJob(String oldName, String newName) {
        synchronized (scheduledRuns) {
            load();
            for (ScheduledRun scheduledRun : scheduledRuns) {
                if (scheduledRun.getJob().equals(oldName)) {
                    scheduledRun.setJob(newName);
                }
            }
            save();
        }
    }

    public static boolean hasPlannedRunsForJob(String jobName) {
        synchronized (scheduledRuns) {
            load();
            return scheduledRuns.stream().anyMatch(r -> r.getJob().equals(jobName));
        }
    }

    public static List<ScheduledRun> getPlannedRunsForJob(String jobName) {
        synchronized (scheduledRuns) {
            load();
            return scheduledRuns.stream()
                    .filter(r -> r.getJob().equals(jobName))
                    .toList();
        }
    }

    static void clearAll() {
        synchronized (scheduledRuns) {
            scheduledRuns.clear();
            save();
        }
    }

    private static void save() {
        XmlFile file = new XmlFile(getFile());
        try {
            file.write(scheduledRuns);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save scheduled runs to " + file, e);
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
            scheduledRuns.addAll((Set<ScheduledRun>) xmlFile.read());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load scheduled runs from " + file, e);
        }
    }

    private static File getFile() {
        return new File(Jenkins.get().getRootDir(), "scheduledRuns.xml");
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
