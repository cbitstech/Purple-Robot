package edu.northwestern.cbits.purple_robot_manager.probes.services;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import edu.northwestern.cbits.purple_robot_manager.EncryptionManager;
import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityCheck;
import edu.northwestern.cbits.purple_robot_manager.logging.SanityManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;
import edu.northwestern.cbits.xsi.oauth.GitHubApi;
import edu.northwestern.cbits.xsi.oauth.Keystore;
import edu.northwestern.cbits.xsi.oauth.OAuthActivity;

public class GitHubProbe extends Probe
{
    private static final String ENABLED = "config_feature_github_probe_enabled";
    private static final boolean DEFAULT_ENABLED = false;
    private static final String OAUTH_TOKEN = "oauth_github_token";
    private static final String OAUTH_SECRET = "oauth_github_secret";
    private static final String REPOSITORY_COUNT = "REPOSITORY_COUNT";
    private static final String LOGIN = "LOGIN";
    private static final String REPOSITORY_NAME = "REPOSITORY_NAME";
    private static final String COMMITS_THIS_MONTH = "COMMITS_THIS_MONTH";
    private static final String COMMITS_TODAY = "COMMITS_TODAY";
    private static final String COMMITS_THIS_WEEK = "COMMITS_THIS_WEEK";
    private static final String REPOSITORIES = "REPOSITORIES";
    private static final String COMMITS_TOTAL = "COMMITS_TOTAL";
    private static final String LAST_REPOSITORY = "LAST_REPOSITORY";
    private static final String LAST_COMMIT_MESSAGE = "LAST_COMMIT_MESSAGE";

    private long _lastUpdate = 0;
    private long _lastCommit = 0;

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_github_probe_desc);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.GitHubProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_github_probe);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(GitHubProbe.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(GitHubProbe.ENABLED, false);
        e.commit();
    }

    private void initKeystore(Context context)
    {
        Keystore.put(GitHubApi.CONSUMER_KEY, context.getString(R.string.github_client_id));
        Keystore.put(GitHubApi.CONSUMER_SECRET, context.getString(R.string.github_client_secret));
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        final SharedPreferences prefs = Probe.getPreferences(context);

        if (super.isEnabled(context))
        {
            if (prefs.getBoolean(GitHubProbe.ENABLED, false))
            {
                this.initKeystore(context);

                String token = prefs.getString(GitHubProbe.OAUTH_TOKEN, "");

                final String title = context.getString(R.string.title_github_check);
                final SanityManager sanity = SanityManager.getInstance(context);

                final GitHubProbe me = this;
                final long now = System.currentTimeMillis();

                if (token == null || token.trim().length() == 0)
                {
                    String message = context.getString(R.string.message_github_check);

                    Runnable action = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            me.fetchAuth(context);
                        }
                    };

                    sanity.addAlert(SanityCheck.WARNING, title, message, action);
                }
                else
                {
                    Keystore.put(GitHubApi.USER_TOKEN, token);

                    sanity.clearAlert(title);

                    if (now - this._lastUpdate > 1000 * 60 * 15) // 15 min refresh interval
                    {
                        this._lastUpdate = now;

                        Runnable r = new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    Bundle bundle = new Bundle();
                                    bundle.putString(Probe.BUNDLE_PROBE, me.name(context));

                                    long lastCommit = 0;
                                    String lastRepo = null;
                                    String lastCommitMessage = null;

                                    HashSet<String> repositories = new HashSet<String>();

                                    JSONObject user = GitHubApi.fetch(Uri.parse("https://api.github.com/user"));

                                    bundle.putString(GitHubProbe.LOGIN, user.getString("login"));

                                    JSONArray allEvents = new JSONArray();

                                    for (int i = 1; i <= 10; i++)
                                    {
                                        JSONArray events = GitHubApi.fetchAll(Uri.parse("https://api.github.com/users/" + user.getString("login") + "/events?page=" + i));

                                        for (int j = 0; j < events.length(); j++)
                                        {
                                            JSONObject event = events.getJSONObject(j);

                                            allEvents.put(event);
                                        }
                                    }

                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                                    long now = System.currentTimeMillis();

                                    HashMap<String, Integer> todayCommits = new HashMap<String, Integer>();
                                    HashMap<String, Integer> weekCommits = new HashMap<String, Integer>();
                                    HashMap<String, Integer> monthCommits = new HashMap<String, Integer>();
                                    HashMap<String, Integer> totalCommits = new HashMap<String, Integer>();

                                    for (int i = 0; i < allEvents.length(); i++)
                                    {
                                        JSONObject event = allEvents.getJSONObject(i);
                                        String repository = event.getJSONObject("repo").getString("name");

                                        JSONObject payload = event.getJSONObject("payload");

                                        if (payload.has("commits"))
                                        {
                                            int commitCount = payload.getJSONArray("commits").length();

                                            Date created = sdf.parse(event.getString("created_at"));

                                            long timestamp = created.getTime();

                                            if (timestamp > lastCommit)
                                            {
                                                lastCommit = timestamp;
                                                lastRepo = repository;
                                                lastCommitMessage = payload.getJSONArray("commits").getJSONObject(0).getString("message");
                                            }

                                            Integer today = todayCommits.get(repository);

                                            if (today == null)
                                                today = 0;

                                            Integer week = weekCommits.get(repository);

                                            if (week == null)
                                                week = 0;

                                            Integer month = monthCommits.get(repository);

                                            if (month == null)
                                                month = 0;

                                            Integer total = totalCommits.get(repository);

                                            if (total == null)
                                                total = 0;

                                            if (now - timestamp < (1000 * 60 * 60 * 24))
                                                todayCommits.put(repository, today + commitCount);

                                            if (now - timestamp < (1000 * 60 * 60 * 24 * 7))
                                                weekCommits.put(repository, week + commitCount);

                                            if (now - timestamp < (1000L * 60L * 60L * 24L * 30L))
                                                monthCommits.put(repository, month + commitCount);

                                            totalCommits.put(repository, total + commitCount);

                                            repositories.add(repository);
                                        }
                                    }

                                    Bundle repositoriesBundle = new Bundle();

                                    int todayTotal = 0;
                                    int weekTotal = 0;
                                    int monthTotal = 0;
                                    int total = 0;

                                    for (String repository : repositories)
                                    {
                                        Bundle repoBundle = new Bundle();
                                        repoBundle.putString(GitHubProbe.REPOSITORY_NAME, repository);

                                        if (todayCommits.containsKey(repository))
                                        {
                                            int commits = todayCommits.get(repository);
                                            repoBundle.putInt(GitHubProbe.COMMITS_TODAY, commits);
                                            todayTotal += commits;
                                        }
                                        else
                                            repoBundle.putInt(GitHubProbe.COMMITS_TODAY, 0);

                                        if (weekCommits.containsKey(repository))
                                        {
                                            int commits = weekCommits.get(repository);
                                            repoBundle.putInt(GitHubProbe.COMMITS_THIS_WEEK, commits);
                                            weekTotal += commits;
                                        }
                                        else
                                            repoBundle.putInt(GitHubProbe.COMMITS_THIS_WEEK, 0);

                                        if (monthCommits.containsKey(repository))
                                        {
                                            int commits = monthCommits.get(repository);
                                            repoBundle.putInt(GitHubProbe.COMMITS_THIS_MONTH, commits);
                                            monthTotal += commits;
                                        }
                                        else
                                            repoBundle.putInt(GitHubProbe.COMMITS_THIS_MONTH, 0);

                                        if (totalCommits.containsKey(repository))
                                        {
                                            int commits = totalCommits.get(repository);
                                            repoBundle.putInt(GitHubProbe.COMMITS_TOTAL, commits);
                                            total += commits;
                                        }
                                        else
                                            repoBundle.putInt(GitHubProbe.COMMITS_TOTAL, 0);

                                        repositoriesBundle.putBundle(repository, repoBundle);
                                    }

                                    bundle.putBundle(GitHubProbe.REPOSITORIES, repositoriesBundle);
                                    bundle.putInt(GitHubProbe.REPOSITORY_COUNT, repositories.size());

                                    bundle.putInt(GitHubProbe.COMMITS_TODAY, todayTotal);
                                    bundle.putInt(GitHubProbe.COMMITS_THIS_WEEK, weekTotal);
                                    bundle.putInt(GitHubProbe.COMMITS_THIS_MONTH, monthTotal);
                                    bundle.putInt(GitHubProbe.COMMITS_TOTAL, total);

                                    if (lastRepo != null && lastCommitMessage != null)
                                    {
                                        bundle.putString(GitHubProbe.LAST_REPOSITORY, lastRepo);
                                        bundle.putString(GitHubProbe.LAST_COMMIT_MESSAGE, lastCommitMessage);
                                    }

                                    if (lastCommit != 0)
                                    {
                                        bundle.putLong(Probe.BUNDLE_TIMESTAMP, lastCommit / 1000);

                                        if (lastCommit > me._lastCommit)
                                        {
                                            me._lastCommit = lastCommit;
                                            me.transmitData(context, bundle);
                                        }
                                    }
                                }
                                catch (Exception e)
                                {
                                    LogManager.getInstance(context).logException(e);
                                }
                            }
                        };

                        Thread t = new Thread(r);
                        t.start();
                    }
                }

                return true;
            }
        }

        return false;
    }

    private void fetchAuth(Context context)
    {
        String userId = EncryptionManager.getInstance().getUserHash(context);

        Intent intent = new Intent(context, OAuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra(OAuthActivity.CONSUMER_KEY, context.getString(R.string.github_client_id));
        intent.putExtra(OAuthActivity.CONSUMER_SECRET, context.getString(R.string.github_client_secret));
        intent.putExtra(OAuthActivity.REQUESTER, "github");
        intent.putExtra(OAuthActivity.CALLBACK_URL, "http://tech.cbits.northwestern.edu/oauth/github");
        intent.putExtra(OAuthActivity.LOG_URL, LogManager.getInstance(context).getLogUrl(context));
        intent.putExtra(OAuthActivity.HASH_SECRET, userId);

        context.startActivity(intent);
    }

    @Override
    public JSONObject fetchSettings(Context context)
    {
        JSONObject settings = new JSONObject();

        try
        {
            JSONObject enabled = new JSONObject();
            enabled.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_BOOLEAN);
            JSONArray values = new JSONArray();
            values.put(true);
            values.put(false);
            enabled.put(Probe.PROBE_VALUES, values);
            settings.put(Probe.PROBE_ENABLED, enabled);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public PreferenceScreen preferenceScreen(final Context context, PreferenceManager manager)
    {
        final PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(this.summary(context));

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(GitHubProbe.ENABLED);
        enabled.setDefaultValue(GitHubProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        final SharedPreferences prefs = Probe.getPreferences(context);

        String token = prefs.getString(GitHubProbe.OAUTH_TOKEN, null);
        String secret = prefs.getString(GitHubProbe.OAUTH_SECRET, null);

        final Preference authPreference = new Preference(context);
        authPreference.setTitle(R.string.title_authenticate_github_probe);
        authPreference.setSummary(R.string.summary_authenticate_github_probe);

        final Preference logoutPreference = new Preference(context);
        logoutPreference.setTitle(R.string.title_logout_github_probe);
        logoutPreference.setSummary(R.string.summary_logout_github_probe);

        final GitHubProbe me = this;

        authPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                me.fetchAuth(context);

                screen.addPreference(logoutPreference);
                screen.removePreference(authPreference);

                return true;
            }
        });

        logoutPreference.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Editor e = prefs.edit();
                e.remove(GitHubProbe.OAUTH_TOKEN);
                e.remove(GitHubProbe.OAUTH_SECRET);
                e.commit();

                me._lastUpdate = 0;

                screen.addPreference(authPreference);
                screen.removePreference(logoutPreference);

                if (context instanceof Activity)
                {
                    Activity activity = (Activity) context;
                    activity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            Toast.makeText(context, context.getString(R.string.toast_github_logout), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                return true;
            }
        });

        if (token == null || secret == null)
            screen.addPreference(authPreference);
        else
            screen.addPreference(logoutPreference);

        return screen;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        String lastCommit = bundle.getString(GitHubProbe.LAST_COMMIT_MESSAGE);
        String lastRepo = bundle.getString(GitHubProbe.LAST_REPOSITORY);

        String[] tokens = lastRepo.split("/");

        if (tokens.length > 1)
            lastRepo = tokens[1];

        return String.format(context.getResources().getString(R.string.summary_github), lastRepo, lastCommit);
    }
}
