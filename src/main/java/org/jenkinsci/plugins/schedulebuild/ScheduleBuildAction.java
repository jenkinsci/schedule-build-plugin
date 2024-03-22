package org.jenkinsci.plugins.schedulebuild;

import hudson.model.Action;
import hudson.model.Descriptor.FormException;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class ScheduleBuildAction implements Action, StaplerProxy, IconSpec {

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
        return target.hasPermission(Job.BUILD) && this.target.isBuildable()
                ? "symbol-calendar-outline plugin-ionicons-api"
                : null;
    }

    @Override
    public String getDisplayName() {
        return target.hasPermission(Job.BUILD) && this.target.isBuildable()
                ? Messages.ScheduleBuildAction_DisplayName()
                : null;
    }

    @Override
    public String getUrlName() {
        return "schedule";
    }

    public boolean schedule(StaplerRequest req, JSONObject formData) throws FormException {
        return true;
    }

    @Override
    public Object getTarget() {
        target.checkPermission(Job.BUILD);
        return this;
    }

    public String getDefaultDate() {
        return getDefaultDateObject().format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    public ZonedDateTime getDefaultDateObject() {
        ZonedDateTime zdt = new ScheduleBuildGlobalConfiguration().getDefaultScheduleTimeObject();
        ZonedDateTime now = ZonedDateTime.now();
        if (now.isAfter(zdt)) {
            zdt = zdt.plusDays(1);
        }
        return zdt;
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
                    .atZone(new ScheduleBuildGlobalConfiguration().getZoneId())
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

    @RequirePOST
    public HttpResponse doNext(@QueryParameter String date, @AncestorInPath Item item) {
        if (item == null) {
            return FormValidation.ok();
        }
        // User requesting a build needs permission to start the build
        item.checkPermission(Item.BUILD);
        ZonedDateTime ddate, now = ZonedDateTime.now();

        final String time = date.trim();
        try {
            ddate = parseDateTime(time).atZone(new ScheduleBuildGlobalConfiguration().getZoneId());
        } catch (DateTimeParseException ex) {
            LOGGER.log(Level.INFO, ex, () -> "Error parsing " + time);
            return HttpResponses.redirectTo("error");
        }
        quietperiod = ChronoUnit.SECONDS.between(now, ddate);
        LOGGER.log(Level.FINER, () -> "Quietperiod: " + quietperiod);
        if (quietperiod + ScheduleBuildAction.SECURITY_MARGIN < 0) { // 120 sec security margin
            LOGGER.log(Level.INFO, () -> "Error security margin" + quietperiod);
            return HttpResponses.redirectTo("error");
        }
        return HttpResponses.forwardToView(this, "redirect");
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

    public boolean isJobParameterized() {
        ParametersDefinitionProperty paramDefinitions = target.getProperty(ParametersDefinitionProperty.class);
        return paramDefinitions != null
                && paramDefinitions.getParameterDefinitions() != null
                && paramDefinitions.getParameterDefinitions().size() > 0;
    }

    @Restricted(NoExternalUse.class)
    public String getDateTimeFormatting() {
        return DATE_TIME_PATTERN;
    }

    @Restricted(NoExternalUse.class)
    public String getTimeZone() {
        return new ScheduleBuildGlobalConfiguration().getTimeZone();
    }
}
