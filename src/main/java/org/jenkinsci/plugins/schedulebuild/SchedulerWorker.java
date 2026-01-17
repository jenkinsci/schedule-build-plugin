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

@Extension
@Symbol("scheduleOnceScheduler")
public class SchedulerWorker extends AsyncPeriodicWork {

    private static final Logger LOGGER = Logger.getLogger(SchedulerWorker.class.getName());

    public SchedulerWorker() {
        super("scheduleOnceScheduler");
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        Set<ScheduledRun> scheduledRuns = ScheduledRunManager.getScheduledRuns();
        ZonedDateTime now = ZonedDateTime.now();
        for (ScheduledRun scheduledRun : scheduledRuns) {
            LOGGER.log(
                    Level.FINE,
                    () -> "Looking at scheduled run: " + scheduledRun.getJob() + " scheduled for "
                            + scheduledRun.getFormattedTime());
            ZonedDateTime time = scheduledRun.getTime();
            long delay = ChronoUnit.SECONDS.between(now, time);

            if (delay < 60) {
                if (delay < 0) {
                    // schedule was missed, run immediately
                    if (delay < -120) {
                        if (!scheduledRun.isTriggerOnMissed()) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Scheduled run for {0} was missed by {1} seconds. It will be skipped.",
                                    new Object[] {scheduledRun.getJob(), -delay});
                            ScheduledRunManager.removeScheduledRun(scheduledRun);
                            continue;
                        }
                        LOGGER.log(
                                Level.WARNING,
                                "Scheduled run for {0} was missed by {1} seconds. It will be started immediately.",
                                new Object[] {scheduledRun.getJob(), -delay});
                    }
                    delay = 0;
                }
                LOGGER.log(Level.FINE, () -> "Scheduling run for: " + scheduledRun.getJob());
                scheduledRun.run(listener, (int) delay);
                ScheduledRunManager.removeScheduledRun(scheduledRun);
            } else {
                // Since the set is sorted, we can break early
                break;
            }
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
