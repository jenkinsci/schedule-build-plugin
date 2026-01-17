package org.jenkinsci.plugins.schedulebuild;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;
import java.io.IOException;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Symbol("scheduleOnceScheduler")
@Restricted(NoExternalUse.class)
public class SchedulerWorker extends AsyncPeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(SchedulerWorker.class.getName());

    public SchedulerWorker() {
        super("scheduleOnceScheduler");
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        Set<ScheduledBuild> scheduledBuilds = ScheduledBuildManager.getScheduledBuilds();
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextMinute =
                now.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES).minusNanos(1000);
        int gracePeriodMillis = ScheduleBuildGlobalConfiguration.get().getGracePeriodMinutes() * 60 * 1000;
        for (ScheduledBuild scheduledBuild : scheduledBuilds) {
            LOGGER.log(
                    Level.FINE,
                    () -> "Looking at scheduled build: " + scheduledBuild.getJob() + " scheduled for "
                            + scheduledBuild.getFormattedTime());
            ZonedDateTime time = scheduledBuild.getTime();

            if (scheduledBuild.isStarted()) {
                // safeguarding against multiple starts
                continue;
            }

            if (time.isAfter(nextMinute)) {
                // Next minute, since the set is sorted, we can break early
                break;
            }

            long delay = ChronoUnit.MILLIS.between(now, time);

            if (delay < 0) {
                // schedule was missed, start immediately
                if (delay < -gracePeriodMillis) {
                    if (!scheduledBuild.isTriggerOnMissed()) {
                        LOGGER.log(
                                Level.WARNING,
                                "Scheduled build for job {0} was missed by {1} milliseconds. It will be skipped as triggerOnMissed is not set.",
                                new Object[] {scheduledBuild.getJob(), -delay});
                        ScheduledBuildManager.removeScheduledBuild(scheduledBuild);
                        continue;
                    }
                    LOGGER.log(
                            Level.WARNING,
                            "Scheduled build for {0} was missed by {1} milliseconds. It will be started immediately as triggerOnMissed is set.",
                            new Object[] {scheduledBuild.getJob(), -delay});
                }
                delay = 0;
            }
            LOGGER.log(Level.FINE, () -> "Scheduling build for: " + scheduledBuild.getJob());
            scheduledBuild.run((int) delay);
        }
    }

    @Override
    public long getInitialDelay() {
        return MIN - TimeUnit.SECONDS.toMillis(LocalTime.now().getSecond());
    }

    @Override
    public long getRecurrencePeriod() {
        return MIN;
    }
}
