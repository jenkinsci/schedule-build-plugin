# Schedule Build plugin

Adds capability to schedule a build for a later point in time. Asks the
user for a date and time and adds the build to the build queue with the
respective quiet period.

## Scheduling Builds

Press the "Schedule Build" link on the project page

![](docs/images/Schedule_Project_Page.png)

or use the schedule build action in the list view.

![](docs/images/Schedule_Action.png)  

Then select date and time when to schedule the build.

![](docs/images/Schedule_Page.png)  

The build will be added to the build queue with the respective quiet
period.

![](docs/images/Schedule_Build_Queue.png)

## Scheduling parameterized jobs

Parameterized jobs can also be scheduled with the plugin.
The parameter page for the job is displayed to the user immediately after the "Schedule" button is pressed.
Once the parameter values are selected, the job will be scheduled.

## Configure Schedule Build Plugin

The configuration of the schedule build plugin is very simple. There are
only two parameters on the Jenkins system configuration page.

The default time which is set when a user wants to schedule a build may
be configured and time zone used by the plugin, which might differ from
the system time zone.

![](docs/images/Schedule_Timezone.png)

## Configuration as code

This plugin supports configuration as code

Add to your yaml file:
```yaml
unclassified:
  scheduleBuild:
    defaultStartTime: "11:00:00 PM"
    timeZone: "Europe/Paris"
```

## Release Notes

* For recent versions, see [GitHub Releases](https://github.com/jenkinsci/schedule-build-plugin/releases)
* For versions 0.5.0 and older, see the [changelog archive](https://github.com/jenkinsci/schedule-build-plugin/blob/schedule-build-1.0.0/CHANGELOG.md)

## Report an Issue

Please report issues and enhancements through the [Jenkins issue tracker](https://www.jenkins.io/participate/report-issue/redirect/#18422).
