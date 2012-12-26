package com.wiley.aoa.kitchen_lamp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

public class WeatherHandler extends AsyncTask<String, Void, Void> {

	private static final String SERVER = "http://weather.yahooapis.com/forecastjson";
	public static final String WEATHER_EVENT = "com.wiley.wrox.kitchen_lamp.WeatherHandler.WEATHER_EVENT";

	private Context context;

	public WeatherHandler(Context context) {
		this.context = context;
	}

	private String doGet(String WOEID) throws URISyntaxException, ClientProtocolException, IOException {

		// En ny httpclient
		HttpClient client = new DefaultHttpClient();

		// The URL object
		String url = SERVER;

		// Make sure the URL is proper REST GET
		if (!url.endsWith("?"))
			url += "?";

		// The attributes list
		List<NameValuePair> params = new LinkedList<NameValuePair>();
		// Add the w attribute with the WOIED value
		params.add(new BasicNameValuePair("w", WOEID));
		String paramString = URLEncodedUtils.format(params, "utf-8");
		url += paramString;

		// Create the GET request
		HttpGet get_request = new HttpGet();

		// Point the Get request to the correct URL
		get_request.setURI(new URI(url));

		// Get the response
		HttpResponse response = client.execute(get_request);

		// Read the content of the response
		InputStream is = response.getEntity().getContent();
		InputStreamReader isreader = new InputStreamReader(is);
		BufferedReader reader = new BufferedReader(isreader);

		// Write to a string buffer
		StringBuffer sb = new StringBuffer();
		String line = "";
		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}

		// Return the response as string
		return sb.toString();
	}

	@Override
	protected Void doInBackground(String... params) {
		while (!isCancelled()) {
			try {
				String response = doGet(params[0]);
				createBroadcast(response);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				// Sleep for 5 minutes
				Thread.sleep(5 * 60 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	private void createBroadcast(String json) {
		JSONObject root;
		try {
			root = new JSONObject(json);
			JSONObject condition = root.getJSONObject("condition");
			int code = condition.getInt("code");

			Intent weatherbroadcast = new Intent();
			weatherbroadcast.setAction(WEATHER_EVENT);
			weatherbroadcast.putExtra("code", code);

			context.sendBroadcast(weatherbroadcast);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
