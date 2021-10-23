# Contributing to the Schedule Build Plugin

Plugin source code is hosted on [GitHub](https://github.com/jenkinsci/schedule-build-plugin).
New feature proposals and bug fix proposals should be submitted as
[GitHub pull requests](https://help.github.com/articles/creating-a-pull-request).
Your pull request will be evaluated by the [Jenkins job](https://ci.jenkins.io/job/Plugins/job/schedule-build-plugin/).

Before submitting your change, please assure that you've added tests that verify the change.

## Code Coverage

Code coverage reporting is available as a maven target.
Please try to improve code coverage with tests when you submit.

* `mvn -P enable-jacoco clean install jacoco:report` to report code coverage

## Static Analysis

Please don't introduce new spotbugs output.

* `mvn spotbugs:check` to analyze project using [Spotbugs](https://spotbugs.github.io)
* `mvn spotbugs:gui` to review report using GUI

## File format

Files in the repository are in Unix format (LF line terminators).
Please continue using Unix file format for consistency.

## Reporting Issues

Report issues in the [Jenkins issue tracker](https://www.jenkins.io/participate/report-issue/redirect/#18422).
