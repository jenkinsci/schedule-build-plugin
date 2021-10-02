# Schedule Build plugin

Adds capability to schedule a build for a later point in time. Asks the
user for a date and time and adds the build to the build queue with the
respective quiet period.

# Scheduling Builds

Press the "Schedule Build" link on the project page or use the schedule
build action in the list view.

![](docs/images/Schedule_Project_Page.png)

![](docs/images/Schedule_Action.png)  
Then select date and time when to schedule the build.

![](docs/images/Schedule_Page.png)  
The build will be added to the build queue with the respective quiet
period.

![](docs/images/Scheule_Build_Queue.png)

# Configure Schedule Build Plugin

The configuration of the schedule build plugin is very simple. There are
only two parameters on the Jenkins system configuration page.

The default time which is set when a user wants to schedule a build may
be configured and time zone used by the plugin, which might differ from
the system time zone.

![](docs/images/image2017-7-10_23.png){width="527" height="115"}

# Changelog

#### **Version 0.5.0 **

-   Add support for scheduling with higher accuracy (thanks
    [matsuu](https://github.com/matsuu) for the PR)

#### **Version 0.4.0 **

-   Add support scheduling pipeline jobs (thanks
    [neuhausjulian](https://github.com/neuhausjulian) for the PR)
-   Add possibility to configure time zone (thanks
    [ArKniiD](https://github.com/ArKniiD) for the PR)
-   Fix 404 issue when running jenkins on non-standard port
    [JENKINS-42060](https://issues.jenkins-ci.org/browse/JENKINS-42060),
    [JENKINS-31261](https://issues.jenkins-ci.org/browse/JENKINS-31261)
-   Fix https redirect error
    [JENKINS-38686](https://issues.jenkins-ci.org/browse/JENKINS-38686)
-   Fix GET trigger link error
    [JENKINS-28961](https://issues.jenkins-ci.org/browse/JENKINS-28961)
    (thanks [phippu](https://github.com/phippu) for the PR)  
      


# schedule-build-plugin
Adds capability to schedule a build for a later point in time.  Timezone is configurable within the Jenkins configuration page.

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/schedule-build-plugin/master)](https://ci.jenkins.io/job/Plugins/job/schedule-build-plugin/job/master/)

# Configuration as code

This plugin supports configuration as code

Add to your yaml file:
```yaml
unclassified:
  scheduleBuild:
    defaultScheduleTime: 23:00:00
    timeZone: Europe/Paris
```

