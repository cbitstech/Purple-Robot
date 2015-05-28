package edu.northwestern.cbits.purple_robot_manager.probes.services;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;

import edu.northwestern.cbits.purple_robot_manager.R;
import edu.northwestern.cbits.purple_robot_manager.activities.settings.FlexibleListPreference;
import edu.northwestern.cbits.purple_robot_manager.logging.LogManager;
import edu.northwestern.cbits.purple_robot_manager.probes.Probe;

public class GooglePlacesProbe extends Probe implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private static final String DEFAULT_RADIUS = "1000";

    private static final String RADIUS = "config_probe_google_places_radius";
    private static final String ENABLED = "config_probe_google_places_enabled";
    private static final boolean DEFAULT_ENABLED = false;
    private static final String MOST_LIKELY_PLACE = "MOST_LIKELY_PLACE";
    private static final String MOST_LIKELY_PLACE_ID = "MOST_LIKELY_PLACE_ID";
    private static final String MOST_LIKELY_PLACE_LIKELIHOOD = "MOST_LIKELY_PLACE_LIKELIHOOD";

    private static String[] EXCLUDED_TYPES = { "establishment" };

    protected long _lastCheck = 0;

    private GoogleApiClient _client = null;

    @Override
    public String summary(Context context)
    {
        return context.getString(R.string.summary_google_places_probe_desc);
    }

    @Override
    public String name(Context context)
    {
        return "edu.northwestern.cbits.purple_robot_manager.probes.services.GooglePlacesProbe";
    }

    @Override
    public String title(Context context)
    {
        return context.getString(R.string.title_google_places_probe);
    }

    @Override
    public String probeCategory(Context context)
    {
        return context.getResources().getString(R.string.probe_external_services_category);
    }

    @Override
    public void enable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(GooglePlacesProbe.ENABLED, true);
        e.commit();
    }

    @Override
    public void disable(Context context)
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        Editor e = prefs.edit();
        e.putBoolean(GooglePlacesProbe.ENABLED, false);
        e.commit();
    }

    @Override
    public boolean isEnabled(final Context context)
    {
        if (super.isEnabled(context))
        {
            final SharedPreferences prefs = Probe.getPreferences(context);

            if (prefs.getBoolean(GooglePlacesProbe.ENABLED, GooglePlacesProbe.DEFAULT_ENABLED))
            {
                long now = System.currentTimeMillis();

                if (now - this._lastCheck > 300000) // 5 minutes
                {
                    final GooglePlacesProbe me = this;

                    if (this._client == null)
                    {
                        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(context);
                        builder = builder.addApi(Places.GEO_DATA_API);
                        builder = builder.addApi(Places.PLACE_DETECTION_API);
                        builder.addConnectionCallbacks(this);
                        builder.addOnConnectionFailedListener(this);

                        this._client = builder.build();
                        this._client.connect();

                        this._lastCheck = 0;
                    }
                    else if (this._client.isConnected())
                    {
                        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(this._client, null);

                        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>()
                        {
                             public void onResult(PlaceLikelihoodBuffer likelyPlaces)
                             {
                                 LocationManager locations = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

                                 Location last = null;

                                 if (locations.isProviderEnabled(LocationManager.GPS_PROVIDER))
                                     last = locations.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                 else if (locations.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                                     last = locations.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                                 else
                                     last = locations.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

                                 double radius = Double.parseDouble(prefs.getString(GooglePlacesProbe.RADIUS, GooglePlacesProbe.DEFAULT_RADIUS));

                                 if (last == null)
                                     radius = Double.MAX_VALUE;

                                 float mostLikelihood = 0;
                                 String mostLikelyPlaceName = null;
                                 String mostLikelyPlaceId = null;

                                 HashMap<String, Integer> places = new HashMap<>();

                                 for (PlaceLikelihood placeLikelihood : likelyPlaces)
                                 {
                                     Place place = placeLikelihood.getPlace();

                                     Location placeLocation = new Location(LocationManager.PASSIVE_PROVIDER);
                                     placeLocation.setLatitude(place.getLatLng().latitude);
                                     placeLocation.setLongitude(place.getLatLng().longitude);

                                     if (last == null || Math.abs(last.distanceTo(placeLocation)) < radius)
                                     {
                                         List<Integer> types = place.getPlaceTypes();

                                         for (Integer type : types)
                                         {
                                             String typeString = GooglePlacesProbe.stringForType(type);

                                             Integer count = places.get(typeString);

                                             if (count == null)
                                                 count = 0;

                                             count += 1;

                                             places.put(typeString, count);
                                         }

                                         if (placeLikelihood.getLikelihood() > mostLikelihood)
                                         {
                                             mostLikelyPlaceId = place.getId();
                                             mostLikelyPlaceName = place.getName().toString();
                                             mostLikelihood = placeLikelihood.getLikelihood();
                                         }
                                     }
                                 }

                                 likelyPlaces.release();

                                Bundle bundle = new Bundle();
                                bundle.putString("PROBE", me.name(context));
                                bundle.putLong("TIMESTAMP", System.currentTimeMillis() / 1000);

                                 if (mostLikelyPlaceName != null)
                                 {
                                     bundle.putString(GooglePlacesProbe.MOST_LIKELY_PLACE, mostLikelyPlaceName);
                                     bundle.putString(GooglePlacesProbe.MOST_LIKELY_PLACE_ID, mostLikelyPlaceId);
                                     bundle.putFloat(GooglePlacesProbe.MOST_LIKELY_PLACE_LIKELIHOOD, mostLikelihood);
                                 }

                                for (String key : places.keySet())
                                {
                                    if (Arrays.asList(GooglePlacesProbe.EXCLUDED_TYPES).contains(key) == false)
                                        bundle.putInt(key, places.get(key));
                                }

                                me.transmitData(context, bundle);
                            }
                        });
                    }
                }

                return true;
            }
        }

        return false;
    }

    protected static Map<String, Integer> nearestLocation(Context context, double latitude, double longitude) throws Exception
    {
        SharedPreferences prefs = Probe.getPreferences(context);

        String key = context.getString(R.string.google_places_browser_key);

        String radius = prefs.getString(GooglePlacesProbe.RADIUS, GooglePlacesProbe.DEFAULT_RADIUS);

        URL u = new URL("https://maps.googleapis.com/maps/api/place/search/json?location=" + latitude + "," + longitude + "&radius=" + radius + "&sensor=false&key=" + key);
        InputStream in = u.openStream();

        String jsonString = IOUtils.toString(in);

        in.close();

        JSONObject json = new JSONObject(jsonString);

        JSONArray results = json.getJSONArray("results");

        HashMap<String, Integer> place = new HashMap<>();

        String[] availableTypes = context.getResources().getStringArray(R.array.google_places_types);

        for (String type : availableTypes)
        {
            place.put(type, 0);
        }

        for (int i = 0; i < results.length(); i++)
        {
            JSONObject result = results.getJSONObject(i);

            JSONArray types = result.getJSONArray("types");

            for (int j = 0; j < types.length(); j++)
            {
                String type = types.getString(j);

                Integer count = place.get(type);

                if (count == null)
                    count = 0;

                count = count.intValue() + 1;

                place.put(type, count);
            }
        }

        return place;
    }

    @Override
    public String summarizeValue(Context context, Bundle bundle)
    {
        String frequentPlace = "none";
        int maxCount = 0;

        for (String key : bundle.keySet())
        {
            Object o = bundle.get(key);

            if ("TIMESTAMP".equals(key))
            {

            }
            else if (o instanceof Integer)
            {
                Integer count = (Integer) o;

                if (count > maxCount)
                {
                    frequentPlace = key;
                    maxCount = count;
                }
            }
            else if (o instanceof Double)
            {
                Double count = (Double) o;

                if (count.intValue() > maxCount)
                {
                    frequentPlace = key;
                    maxCount = count.intValue();
                }
            }
        }

        return String.format(context.getResources().getString(R.string.summary_google_places), frequentPlace.replaceAll("_", " "), maxCount);
    }

    @Override
    public PreferenceScreen preferenceScreen(Context context, PreferenceManager manager)
    {
        final PreferenceScreen screen = manager.createPreferenceScreen(context);
        screen.setTitle(this.title(context));
        screen.setSummary(this.summary(context));

        CheckBoxPreference enabled = new CheckBoxPreference(context);
        enabled.setTitle(R.string.title_enable_probe);
        enabled.setKey(GooglePlacesProbe.ENABLED);
        enabled.setDefaultValue(GooglePlacesProbe.DEFAULT_ENABLED);

        screen.addPreference(enabled);

        FlexibleListPreference radius = new FlexibleListPreference(context);
        radius.setKey(GooglePlacesProbe.RADIUS);
        radius.setEntryValues(R.array.feature_google_places_values);
        radius.setEntries(R.array.feature_google_places_labels);
        radius.setTitle(R.string.feature_google_places_radius_label);
        radius.setDefaultValue(GooglePlacesProbe.DEFAULT_RADIUS);

        screen.addPreference(radius);

        return screen;
    }

    @Override
    public Map<String, Object> configuration(Context context)
    {
        Map<String, Object> map = super.configuration(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        map.put(GooglePlacesProbe.RADIUS, Double.parseDouble(prefs.getString(GooglePlacesProbe.RADIUS, GooglePlacesProbe.DEFAULT_RADIUS)));

        return map;
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

            JSONObject radius = new JSONObject();
            radius.put(Probe.PROBE_TYPE, Probe.PROBE_TYPE_LONG);
            values = new JSONArray();

            String[] options = context.getResources().getStringArray(R.array.feature_google_places_values);

            for (String option : options)
            {
                values.put(Double.parseDouble(option));
            }

            radius.put(Probe.PROBE_VALUES, values);
            settings.put(GooglePlacesProbe.RADIUS, radius);
        }
        catch (JSONException e)
        {
            LogManager.getInstance(context).logException(e);
        }

        return settings;
    }

    @Override
    public void updateFromMap(Context context, Map<String, Object> params)
    {
        super.updateFromMap(context, params);

        if (params.containsKey(GooglePlacesProbe.RADIUS))
        {
            Object radius = params.get(GooglePlacesProbe.RADIUS);

            if (radius instanceof Double)
            {
                SharedPreferences prefs = Probe.getPreferences(context);
                Editor e = prefs.edit();

                e.putString(GooglePlacesProbe.RADIUS, radius.toString());
                e.commit();
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {

    }

    private static String stringForType(int type)
    {
        switch(type) {
            case Place.TYPE_ACCOUNTING:
                return "accounting";
            case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_1:
                return "administrative_level_1";
            case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_2:
                return "administrative_level_2";
            case Place.TYPE_ADMINISTRATIVE_AREA_LEVEL_3:
                return "administrative_level_3";
            case Place.TYPE_AMUSEMENT_PARK:
                return "amusement_park";
            case Place.TYPE_AQUARIUM:
                return "aquarium";
            case Place.TYPE_ART_GALLERY:
                return "art_gallery";
            case Place.TYPE_ATM:
                return "atm";
            case Place.TYPE_BAKERY:
                return "bakery";
            case Place.TYPE_BANK:
                return "bank";
            case Place.TYPE_BAR:
                return "bar";
            case Place.TYPE_BEAUTY_SALON:
                return "beauty_salon";
            case Place.TYPE_BICYCLE_STORE:
                return "bicycle_store";
            case Place.TYPE_BOWLING_ALLEY:
                return "bowling_alley";
            case Place.TYPE_BUS_STATION:
                return "bus_station";
            case Place.TYPE_CAFE:
                return "cafe";
            case Place.TYPE_CAMPGROUND:
                return "campground";
            case Place.TYPE_CAR_DEALER:
                return "car_dealer";
            case Place.TYPE_CAR_RENTAL:
                return "car_rental";
            case Place.TYPE_CAR_REPAIR:
                return "car_repair";
            case Place.TYPE_CAR_WASH:
                return "car_wash";
            case Place.TYPE_CASINO:
                return "casino";
            case Place.TYPE_CEMETERY:
                return "cemetery";
            case Place.TYPE_CHURCH:
                return "church";
            case Place.TYPE_CITY_HALL:
                return "city_hall";
            case Place.TYPE_CLOTHING_STORE:
                return "clothing_store";
            case Place.TYPE_COLLOQUIAL_AREA:
                return "colloquial_area";
            case Place.TYPE_CONVENIENCE_STORE:
                return "convenience_store";
            case Place.TYPE_COURTHOUSE:
                return "courthouse";
            case Place.TYPE_DENTIST:
                return "dentist";
            case Place.TYPE_DEPARTMENT_STORE:
                return "department_store";
            case Place.TYPE_DOCTOR:
                return "doctor";
            case Place.TYPE_ELECTRICIAN:
                return "electrician";
            case Place.TYPE_ELECTRONICS_STORE:
                return "electronics_store";
            case Place.TYPE_EMBASSY:
                return "embassy";
            case Place.TYPE_ESTABLISHMENT:
                return "establishment";
            case Place.TYPE_FINANCE:
                return "finance";
            case Place.TYPE_FIRE_STATION:
                return "fire_station";
            case Place.TYPE_FLOOR:
                return "floor";
            case Place.TYPE_FLORIST:
                return "florist";
            case Place.TYPE_FOOD:
                return "food";
            case Place.TYPE_FUNERAL_HOME:
                return "funeral_home";
            case Place.TYPE_FURNITURE_STORE:
                return "furniture_store";
            case Place.TYPE_GAS_STATION:
                return "gas_station";
            case Place.TYPE_GENERAL_CONTRACTOR:
                return "general_contractor";
            case Place.TYPE_GEOCODE:
                return "geocode";
            case Place.TYPE_GROCERY_OR_SUPERMARKET:
                return "grocery_or_supermarket";
            case Place.TYPE_GYM:
                return "gym";
            case Place.TYPE_HAIR_CARE:
                return "hair_care";
            case Place.TYPE_HARDWARE_STORE:
                return "hardware_store";
            case Place.TYPE_HEALTH:
                return "health";
            case Place.TYPE_HINDU_TEMPLE:
                return "hindu_temple";
            case Place.TYPE_HOME_GOODS_STORE:
                return "home_goods_store";
            case Place.TYPE_HOSPITAL:
                return "hospital";
            case Place.TYPE_INSURANCE_AGENCY:
                return "insurance_agency";
            case Place.TYPE_INTERSECTION:
                return "intersection";
            case Place.TYPE_JEWELRY_STORE:
                return "jewelry_store";
            case Place.TYPE_LAUNDRY:
                return "laundry";
            case Place.TYPE_LAWYER:
                return "lawyer";
            case Place.TYPE_LIBRARY:
                return "library";
            case Place.TYPE_LIQUOR_STORE:
                return "liquor_store";
            case Place.TYPE_LOCALITY:
                return "locality";
            case Place.TYPE_LOCAL_GOVERNMENT_OFFICE:
                return "local_government_office";
            case Place.TYPE_LOCKSMITH:
                return "locksmith";
            case Place.TYPE_LODGING:
                return "lodging";
            case Place.TYPE_MEAL_DELIVERY:
                return "meal_delivery";
            case Place.TYPE_MEAL_TAKEAWAY:
                return "meal_takeaway";
            case Place.TYPE_MOSQUE:
                return "mosque";
            case Place.TYPE_MOVIE_RENTAL:
                return "movie_rental";
            case Place.TYPE_MOVIE_THEATER:
                return "movie_theater";
            case Place.TYPE_MOVING_COMPANY:
                return "moving_company";
            case Place.TYPE_MUSEUM:
                return "museum";
            case Place.TYPE_NATURAL_FEATURE:
                return "natural_feature";
            case Place.TYPE_NEIGHBORHOOD:
                return "neighborhood";
            case Place.TYPE_NIGHT_CLUB:
                return "night_club";
            case Place.TYPE_OTHER:
                return "other";
            case Place.TYPE_PAINTER:
                return "painter";
            case Place.TYPE_PARK:
                return "park";
            case Place.TYPE_PARKING:
                return "parking";
            case Place.TYPE_PET_STORE:
                return "pet_store";
            case Place.TYPE_PHARMACY:
                return "pharmacy";
            case Place.TYPE_PHYSIOTHERAPIST:
                return "physiotherapist";
            case Place.TYPE_PLACE_OF_WORSHIP:
                return "place_of_worship";
            case Place.TYPE_PLUMBER:
                return "plumber";
            case Place.TYPE_POINT_OF_INTEREST:
                return "point_of_interest";
            case Place.TYPE_POLICE:
                return "police";
            case Place.TYPE_POLITICAL:
                return "political";
            case Place.TYPE_POSTAL_CODE:
                return "postal_code";
            case Place.TYPE_POSTAL_CODE_PREFIX:
                return "postal_code_prefix";
            case Place.TYPE_POSTAL_TOWN:
                return "postal_town";
            case Place.TYPE_POST_BOX:
                return "post_box";
            case Place.TYPE_POST_OFFICE:
                return "post_office";
            case Place.TYPE_PREMISE:
                return "premise";
            case Place.TYPE_REAL_ESTATE_AGENCY:
                return "real_estate_agency";
            case Place.TYPE_RESTAURANT:
                return "restaurant";
            case Place.TYPE_ROOFING_CONTRACTOR:
                return "roofing_contractor";
            case Place.TYPE_ROOM:
                return "room";
            case Place.TYPE_ROUTE:
                return "route";
            case Place.TYPE_RV_PARK:
                return "rv_park";
            case Place.TYPE_SCHOOL:
                return "school";
            case Place.TYPE_SHOE_STORE:
                return "show_store";
            case Place.TYPE_SHOPPING_MALL:
                return "shopping_mall";
            case Place.TYPE_SPA:
                return "spa";
            case Place.TYPE_STADIUM:
                return "stadium";
            case Place.TYPE_STORAGE:
                return "storage";
            case Place.TYPE_STORE:
                return "store";
            case Place.TYPE_STREET_ADDRESS:
                return "street_address";
            case Place.TYPE_SUBLOCALITY:
                return "sublocality";
            case Place.TYPE_SUBLOCALITY_LEVEL_1:
                return "sublocality_level_1";
            case Place.TYPE_SUBLOCALITY_LEVEL_2:
                return "sublocality_level_2";
            case Place.TYPE_SUBLOCALITY_LEVEL_3:
                return "sublocality_level_3";
            case Place.TYPE_SUBLOCALITY_LEVEL_4:
                return "sublocality_level_4";
            case Place.TYPE_SUBLOCALITY_LEVEL_5:
                return "sublocality_level_5";
            case Place.TYPE_SUBPREMISE:
                return "subpremise";
            case Place.TYPE_SUBWAY_STATION:
                return "subway_station";
            case Place.TYPE_SYNAGOGUE:
                return "synagogue";
            case Place.TYPE_SYNTHETIC_GEOCODE:
                return "synthetic_geocode";
            case Place.TYPE_TAXI_STAND:
                return "taxi_stand";
            case Place.TYPE_TRAIN_STATION:
                return "train_station";
            case Place.TYPE_TRANSIT_STATION:
                return "transit_station";
            case Place.TYPE_TRAVEL_AGENCY:
                return "travel_agency";
            case Place.TYPE_UNIVERSITY:
                return "university";
            case Place.TYPE_VETERINARY_CARE:
                return "veterinary_care";
            case Place.TYPE_ZOO:
                return "zoo";
        }

        return "unknown";
    }
}
