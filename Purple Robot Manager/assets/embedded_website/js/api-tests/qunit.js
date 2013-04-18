var runScript = function(script, success, failure)
{
	var json = {};
	json.command = "execute_script";
	json.script = script;

	var post_data = {};
	post_data.json = JSON.stringify(json);

	$.ajax("/json/submit", {
		type: "POST",
		contentType: "application/x-www-form-urlencoded; charset=UTF-8", 

		data: post_data,
		success: function(data)
		{
			if (success != null)
				success(data);
		},
		error: function(jqXHR, textStatus, errorThrown) 
		{ 
			if (failure != null)
				failure(jqXHR, textStatus, errorThrown);
		}
	});
};

var generateUuid = function()
{
	return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) 
	{
    	var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
	    return v.toString(16);
	});
};

var pendingTests = [];

pendingTests.push(function()
{
	asyncTest("Unencrypted String Getters & Setters Test", 2, function()
	{
		var key = generateUuid();
		var value = generateUuid();
		
		runScript("PurpleRobot.persistString('" + key + "', '" + value + "');", function(data)
		{
			ok(true, key + " => " + value + " persisted");

			runScript("PurpleRobot.fetchString('" + key + "');", function(data)
			{
				ok(value == data.payload, "Fetched value matches original value");

				start();
			
				if (pendingTests.length > 0)
				{
					var next = pendingTests.pop();
					next();				
				}
			}, function(jqXHR, textStatus, errorThrown)
			{
				ok(false, "Encountered error: " + errorThrown);

				start();

				if (pendingTests.length > 0)
				{
					var next = pendingTests.pop();
					next();				
				}
			});
		}, function(jqXHR, textStatus, errorThrown)
		{
			ok(false, "Encountered error: " + errorThrown);

			if (pendingTests.length > 0)
			{
				var next = pendingTests.pop();
				next();				
			}
		});
	});
});

pendingTests.push(function()
{
	asyncTest("Encrypted String Getters & Setters Test", 2, function()
	{
		var key = generateUuid();
		var value = generateUuid();
		
		runScript("PurpleRobot.persistEncryptedString('" + key + "', '" + value + "');", function(data)
		{
			ok(true, key + " => " + value + " persisted");

			runScript("PurpleRobot.fetchEncryptedString('" + key + "');", function(data)
			{
				ok(value == data.payload, "Fetched value matches original value");

				start();
			
				if (pendingTests.length > 0)
				{
					var next = pendingTests.pop();
					next();				
				}
			}, function(jqXHR, textStatus, errorThrown)
			{
				ok(false, "Encountered error: " + errorThrown);

				start();

				if (pendingTests.length > 0)
				{
					var next = pendingTests.pop();
					next();				
				}
			});
		}, function(jqXHR, textStatus, errorThrown)
		{
			ok(false, "Encountered error: " + errorThrown);

			if (pendingTests.length > 0)
			{
				var next = pendingTests.pop();
				next();				
			}
		});
	});
});

pendingTests.push(function ()
{
	asyncTest("playDefaultTone Test", function()
	{
		runScript("PurpleRobot.playDefaultTone();", function(data)
		{
			ok(true, "Test tone played.");
			start();
			
			if (pendingTests.length > 0)
			{
				var next = pendingTests.pop();
				next();				
			}
		}, function(jqXHR, textStatus, errorThrown)
		{
			ok(false, "Encountered error: " + errorThrown);

			if (pendingTests.length > 0)
			{
				var next = pendingTests.pop();
				next();				
			}
		});
	});
});

var go = pendingTests.pop();
go();