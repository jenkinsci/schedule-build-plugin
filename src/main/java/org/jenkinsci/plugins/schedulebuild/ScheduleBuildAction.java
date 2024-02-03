package org.jenkinsci.plugins.schedulebuild;

import hudson.model.Action;
import hudson.model.Descriptor.FormException;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
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

    public String getIconPath() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            String rootUrl = instance.getRootUrl();

            if (rootUrl != null) {
                return rootUrl + "plugin/schedule-build/";
            }
        }
        throw new IllegalStateException("couldn't load rootUrl");
    }

    public String getDefaultDate() {
        return getDefaultDateObject().format(getDateTimeFormatter());
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
    public FormValidation doCheckDate(@QueryParameter String date, @AncestorInPath Item item) {
        if (item == null) {
            return FormValidation.ok();
        }
        // User requesting a build needs permission to start the build
        item.checkPermission(Item.BUILD);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime ddate;
        LocalDateTime input;
        try {
            input = LocalDateTime.parse(date, getDateTimeFormatter());
        } catch (DateTimeParseException ex) {
            LOGGER.log(Level.INFO, () -> "Parsing error: " + ex.getMessage());
            LOGGER.log(Level.INFO, () -> "Parsing error index: " + ex.getErrorIndex());
            LOGGER.log(Level.INFO, () -> "Parsing error value: " + ex.getParsedString());
            return FormValidation.error(Messages.ScheduleBuildAction_ParsingError());
        }
        ddate = input.atZone(new ScheduleBuildGlobalConfiguration().getZoneId()).plusSeconds(SECURITY_MARGIN);
        if (now.isAfter(ddate)) {
            return FormValidation.error(Messages.ScheduleBuildAction_DateInPastError());
        }

        return FormValidation.ok();
    }

    public long getQuietPeriodInSeconds() {
        return quietperiod;
    }

    @RequirePOST
    public HttpResponse doNext(StaplerRequest req, @AncestorInPath Item item)
            throws FormException, ServletException, IOException {
        if (item == null) {
            return FormValidation.ok();
        }
        // User requesting a build needs permission to start the build
        item.checkPermission(Item.BUILD);
        // Deprecated function StructureForm.get()
        // JSONObject param = StructuredForm.get(req);
        JSONObject param = req.getSubmittedForm();
        ZonedDateTime ddate = getDefaultDateObject(), now = ZonedDateTime.now();

        if (param.containsKey("date")) {
            try {
                ddate = ZonedDateTime.parse(param.getString("date"), getDateTimeFormatter());
            } catch (DateTimeParseException ex) {
                return HttpResponses.redirectTo("error");
            }
        }

        quietperiod = ChronoUnit.SECONDS.between(now, ddate);
        LOGGER.log(Level.INFO, () -> "Quietperiod: " + quietperiod);
        if (quietperiod + ScheduleBuildAction.SECURITY_MARGIN < 0) { // 120 sec security margin
            return HttpResponses.redirectTo("error");
        }
        return HttpResponses.forwardToView(this, "redirect");
    }

    public boolean isJobParameterized() {
        ParametersDefinitionProperty paramDefinitions = target.getProperty(ParametersDefinitionProperty.class);
        return paramDefinitions != null
                && paramDefinitions.getParameterDefinitions() != null
                && paramDefinitions.getParameterDefinitions().size() > 0;
    }

    private DateTimeFormatter getDateTimeFormatter() {
        return DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
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
