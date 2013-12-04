package edu.northwestern.cbits.purple_robot_manager.oauth;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

public class TwitterApi extends DefaultApi10a 
{
	public TwitterApi()
	{
		super();
	}
	
	public String getAccessTokenEndpoint() 
	{
		return "https://api.twitter.com/oauth/access_token";
	}

	public String getAuthorizationUrl(Token token) 
	{
		return "https://api.twitter.com/oauth/authorize?oauth_token=" + token.getToken();
	}

	public String getRequestTokenEndpoint() 
	{
		return "https://api.twitter.com/oauth/request_token";
	}
}
