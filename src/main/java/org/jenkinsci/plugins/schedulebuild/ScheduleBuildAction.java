package org.jenkinsci.plugins.schedulebuild;

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
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;

public class ScheduleBuildAction implements Action, StaplerProxy {
    private final static long securityMargin = 120*1000;
    private final static long oneDay = 24*3600*1000;
    
    private final Job<?,?> target;

    public ScheduleBuildAction(final Job<?,?> target) {
        this.target = target;
    }

    public Job<?,?> getOwner() {
        return target;
    }

    
    public String getIconFileName() {
        return target.hasPermission(Job.BUILD) && this.target.isBuildable() ? "/plugin/schedule-build/images/schedule.png" : null;
    }

    public String getDisplayName() {
        return target.hasPermission(Job.BUILD) && this.target.isBuildable() ? Messages.ScheduleBuildAction_DisplayName() : null;
    }
    
    public String getUrlName() {
        return "schedule";
    }
    
    public boolean schedule(StaplerRequest req, JSONObject formData) throws FormException {
            return true;
    }
    
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
             ddate = dateFormat().parse(date);
        }
        catch(ParseException ex) {
            return FormValidation.error(Messages.ScheduleBuildAction_ParsingError());   
        }
                      
        if(now.getTime() > ddate.getTime() + ScheduleBuildAction.securityMargin) // 120 sec security margin
            return FormValidation.error(Messages.ScheduleBuildAction_DateInPastError());   
                
        return FormValidation.ok();       
    }
    long quietperiod;

    public long getQuietPeriodInSeconds(){
        return quietperiod/1000;
    }


    public HttpResponse doNext(StaplerRequest req) throws FormException, ServletException, IOException {
        //Deprecated function StructureForm.get()
        //JSONObject param = StructuredForm.get(req);
        JSONObject param = req.getSubmittedForm();
        Date ddate = getDefaultDateObject(), now = new Date();

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
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Stapler.getCurrentRequest().getLocale());
    }
}
