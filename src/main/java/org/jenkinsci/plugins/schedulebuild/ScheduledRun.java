package org.jenkinsci.plugins.schedulebuild;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Action;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TaskListener;
import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
public class ScheduledRun implements Serializable, Comparable<ScheduledRun> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss v";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private final String id;
    private String job;
    private final ZonedDateTime time;
    private final List<ParameterValue> values;
    private final boolean triggerOnMissed;
    private final ScheduledBuildCause cause;

    public ScheduledRun(
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

    public Cause getCause() {
        return cause;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public void run(TaskListener listener, int delay) {
        Job<?, ?> j = Jenkins.get().getItemByFullName(job, Job.class);
        if (j != null) {
            run(j, delay);
        } else {
            if (listener != null) {
                listener.error("Job " + job + " not found, cannot schedule build");
            }
        }
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

    public void run(Job<?, ?> job, int delay) {
        ParametersDefinitionProperty pp = job.getProperty(ParametersDefinitionProperty.class);
        List<Action> actions = new ArrayList<>();
        if (cause != null) {
            actions.add(new CauseAction(cause));
        }
        if (pp != null) {
            actions.add(new ParametersAction(values));
        }
        // Calculate the delay in seconds
        // The worker runs every minute at the 0 seconds, but we might have scheduled a job at 30 seconds past the
        // minute
        // So we need to calculate the delay in seconds, for which the job stays in the queue so it starts at the right
        // time
        ParameterizedJobMixIn.scheduleBuild2(job, delay, actions.toArray(new Action[0]));
    }

    @Override
    public int compareTo(ScheduledRun o) {
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
        ScheduledRun other = (ScheduledRun) obj;
        return id.equals(other.id);
    }
}
