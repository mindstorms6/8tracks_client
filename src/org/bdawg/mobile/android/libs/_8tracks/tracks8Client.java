package org.bdawg.mobile.android.libs._8tracks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.drm.DrmStore.Action;
import android.os.MemoryFile;
import android.util.Log;

public class tracks8Client {
	
	private static final String LOGTAG = "track8client";
	private JSONObject currUser = null;
	private String currToken = null;
	private String lastMixID = null;
	private String lastPlayToken = null;
	private String baseDownloadDir = null;
	private Boolean isLoggedIn=false;
	
	private enum Actions{
		MIXES,
		SESSIONS,
		USERS,
		SETS,
		SETS_NEW,
		NEXT,
		SKIP,
		MIXES_REVIEWS,
		USERS_REVIEWS,
		REVIEWS,
		REPORT,
		TAGS,
		MIXES_TOGGLE_LIKE,
		MIXES_LIKE,
		MIXES_UNLIKE,
		TRACKS_TOGGLE_FAV,
		TRACKS_FAV,
		TRACKS_UNFAV,
		USERS_TOGGLE_FOLLOW,
		USERS_FOLLOW,
		USERS_UNFOLLOW,
		USERS_MIXES,
		PLAY,
	}
	

	public tracks8Client(String cacheDir){
		this.baseDownloadDir = cacheDir;
		File f = new File(this.baseDownloadDir);
		if (!f.exists())
			f.mkdirs();
	}

	private enum HttpSendTypes{
		GET,
		POST
	}
	
	private final static String baseURL = "https://8tracks.com/";
	
	private HashMap<String,String> getDefaultQueryParams(){
		HashMap<String,String> tr = new HashMap<String,String>(){{
			put("format","json");
			put("api_key","eef81c8b7240e80c448fa1afed7f411ed0309f1e");
		}};
		if (currToken != null){
			tr.put("user_token", currToken);
		}
		return tr;
	}
	
	public boolean login(final String userName, final String password) throws ClientProtocolException, IOException{
		HashMap<String,String> postParms = new HashMap<String, String>(){{
			put("login",userName);
			put("password",password);
		}};
		
		HttpResponse loginResponse = simplePost(baseURL + Actions.SESSIONS.toString().toLowerCase(),getDefaultQueryParams(), postParms);
		Log.i(LOGTAG,loginResponse.getStatusLine().toString());
		if (loginResponse.getStatusLine().getStatusCode() != 200){
			return false;
		} else {
			//Parse response
			String result = EntityUtils.toString(loginResponse.getEntity());
			Log.i(LOGTAG,result);
			try {
				currUser = new JSONObject(result);
				currToken = currUser.getString("user_token");
				Log.i(LOGTAG,"Current user token is " + currToken);
				this.isLoggedIn=true;
				return true;
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				this.isLoggedIn=false;
				e.printStackTrace();
			}
			
		}
		this.isLoggedIn=false;
		return false;
	}
	
	public boolean isLoggedIn(){
		
		return this.isLoggedIn();
	}
	
	private HttpResponse simpleGet(String fullURL, Map<String,String> queryParms) throws ClientProtocolException, IOException{
		return genericWebExchange(HttpSendTypes.GET, fullURL, queryParms, null, null,null);
	}
	
	private HttpResponse simplePost(String fullURL, Map<String,String>queryParms, Map<String,String> valueToPost) throws ClientProtocolException, IOException{
		return genericWebExchange(HttpSendTypes.POST, fullURL, queryParms, valueToPost,null,null);
	}
	
	private HttpResponse genericWebExchange(HttpSendTypes type, String fullURL, Map<String,String> queryParms, Map<String,String> postParms, Map<String,String> headers, List<Cookie> cookies) throws ClientProtocolException, IOException{
		DefaultHttpClient cl = new DefaultHttpClient();
		
		if (cookies != null && cookies.size() > 0){
			CookieStore cs =new BasicCookieStore();
			for (Cookie c : cookies){
				cs.addCookie(c);
			}
			cl.setCookieStore(cs);
		}
		
		if (queryParms != null && queryParms.size() >0){
			
			if (fullURL.charAt(fullURL.length()-1) != '?')
				fullURL = fullURL + "?";
			for (Entry<String,String> kvp : queryParms.entrySet()){
				fullURL = String.format("%s%s=%s&",fullURL, kvp.getKey(), kvp.getValue());
			}
			fullURL = fullURL.substring(0, fullURL.length()-1);
		}
		Log.i(LOGTAG,"Full URL is " + fullURL);
		
		HttpUriRequest rq = null;
		if (type.equals(HttpSendTypes.GET)){
			HttpGet htGet = new HttpGet(fullURL);
			rq = htGet;
		} else if (type.equals(HttpSendTypes.POST)){
			HttpPost htPost = new HttpPost(fullURL);
			if (postParms != null && postParms.size() > 0){
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				for (Entry<String,String> ent : postParms.entrySet()){
					nvps.add(new BasicNameValuePair(ent.getKey(), ent.getValue()));
				}
				try {
					htPost.setEntity(new UrlEncodedFormEntity(nvps,HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			rq = htPost;
		}
		if (headers != null && headers.size() > 0){
			for (Entry<String,String> ent : headers.entrySet()){
				rq.addHeader(ent.getKey(), ent.getValue());
			}
		}
        return cl.execute(rq);

		
		
		
		
	}
	
	private String getNewPlayToken() throws Exception{
		String fUrl = baseURL + (Actions.SETS_NEW.toString().toLowerCase().replace("_", "/"));
		//Log.i(LOGTAG, "New token url is " + fUrl);
		HttpResponse resp = simpleGet(fUrl, getDefaultQueryParams());
		if (resp.getStatusLine().getStatusCode() != 200){
			throw new IOException("HTTP Status was not 200");
		}
		String result = EntityUtils.toString(resp.getEntity());
		try {
			JSONObject jResult = new JSONObject(result);
			String play_token = jResult.getString("play_token");
			Log.i(LOGTAG,"Playtoken is " + play_token);
			return play_token;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new Exception("Unknown error getting play token!");
	}
	
	public File playMix(final String mixID) throws Exception{
		try {
			this.lastMixID = mixID;
			final String pToken = getNewPlayToken();
			this.lastPlayToken = pToken;
			String fullURL = baseURL + (Actions.SETS.toString().toLowerCase()) + "/" + pToken + "/" + Actions.PLAY.toString().toLowerCase();
			HashMap<String,String> qParms = getDefaultQueryParams();
			qParms.put("mix_id", mixID);
			HttpResponse resp = simpleGet(fullURL, qParms);
			JSONObject obj = new JSONObject(EntityUtils.toString(resp.getEntity()));
			String musicURL = obj.getJSONObject("set").getJSONObject("track").getString("url");
			final String trackID = obj.getJSONObject("set").getJSONObject("track").getString("id");
			Thread thirtySecThread = new Thread(new Runnable(){
				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						Thread.sleep(30000);
						reportPlayedTrack(pToken, trackID, mixID);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ClientProtocolException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			});
			thirtySecThread.start();
			return getMusicFromURL(musicURL);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new Exception("Error fetching data!");
		}
	}
	
	public void reportPlayedTrack(String setID, String trackID, String mixID) throws ClientProtocolException, IOException{
		Map<String,String> qParms = getDefaultQueryParams();
		qParms.put("track_id", trackID);
		qParms.put("mix_id", mixID);
		String fullURL = baseURL + (Actions.SETS.toString().toLowerCase()) + "/" + setID + "/" + Actions.REPORT.toString().toLowerCase();
		simpleGet(fullURL, qParms);
	}
	
	public File getNext() throws ClientProtocolException, IOException, ParseException, JSONException{
		String fullURL = baseURL + (Actions.SETS.toString().toLowerCase()) + "/" + this.lastPlayToken + "/" + Actions.NEXT.toString().toLowerCase();
		Map<String,String> qParms = getDefaultQueryParams();
		qParms.put("mix_id", this.lastMixID);
		
		HttpResponse resp = simpleGet(fullURL, qParms);
		JSONObject obj = new JSONObject(EntityUtils.toString(resp.getEntity()));
		String musicURL = obj.getJSONObject("set").getJSONObject("track").getString("url");
		return getMusicFromURL(musicURL);
		
	}
	
	public File skip() throws ClientProtocolException, IOException, ParseException, JSONException{
		String fullURL = baseURL + (Actions.SETS.toString().toLowerCase()) + "/" + this.lastPlayToken + "/" + Actions.SKIP.toString().toLowerCase();
		Map<String,String> qParms = getDefaultQueryParams();
		qParms.put("mix_id", this.lastMixID);
		
		HttpResponse resp = simpleGet(fullURL, qParms);
		JSONObject obj = new JSONObject(EntityUtils.toString(resp.getEntity()));
		String musicURL = obj.getJSONObject("set").getJSONObject("track").getString("url");
		return getMusicFromURL(musicURL);
	}
	
	private File getMusicFromURL(String musicURL) throws ClientProtocolException, IOException{

		HttpResponse audioData = simpleGet(musicURL, null);
		BufferedInputStream bis = new BufferedInputStream(audioData.getEntity().getContent());
		
		File f = new File(this.baseDownloadDir,"fromRemote");
		if (f.exists() && f.isFile())
			f.delete();
		f.createNewFile();
		f.setReadable(true, false);
		FileOutputStream fl = new FileOutputStream(f);
		byte[] buffer = new byte[512];
		int readBytes = bis.read(buffer);
		int totalRead = 0;
		while (readBytes > 0){
			fl.write(buffer, 0, readBytes);
			//mf.writeBytes(buffer, 0, totalRead, readBytes);
			totalRead = totalRead+readBytes;
			readBytes = bis.read(buffer);
		}
		bis.close();
		Log.i(LOGTAG,"Fetch done!");
		return f;
	}
}
