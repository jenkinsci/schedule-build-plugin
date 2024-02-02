Behaviour.specify("#schedule-build-input", "schedule-build-input", 0, function(input) {
  input.onkeyup = function(event) {
    input.onchange();
  }
});
