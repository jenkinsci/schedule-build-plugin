let d = document.getElementById("schedule-build-data");
let url = d.dataset.url;
let quietPeriodInSeconds = d.dataset.quietPeriodSeconds;
let scheduleUrl = url + "build?delay=" + quietPeriodInSeconds + "sec";

if (d.dataset.parameterized === "true") {
  window.location = d.dataset.url + "build?delay=" + quietPeriodInSeconds + "sec";
} else {
  fetch(scheduleUrl, {
    method: "post",
    headers: crumb.wrap({}),
  }).then(function() {
    window.location = url;
  });
}
