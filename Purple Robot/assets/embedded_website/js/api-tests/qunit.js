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
	asyncTest("Logger Test", 3, function()
	{
		var userId = generateUuid();
		
		var script = "PurpleRobot.persistString('event_log_params', 'study_name', 'My Test Study');";
		script += "PurpleRobot.persistString('event_log_params', 'group_id', 'My Group ID');";
		script += "PurpleRobot.persistString('event_log_params', 'username', '" + userId + "');";
		script += "PurpleRobot.persistString('event_log_params', 'user_id', PurpleRobot.fetchUserHash());";
		script += "PurpleRobot.persistString('event_log_params', 'readable_content', 'Some simple readable content.');";
		
		runScript(script, function(data)
		{
			ok(true, "Log namespace values persisted.");

			runScript("PurpleRobot.fetchString('event_log_params', 'username');", function(data)
			{
				ok(userId == data.payload, "Username matches.");

				runScript("PurpleRobot.log('test_event', {'session_id': 'my-session-id'});", function(data)
				{
					ok(data.payload, "Event logged");
	
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

pendingTests.push(function()
{
	asyncTest("Unencrypted Namespace Test", 3, function()
	{
		var key = generateUuid();
		var value = generateUuid();
		var namespace = generateUuid();

		runScript("PurpleRobot.persistString('" + namespace + "', '" + key + "', '" + value + "');", function(data)
		{
			ok(true, key + " => " + value + " persisted");

			runScript("PurpleRobot.fetchString('" + namespace + "', '" + key + "');", function(data)
			{
				ok(value == data.payload, "Fetched value matches original value.");

				runScript("PurpleRobot.fetchNamespaces();", function(data)
				{
					if (data.payload != undefined)
						ok($.inArray(namespace, data.payload), "Namespace list contains expected namespace.");
					else
						ok(false, "Namespace list returned");
	
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

			start();

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
	asyncTest("Encrypted Namespace Test", 3, function()
	{
		var key = generateUuid();
		var value = generateUuid();
		var namespace = generateUuid();

		runScript("PurpleRobot.persistEncryptedString('" + namespace + "', '" + key + "', '" + value + "');", function(data)
		{
			ok(true, key + " => " + value + " persisted");

			runScript("PurpleRobot.fetchEncryptedString('" + namespace + "', '" + key + "');", function(data)
			{
				ok(value == data.payload, "Fetched value matches original value.");

				runScript("PurpleRobot.fetchNamespaces();", function(data)
				{
					if (data.payload != undefined)
						ok($.inArray(namespace, data.payload), "Namespace list contains expected namespace.");
					else
						ok(false, "Namespace list returned");
	
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

			start();

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
	asyncTest("Namespace Contents Test", 2, function()
	{
		var key = generateUuid();
		var value = generateUuid();
		var namespace = generateUuid();

		runScript("PurpleRobot.persistString('" + namespace + "', '" + key + "', '" + value + "');", function(data)
		{
			ok(true, key + " => " + value + " persisted");

			runScript("PurpleRobot.fetchNamespace('" + namespace + "');", function(data)
			{
				ok(value == data.payload[key], "Namespace object contains original value.");

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

			start();

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
	asyncTest("Javascript Update Config Test", 2, function()
	{
		var key = "config_json_refresh_manually"; // Unused ID used in the preferences screen for button activity.
		var value = generateUuid();

		runScript("PurpleRobot.updateConfig({ '" + key + "': '" + value + "'});", function(data)
		{
			ok(true, key + " => " + value + " persisted");

			runScript("PurpleRobot.fetchConfig();", function(data)
			{
			    alert("PAY: " + data.payload);
			    
				var result = JSON.parse(data.payload);
				
				alert(value + " =? " + result[key]);
				
				var matches = (value == result[key]);

				ok(matches, key + " => " + value + " set successfully.");
	
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

			start();

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