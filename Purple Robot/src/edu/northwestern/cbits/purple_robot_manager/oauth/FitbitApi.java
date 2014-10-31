package edu.northwestern.cbits.purple_robot_manager.oauth;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

public class FitbitApi extends DefaultApi10a
{
    public FitbitApi()
    {
        super();
    }

    public String getAccessTokenEndpoint()
    {
        return "https://api.fitbit.com/oauth/access_token";
    }

    public String getAuthorizationUrl(Token token)
    {
        return "https://www.fitbit.com/oauth/authenticate?oauth_token=" + token.getToken();
    }

    public String getRequestTokenEndpoint()
    {
        return "https://api.fitbit.com/oauth/request_token";
    }
}
