Behaviour.specify("#schedule-build-input", "schedule-build-input", 0, function(input) {
  input.onkeyup = function(event) {
    input.onchange();
  }
});

let fp = document.getElementById("schedule-build-flatpickr");
flatpickr(fp, {
  allowInput: true,
  enableTime: true,
  enableSeconds: true,
  wrap: true,
  clickOpens: false,
  dateFormat: "d-m-Y H:i:S",
  time_24hr: true,
  positionElement: fp.querySelector("button"),
  minDate: fp.dataset.now,
})

