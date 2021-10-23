var newRequest = function() {
	if (window.XMLHttpRequest) {
		return new XMLHttpRequest();
	} else {
		return new ActiveXObject("Microsoft.XMLHTTP");
	}
}

var sumbitScheduleRequest = function(absoluteUrl, quietPeriodInSeconds, isJobParameterized){

	if(isJobParameterized){
		// if job has parameters, redirect to build page, so user can set parameters
		window.location = absoluteUrl + "build?delay=" + quietPeriodInSeconds + "sec";
	}else{
		// if job has NO parameters, submit build directly
		var csrfCrumb;
		var csrfRequest = newRequest();
		csrfRequest.onreadystatechange = function() {
			if (csrfRequest.readyState === 4) {
				if (csrfRequest.status === 200 || csrfRequest.status === 201) {
					csrfCrumb = JSON.parse(csrfRequest.responseText);
				} else {
					// csrf might be deactivated
				}

				// do the actual submit
				var xmlhttp = newRequest();
				xmlhttp.onreadystatechange = function() {
					if (xmlhttp.readyState === 4) {
						if (xmlhttp.status === 200 || xmlhttp.status === 201) {
							window.location = absoluteUrl;
							return false;
						} else {
							window.location = absoluteUrl;
							return false;
						}
					}
				};
				xmlhttp.open("POST", absoluteUrl + "build?delay=" + quietPeriodInSeconds + "sec", true);
				if (csrfCrumb) {
					xmlhttp.setRequestHeader(csrfCrumb.crumbRequestField, csrfCrumb.crumb)
				}
				xmlhttp.send();
			}
		};

		csrfRequest.open('GET', rootURL + '/crumbIssuer/api/json', false);
		csrfRequest.send();
	}
}
