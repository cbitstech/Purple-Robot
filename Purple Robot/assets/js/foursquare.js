var clientId = function()
{
	return "LPF5BLAFJNO1MRGULIOYFKBLWMGQTFAR3WPHZJ1JLDD24S25";
};

var clientSecret = function()
{
	return "O1N4EAXORJSLT50PSUOSYYIONS0MCQMWAKK1I1ZLSP0TIKGU";
};

var venuesUrlForLocation = function(latitude, longitude)
{
	var endpoint = "https://api.foursquare.com/v2/venues/search?"
	
	endpoint += "client_id=" + clientId();
	endpoint += "&client_secret=" + clientSecret();
	endpoint += "&v=20121212&intent=browse";
	endpoint += "&radius=400";
	endpoint += "&ll=" + latitude + "," + longitude;
	endpoint += "&limit=50";
	
	PurpleRobot.log("ENDPOINT: " + endpoint);
	
	return endpoint;
};

var venuesForLocation = function(latitude, longitude)
{
	var jsonString = PurpleRobot.readUrl(venuesUrlForLocation(latitude, longitude));
	
	PurpleRobot.log(jsonString);

	return JSON.parse(jsonString);
};

var metadataForLocation = function(latitude, longitude)
{
PurpleRobot.log("1 " + latitude + " " + longitude);
	var reply = venuesForLocation(latitude, longitude);
	
PurpleRobot.log("2 " + reply.response);
	var metadata = {};

PurpleRobot.log("3 " + reply.response.venues);
	
	metadata["venue_count"] = reply.response.venues.length;
	metadata["venues"] = reply.response.venues;

	PurpleRobot.log(reply.response.venues.length);
	
	return metadata;
};
