package org.jenkinsci.plugins.schedulebuild;

import org.jenkinsci.plugins.schedulebuild.Messages;
import hudson.Extension;
import hudson.util.FormValidation;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import jenkins.model.GlobalConfiguration;
import jenkins.model.GlobalConfigurationCategory;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class ScheduleBuildGlobalConfiguration extends GlobalConfiguration {
    private Date defaultScheduleTime;
    
    public ScheduleBuildGlobalConfiguration() {
        this.defaultScheduleTime = new Date(0,0,0,22,0);
        load();
    }
   
    @Override
    public GlobalConfigurationCategory getCategory() {
        return new GlobalConfigurationCategory.Unclassified();
    }
    
    public String getDefaultScheduleTime() {
        return DateFormat.getTimeInstance().format(this.defaultScheduleTime);
    } 
    
    public Date getDefaultScheduleTimeObject() {
        return this.defaultScheduleTime;
    } 
    
    public FormValidation doCheckDefaultScheduleTime(@QueryParameter String value) {
        try {
            DateFormat.getTimeInstance().parse(value);
        } catch(ParseException ex) {
            return FormValidation.error(Messages.ScheduleBuildGlobalConfiguration_ParsingError());
        }
        return FormValidation.ok();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        if(json.containsKey("defaultScheduleTime")) {
            try {
                this.defaultScheduleTime = DateFormat.getTimeInstance().parse(json.getString("defaultScheduleTime"));
                save();
                return true;
            } catch(ParseException ex) {
            }
        }
        return false;
    }
}
