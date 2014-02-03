package org.jenkinsci.plugins.schedulebuild;

import org.jenkinsci.plugins.schedulebuild.Messages;
import hudson.StructuredForm;
import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.util.FormValidation;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import javax.servlet.ServletException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;

public class ScheduleBuildAction implements Action, StaplerProxy {
    private final static long securityMargin = 120*1000;
    private final static long oneDay = 24*3600*1000;
    
    private final AbstractProject target;

    public ScheduleBuildAction(final AbstractProject target) {
        this.target = target;
    }

    public AbstractProject<?,?> getOwner() {
        return target;
    }

    
    public String getIconFileName() {
        return target.hasPermission(Job.BUILD) ? "/plugin/schedule-build-plugin/schedule.png" : null;
    }

    public String getDisplayName() {
            return target.hasPermission(Job.BUILD) ? Messages.ScheduleBuildAction_DisplayName() : null;
    }
    
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
        return Hudson.getInstance().getRootUrl() + "plugin/schedule-build-plugin/";
    }
           
    public String getDefaultDate() {
        Date buildtime = getDefaultDateObject();
        return DateFormat.getInstance().format(buildtime);
    }
    
    public Date getDefaultDateObject() {
        Date buildtime = new Date(), now = new Date(),
                defaultScheduleTime = new ScheduleBuildGlobalConfiguration().getDefaultScheduleTimeObject();
        buildtime.setHours(defaultScheduleTime.getHours());
        buildtime.setMinutes(defaultScheduleTime.getMinutes());
        buildtime.setSeconds(defaultScheduleTime.getSeconds());
        
        if(now.getTime() > buildtime.getTime()) {
            buildtime.setTime(buildtime.getTime() + ScheduleBuildAction.oneDay);
        }
        
        return buildtime;
    }
    
    public FormValidation doCheckDate(@QueryParameter String date) {
        Date ddate, now = new Date();
        try {
             ddate = DateFormat.getInstance().parse(date);
        }
        catch(ParseException ex) {
            return FormValidation.error(Messages.ScheduleBuildAction_ParsingError());   
        }
                      
        if(now.getTime() > ddate.getTime() + ScheduleBuildAction.securityMargin) // 120 sec security margin
            return FormValidation.error(Messages.ScheduleBuildAction_DateInPastError());   
                
        return FormValidation.ok();       
    }
       
    public HttpResponse doNext(StaplerRequest req) throws FormException, ServletException, IOException {
        JSONObject param = StructuredForm.get(req);
        Date ddate = getDefaultDateObject(), now = new Date();

        if (param.containsKey("date")) {
            try {
                ddate = DateFormat.getInstance().parse(param.getString("date"));
            } catch (ParseException ex) {
                return HttpResponses.redirectTo("error");
            }
        }

        long quietperiod = ddate.getTime() - now.getTime() + ScheduleBuildAction.securityMargin; // 120 sec security margin
        if (quietperiod < 0) {
            return HttpResponses.redirectTo("error");
        }
        
        return HttpResponses.redirectTo(getOwner().getAbsoluteUrl() + "build?delay=" + quietperiod / 1000 + "sec");
    }
}
