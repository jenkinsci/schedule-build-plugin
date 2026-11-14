package org.jenkinsci.plugins.schedulebuild;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.util.Timer;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
public class ScheduledBuild implements Serializable, Comparable<ScheduledBuild> {

    private static final Logger LOGGER = Logger.getLogger(ScheduledBuild.class.getName());

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss v";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private final String id;
    private volatile String job;
    private final ZonedDateTime time;
    private final List<ParameterValue> values;
    private final boolean triggerOnMissed;
    private final ScheduledBuildCause cause;
    private transient boolean started = false;
    private transient boolean aborted = false;

    public ScheduledBuild(
            String id,
            String job,
            ZonedDateTime time,
            @NonNull List<ParameterValue> values,
            boolean triggerOnMissed,
            ScheduledBuildCause cause) {
        this.id = id;
        this.time = time;
        this.values = values;
        this.triggerOnMissed = triggerOnMissed;
        this.job = job;
        this.cause = cause;
    }

    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    public boolean isStarted() {
        return started;
    }

    public String getId() {
        return id;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    public String getFormattedTime() {
        return DATE_TIME_FORMATTER.format(time);
    }

    public boolean isTriggerOnMissed() {
        return triggerOnMissed;
    }

    public List<ParameterValue> getValues() {
        return values;
    }

    public ScheduledBuildCause getCause() {
        return cause;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public void run(int delay) {
        LOGGER.log(Level.FINER, () -> "Starting build for " + this.job + " in " + delay + " milliseconds.");
        final ScheduledExecutorService scheduler = Timer.get();
        this.started = true;
        scheduler.schedule(this::start, delay, TimeUnit.MILLISECONDS);
    }

    public String getParametersTooltip() {
        StringBuilder sb = new StringBuilder();
        sb.append("Build Parameters:");
        for (ParameterValue v : values) {
            sb.append("\n");
            sb.append(v.getName()).append("=").append(v.getValue());
        }
        return sb.toString();
    }

    public void start() {
        if (aborted) {
            LOGGER.log(Level.FINE, () -> "Scheduled build for " + this.job + " has been aborted. Not starting build.");
            return;
        }
        Job<?, ?> j = Jenkins.get().getItemByFullName(this.job, Job.class);
        ScheduledBuildManager.removeScheduledBuild(this);
        if (j == null) {
            LOGGER.log(Level.FINE, "Job {0} not found. Cannot start scheduled build.", this.job);
            return;
        }
        LOGGER.log(Level.FINER, () -> "Starting build for " + this.job + " now.");
        ParametersDefinitionProperty pp = j.getProperty(ParametersDefinitionProperty.class);
        List<Action> actions = new ArrayList<>();
        if (cause != null) {
            actions.add(new CauseAction(cause));
        }
        if (pp != null) {
            actions.add(new ParametersAction(values));
        }

        ParameterizedJobMixIn.scheduleBuild2(j, 0, actions.toArray(new Action[0]));
    }

    @Override
    public int compareTo(ScheduledBuild o) {
        int c = time.compareTo(o.time);
        if (c != 0) {
            return c;
        }
        return id.compareTo(o.id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ScheduledBuild other = (ScheduledBuild) obj;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
