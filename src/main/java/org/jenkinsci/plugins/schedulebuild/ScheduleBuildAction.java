package org.jenkinsci.plugins.schedulebuild;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import javax.servlet.ServletException;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;

import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

public class ScheduleBuildAction implements Action, StaplerProxy {
    private final static long securityMargin = 120 * 1000;
    private final static long oneDay = 24 * 3600 * 1000;

    private final AbstractProject target;

    public ScheduleBuildAction(final AbstractProject target) {
        this.target = target;
    }

    public AbstractProject<?, ?> getOwner() {
        return target;
    }

    @Override
    public String getIconFileName() {
        return target.hasPermission(Job.BUILD) && this.target.isBuildable() ? "/plugin/schedule-build/images/schedule.png" : null;
    }

    @Override
    public String getDisplayName() {
        return target.hasPermission(Job.BUILD) && this.target.isBuildable() ? Messages.ScheduleBuildAction_DisplayName() : null;
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
        return Hudson.getInstance().getRootUrl() + "plugin/schedule-build/";
    }

    public String getDefaultDate() {
        Date buildtime = getDefaultDateObject();
        return dateFormat().format(buildtime);
    }

    public Date getDefaultDateObject() {
        Date buildtime = new Date(), now = new Date(), defaultScheduleTime = new ScheduleBuildGlobalConfiguration().getDefaultScheduleTimeObject();
        DateFormat dateFormat = dateFormat();
        try {
            now = dateFormat.parse(dateFormat.format(now));
        } catch (ParseException e) {
        }
        buildtime.setHours(defaultScheduleTime.getHours());
        buildtime.setMinutes(defaultScheduleTime.getMinutes());
        buildtime.setSeconds(defaultScheduleTime.getSeconds());

        if (now.getTime() > buildtime.getTime()) {
            buildtime.setTime(buildtime.getTime() + ScheduleBuildAction.oneDay);
        }

        return buildtime;
    }

    public FormValidation doCheckDate(@QueryParameter String date) {
        Date ddate, now = new Date();
        DateFormat dateFormat = dateFormat();
        try {
            ddate = dateFormat.parse(date);
            now = dateFormat.parse(dateFormat.format(now));
        } catch (ParseException ex) {
            return FormValidation.error(Messages.ScheduleBuildAction_ParsingError());
        }

        if (now.getTime() > ddate.getTime() + ScheduleBuildAction.securityMargin) // 120 sec security margin
            return FormValidation.error(Messages.ScheduleBuildAction_DateInPastError());

        return FormValidation.ok();
    }

    long quietperiod;

    public long getQuietPeriodInSeconds() {
        return quietperiod / 1000;
    }

    public HttpResponse doNext(StaplerRequest req) throws FormException, ServletException, IOException {
        // Deprecated function StructureForm.get()
        // JSONObject param = StructuredForm.get(req);
        JSONObject param = req.getSubmittedForm();
        Date ddate = getDefaultDateObject(), now = new Date();
        DateFormat dateFormat = dateFormat();
        try {
            now = dateFormat.parse(dateFormat.format(now));
        } catch (ParseException e) {
        }

        if (param.containsKey("date")) {
            try {
                ddate = dateFormat().parse(param.getString("date"));
            } catch (ParseException ex) {
                return HttpResponses.redirectTo("error");
            }
        }

        quietperiod = ddate.getTime() - now.getTime() + ScheduleBuildAction.securityMargin; // 120 sec security margin
        if (quietperiod < 0) {
            return HttpResponses.redirectTo("error");
        }

        return HttpResponses.forwardToView(this, "redirect");
    }

    private DateFormat dateFormat() {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Stapler.getCurrentRequest().getLocale());
        df.setTimeZone(new ScheduleBuildGlobalConfiguration().getTimeZoneObject());
        return df;
    }
}
