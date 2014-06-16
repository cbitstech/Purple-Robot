var dayOfYear = function () {
	var yearTime = new Date().setFullYear(new Date().getFullYear(), 0, 1);
	var firstDay = Math.floor(yearTime / 86400000);
	var today = Math.ceil((new Date().getTime()) / 86400000);

	return today - firstDay;
};

var fractionalYear = function () {
	var now = new Date();

	return ((2 * Math.PI) / 365.25) * (dayOfYear() - 1 + ((now.getHours() - 12) / 24));
};

var eqTime = function () {
	var fractYear = fractionalYear();

	var time = 229.18 * (0.000075 + (0.001868 * Math.cos(fractYear)) - (0.032077 * Math.sin(fractYear)) -
						   (0.014615 * Math.cos(2 * fractYear)) - (0.040849 * Math.sin(2 * fractYear)));

	return time;
};

var solarDeclination = function () {
	var fractYear = fractionalYear();

	var decl = 0.006918 - (0.399912 * Math.cos(fractYear)) + (0.070257 * Math.sin(fractYear)) - (0.006758 * Math.cos(2 * fractYear)) +
			   (0.000907 * Math.sin(2 * fractYear)) - (0.002697 * Math.cos(3 * fractYear)) + (0.00148 * Math.sin(3 * fractYear));

	return decl;
};

var radiansForDegrees = function (degrees) {
	return (Math.PI / 180) * degrees;
};

var degreesForRadians = function (radians) {
	return (180 / Math.PI) * radians;
};

var hourAngleForLocation = function (latitude, longitude) {
	var decl = solarDeclination();

	var angle = Math.acos((Math.cos(radiansForDegrees(90.833)) / (Math.cos(radiansForDegrees(latitude)) * Math.cos(decl))) -
				(Math.tan(radiansForDegrees(latitude)) * Math.tan(decl)));

	return angle;
};

var sunriseForCoordinate = function (latitude, longitude) {
	var angle = hourAngleForLocation(latitude, longitude);

	return 720 + (4 * (longitude - degreesForRadians(angle))) - eqTime();
};

var sunsetForCoordinate = function (latitude, longitude) {
	var angle = hourAngleForLocation(latitude, longitude);

	return 720 + (4 * (longitude + degreesForRadians(angle))) - eqTime();
};

var solarNoonForLocation = function (latitude, longitude) {
	return 720 + (4 * longitude) - eqTime();
};

var sunriseForLocation = function (latitude, longitude) {
	var now = new Date();

	return sunriseForCoordinate(latitude, longitude) + now.getTimezoneOffset();
};

var sunsetForLocation = function (latitude, longitude) {
	var now = new Date();

	return sunsetForCoordinate(latitude, longitude) + now.getTimezoneOffset();
};

var solarObjectForLocation = function(latitude, longitude) {
	var sunrise = sunriseForLocation(latitude, longitude) ;
	var sunset = sunsetForLocation(latitude, longitude);
	var equationTime = eqTime();
	var solarDecl = degreesForRadians(solarDeclination());
	var dayLength = sunset - sunrise;

	var riseString = Math.floor(sunrise / 60) + ":" + Math.floor(sunrise % 60);
	var setString = Math.floor(sunset / 60) + ":" + Math.floor(sunset % 60);

	return {
			"sunrise_minutes": sunrise,
			"sunset_minutes": sunset,
			"equation_of_time": equationTime,
			"solar_declination": solarDecl,
			"sunrise_desc": riseString,
			"sunset_desc": setString
	};
}
