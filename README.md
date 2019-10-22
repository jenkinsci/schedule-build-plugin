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
