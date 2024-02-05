package org.jenkinsci.plugins.schedulebuild;

import hudson.model.Action;
import hudson.model.Descriptor.FormException;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.FormValidation;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.jenkins.ui.icon.IconSpec;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class ScheduleBuildAction implements Action, StaplerProxy, IconSpec {

    private static final Logger LOGGER = Logger.getLogger(ScheduleBuildAction.class.getName());

    private final Job<?, ?> target;
    private static final long SECURITY_MARGIN = 120 * 1000;
    private static final long ONE_DAY = 24 * 3600 * 1000;

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
        return dateFormat().format(getDefaultDateObject());
    }

    public Date getDefaultDateObject() {
        Date buildtime = new Date(),
                now = new Date(),
                defaultScheduleTime = new ScheduleBuildGlobalConfiguration().getDefaultScheduleTimeObject();
        DateFormat dateFormat = dateFormat();
        try {
            now = dateFormat.parse(dateFormat.format(now));
        } catch (ParseException e) {
            LOGGER.log(Level.WARNING, "Error while parsing date", e);
        }
        buildtime.setHours(defaultScheduleTime.getHours());
        buildtime.setMinutes(defaultScheduleTime.getMinutes());
        buildtime.setSeconds(defaultScheduleTime.getSeconds());

        if (now.getTime() > buildtime.getTime()) {
            buildtime.setTime(buildtime.getTime() + ScheduleBuildAction.ONE_DAY);
        }

        return buildtime;
    }

    @RequirePOST
    public FormValidation doCheckDate(@QueryParameter String value, @AncestorInPath Item item) {
        if (item == null) {
            return FormValidation.ok();
        }
        // User requesting a build needs permission to start the build
        item.checkPermission(Item.BUILD);
        Date ddate, now = new Date();
        DateFormat dateFormat = dateFormat();
        try {
            ddate = dateFormat.parse(value);
            now = dateFormat.parse(dateFormat.format(now));
        } catch (ParseException ex) {
            return FormValidation.error(Messages.ScheduleBuildAction_ParsingError());
        }

        if (now.getTime() > ddate.getTime() + ScheduleBuildAction.SECURITY_MARGIN) {
            return FormValidation.error(Messages.ScheduleBuildAction_DateInPastError());
        }

        return FormValidation.ok();
    }

    public long getQuietPeriodInSeconds() {
        return quietperiod / 1000;
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
        Date ddate = getDefaultDateObject(), now = new Date();
        DateFormat dateFormat = dateFormat();
        try {
            now = dateFormat.parse(dateFormat.format(now));
        } catch (ParseException e) {
            LOGGER.log(Level.WARNING, "Error while parsing date", e);
        }

        if (param.containsKey("date")) {
            try {
                ddate = dateFormat().parse(param.getString("date"));
            } catch (ParseException ex) {
                return HttpResponses.redirectTo("error");
            }
        }

        quietperiod = ddate.getTime() - now.getTime();
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

    private DateFormat dateFormat() {
        Locale locale = Stapler.getCurrentRequest() != null
                ? Stapler.getCurrentRequest().getLocale()
                : Locale.getDefault();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        df.setTimeZone(new ScheduleBuildGlobalConfiguration().getTimeZoneObject());
        return df;
    }
}
