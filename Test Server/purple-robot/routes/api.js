var template  = require('swig');

function render_template(path, context)
{
	var tmpl = template.compileFile(__dirname + '/../views/' + path);

	return tmpl.render(context);
}

function send_error(res, code, error_message)
{
	var crypto = require('crypto');
	var md5 = crypto.createHash('md5');

	var error_obj = {};

	error_obj.status = "error";
	error_obj.message = error_message;

	md5.update(error_obj.status + error_obj.message);

	error_obj.checksum = md5.digest("hex");

	res.setHeader('Content-Type', 'application/json');
	res.send(code, JSON.stringify(error_obj, null, 2))
}

function send_payload(res, operation, status_message, payload_obj)
{
	var crypto = require('crypto');
	var md5 = crypto.createHash('md5');

	var result_obj = {};

	result_obj.status = "ok";
	result_obj.message = status_message;
	result_obj.operation = operation;

	var payload_str = JSON.stringify(payload_obj);

	result_obj.payload = payload_str;

	md5.update(result_obj.status + result_obj.message + result_obj.operation + payload_str);

	result_obj.checksum = md5.digest("hex");

	res.setHeader('Content-Type', 'application/json');
	res.send(200, JSON.stringify(result_obj, null, 2))
}


/* 
 * POST endpoint for receiving sensor data.
 */

exports.submit = function(req, res)
{
	var message = JSON.parse(req.body.json); 

	console.log(JSON.stringify(message, null, 2));

	send_payload(res, "echo_payload", "Message received successfully.", JSON.parse(message.payload));
};

exports.validateMessage = function(req, res, next)
{
	try
	{
		var message = JSON.parse(req.body.json); 

		return next();
	}
	catch (err)
	{
		send_error(res, "400", "Invalid JSON syntax. Please try again.")
	}
};

exports.testSubmitPage = function(req, res)
{
	console.log("in test page call");

	var crypto = require('crypto');
	var md5 = crypto.createHash('md5');

	var payload = {}
	payload.name = "(App-specific name)";
	payload.value = "(Payload-specific value)";

	var json = {};
	json.operation = "test_submit";
	json.payload = JSON.stringify(payload);

	md5.update(json.operate + json.payload);

	json.checksum = md5.digest("hex");

	res.send(render_template('api_submit.html', { json_value: JSON.stringify(json, null, 2) }));
};

exports.loadRoutes = function(app, api)
{
	app.get('/api/submit', exports.testSubmitPage)
	app.post('/api/submit', exports.validateMessage, exports.submit);
};
