package org.jenkinsci.plugins.schedulebuild;

import hudson.model.Action;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.management.Badge;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.util.TimeDuration;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;

public class ScheduleBuildAction implements Action, IconSpec {

    private static final Logger LOGGER = Logger.getLogger(ScheduleBuildAction.class.getName());

    private final Job<?, ?> target;
    private static final long SECURITY_MARGIN = 120;

    private static final String DATE_TIME_PATTERN = "dd-MM-yyyy HH:mm:ss";

    private static final DateTimeFormatter[] FORMATTERS = {
        DateTimeFormatter.ofPattern("d-M-y H:m[:s]"), DateTimeFormatter.ofPattern("d-M-y h:m[:s] a", Locale.ROOT),
    };

    private long quietperiod;

    public ScheduleBuildAction(final Job<?, ?> target) {
        this.target = target;
    }

    public Job<?, ?> getOwner() {
        return target;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getIconClassName() {
        return "symbol-calendar-outline plugin-ionicons-api";
    }

    @Override
    public String getDisplayName() {
        return Messages.ScheduleBuildAction_DisplayName();
    }

    @Override
    public String getUrlName() {
        return "schedule";
    }

    public String getDefaultDate() {
        return getDefaultDateObject().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    public ZonedDateTime getDefaultDateObject() {
        ZonedDateTime zdt = ScheduleBuildGlobalConfiguration.get().getDefaultScheduleTimeObject();
        ZonedDateTime now = ZonedDateTime.now();
        if (now.isAfter(zdt)) {
            zdt = zdt.plusDays(1);
        }
        return zdt;
    }

    public String getMinDate() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime zonedNow =
                now.withZoneSameInstant(ScheduleBuildGlobalConfiguration.get().getZoneId());
        return zonedNow.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    @RequirePOST
    public FormValidation doCheckDate(@QueryParameter String value, @AncestorInPath Item item) {
        if (item == null) {
            return FormValidation.ok();
        }
        // User requesting a build needs permission to start the build
        item.checkPermission(Item.BUILD);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime ddate;
        try {
            ddate = parseDateTime(value.trim())
                    .atZone(ScheduleBuildGlobalConfiguration.get().getZoneId())
                    .plusSeconds(SECURITY_MARGIN);
        } catch (DateTimeParseException ex) {
            return FormValidation.error(Messages.ScheduleBuildAction_ParsingError());
        }
        if (now.isAfter(ddate)) {
            return FormValidation.error(Messages.ScheduleBuildAction_DateInPastError());
        }

        return FormValidation.ok();
    }

    public long getQuietPeriodInSeconds() {
        return quietperiod;
    }

    public Badge getBadge() {
        List<ScheduledBuild> plannedBuilds = getPlannedBuilds();
        if (plannedBuilds.isEmpty()) {
            return null;
        }
        return new Badge(
                Integer.toString(plannedBuilds.size()),
                Messages.ScheduleBuildAction_BadgeTooltip(plannedBuilds.size()),
                Badge.Severity.INFO);
    }

    @POST
    public void doCancelBuild(@QueryParameter String id, StaplerResponse2 rsp) {
        target.checkPermission(Item.CANCEL);
        try {
            for (ScheduledBuild sr : ScheduledBuildManager.getPlannedBuildsForJob(target.getFullName())) {
                if (sr.getId().equals(id)) {
                    sr.setAborted(true);
                    ScheduledBuildManager.removeScheduledBuild(sr);
                    break;
                }
            }
            if (ScheduledBuildManager.hasPlannedBuildsForJob(target.getFullName())) {
                rsp.sendRedirect2("./planned");
            } else {
                rsp.sendRedirect2("..");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @POST
    public void doBuild(StaplerRequest2 req, StaplerResponse2 rsp) {
        target.checkPermission(Item.BUILD);
        try {
            JSONObject formData = req.getSubmittedForm();
            String date = formData.getString("date");
            boolean scheduleViaCron = formData.has("scheduleViaCron");

            ZonedDateTime startDateTime, now = ZonedDateTime.now();

            final String time = date.trim();
            try {
                startDateTime = parseDateTime(time)
                        .atZone(ScheduleBuildGlobalConfiguration.get().getZoneId());
            } catch (DateTimeParseException ex) {
                LOGGER.log(Level.INFO, ex, () -> "Error parsing " + time);
                rsp.sendRedirect2("error");
                return;
            }

            long delay = ChronoUnit.SECONDS.between(now, startDateTime);
            LOGGER.log(Level.FINER, () -> "Quietperiod: " + delay);
            if (delay + ScheduleBuildAction.SECURITY_MARGIN < 0) { // 120 sec security margin
                LOGGER.log(Level.INFO, () -> "Error security margin " + delay);
                rsp.sendRedirect2("error");
                return;
            }

            if (!scheduleViaCron) {
                TimeDuration quietPeriod = new TimeDuration(Math.max(0, delay) * 1000);
                new ParameterizedJobMixIn() {
                    @Override
                    protected Job<?, ?> asJob() {
                        return target;
                    }
                }.doBuild(req, rsp, quietPeriod);
                return;
            }
            String id = UUID.randomUUID().toString();
            List<ParameterValue> values = new ArrayList<>();
            JSONObject scheduleViaCronObject = formData.getJSONObject("scheduleViaCron");
            boolean triggerOnMissed = scheduleViaCronObject.getBoolean("triggerOnMissed");

            if (target instanceof ParameterizedJobMixIn.ParameterizedJob<?, ?> pj) {
                if (pj.isParameterized()) {

                    Object parameter = formData.get("parameter");

                    if (parameter != null) {
                        ParametersDefinitionProperty prop = target.getProperty(ParametersDefinitionProperty.class);
                        JSONArray a = JSONArray.fromObject(parameter);

                        for (Object o : a) {
                            JSONObject jo = (JSONObject) o;
                            String name = jo.getString("name");

                            ParameterDefinition d = prop.getParameterDefinition(name);
                            if (d == null) throw new IllegalArgumentException("No such parameter definition: " + name);
                            ParameterValue parameterValue = d.createValue(req, jo);
                            if (parameterValue != null) {
                                values.add(parameterValue);
                            } else {
                                throw new IllegalArgumentException("Cannot retrieve the parameter value: " + name);
                            }
                        }
                    }
                }
            }

            ScheduledBuild sr = new ScheduledBuild(
                    id, target.getFullName(), startDateTime, values, triggerOnMissed, new ScheduledBuildCause());

            ScheduledBuildManager.addScheduledBuild(sr);

            rsp.sendRedirect2("..");
        } catch (IOException | ServletException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasPlannedBuilds() {
        return ScheduledBuildManager.hasPlannedBuildsForJob(target.getFullName());
    }

    public List<ScheduledBuild> getPlannedBuilds() {
        return ScheduledBuildManager.getPlannedBuildsForJob(target.getFullName());
    }

    public boolean isParameterized() {
        if (target instanceof ParameterizedJobMixIn.ParameterizedJob<?, ?> pj) {
            return pj.isParameterized();
        }
        return false;
    }

    public List<ParameterDefinition> getParameterDefinitions() {
        if (target instanceof ParameterizedJobMixIn.ParameterizedJob<?, ?> pj) {
            if (pj.isParameterized()) {
                return target.getProperty(ParametersDefinitionProperty.class).getParameterDefinitions();
            }
        }
        return List.of();
    }

    private LocalDateTime parseDateTime(String time) {
        DateTimeParseException exception = null;
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(time.toUpperCase(Locale.ROOT), formatter);
            } catch (DateTimeParseException dtex) {
                exception = dtex;
                LOGGER.log(Level.FINE, dtex, () -> "Did not parse '" + time + "' with formatter " + formatter);
            }
        }
        throw exception;
    }

    @Restricted(NoExternalUse.class)
    public String getDateTimeFormatting() {
        return DATE_TIME_PATTERN;
    }

    @Restricted(NoExternalUse.class)
    public String getTimeZone() {
        return ScheduleBuildGlobalConfiguration.get().getTimeZone();
    }
}
