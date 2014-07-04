/*
 * Androzic - android navigation client that uses OziExplorer maps (ozf2, ozfx3).
 * Copyright (C) 2010-2012 Andrey Novikov <http://andreynovikov.info/>
 * 
 * This file is part of Androzic application.
 * 
 * Androzic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Androzic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Androzic. If not, see <http://www.gnu.org/licenses/>.
 */

package com.androzic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;
import com.androzic.data.Route;
import com.androzic.data.Track;
import com.androzic.overlay.CurrentTrackOverlay;
import com.androzic.overlay.RouteOverlay;
import com.androzic.util.AutoloadedRouteFilenameFilter;
import com.androzic.util.FileList;
import com.androzic.util.GpxFiles;
import com.androzic.util.KmlFiles;
import com.androzic.util.OziExplorerFiles;

public class Splash extends SherlockActivity implements OnClickListener
{
	private static final int MSG_FINISH = 1;
	private static final int MSG_ERROR = 2;
	private static final int MSG_STATUS = 3;
	private static final int MSG_PROGRESS = 4;
	private static final int MSG_ASK = 5;
	private static final int MSG_SAY = 6;

	private static final int RES_YES = 1;
	private static final int RES_NO = 2;

	private static final int DOWNLOAD_SIZE = 207216;
	private static final int PROGRESS_STEP = 10000;

	private int result;
	private boolean wait;
	protected String savedMessage;
	private ProgressBar progress;
	private TextView message;
	private Button gotit;
	private Button yes;
	private Button no;
	private Button quit;
	protected Androzic application;

	@SuppressLint("NewApi")
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		application = (Androzic) getApplication();

		PreferenceManager.setDefaultValues(this, R.xml.pref_behavior, true);
		PreferenceManager.setDefaultValues(this, R.xml.pref_folder, true);
		PreferenceManager.setDefaultValues(this, R.xml.pref_location, true);
		PreferenceManager.setDefaultValues(this, R.xml.pref_display, true);
		PreferenceManager.setDefaultValues(this, R.xml.pref_unit, true);
		PreferenceManager.setDefaultValues(this, R.xml.pref_tracking, true);
		PreferenceManager.setDefaultValues(this, R.xml.pref_waypoint, true);
		PreferenceManager.setDefaultValues(this, R.xml.pref_route, true);
		PreferenceManager.setDefaultValues(this, R.xml.pref_navigation, true);
		PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);

		setContentView(R.layout.act_splash);

		if (application.isPaid)
		{
			findViewById(R.id.paid).setVisibility(View.VISIBLE);
		}

		progress = (ProgressBar) findViewById(R.id.progress);
		message = (TextView) findViewById(R.id.message);

		message.setText(getString(R.string.msg_connectingtoserver));
		progress.setMax(DOWNLOAD_SIZE + PROGRESS_STEP * 4);

		yes = (Button) findViewById(R.id.yes);
		yes.setOnClickListener(this);
		no = (Button) findViewById(R.id.no);
		no.setOnClickListener(this);
		gotit = (Button) findViewById(R.id.gotit);
		gotit.setOnClickListener(this);
		quit = (Button) findViewById(R.id.quit);
		quit.setOnClickListener(this);

		wait = true;

		showEula();

		if (!application.mapsInited)
		{
			new InitializationThread(progressHandler).start();
		}
		else
		{
			progressHandler.sendEmptyMessage(MSG_FINISH);
		}
	}

	private void showEula()
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		boolean hasBeenShown = prefs.getBoolean(getString(R.string.app_eulaaccepted), false);
		if (hasBeenShown == false)
		{
			final SpannableString message = new SpannableString(Html.fromHtml(getString(R.string.app_eula).replace("/n", "<br/>")));
			Linkify.addLinks(message, Linkify.WEB_URLS);

			AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(getString(R.string.app_name)).setIcon(R.drawable.icon).setMessage(message)
					.setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							prefs.edit().putBoolean(getString(R.string.app_eulaaccepted), true).commit();
							wait = false;
							dialogInterface.dismiss();
						}
					}).setOnKeyListener(new OnKeyListener() {
						@Override
						public boolean onKey(DialogInterface dialoginterface, int keyCode, KeyEvent event)
						{
							return !(keyCode == KeyEvent.KEYCODE_HOME);
						}
					}).setCancelable(false);

			AlertDialog d = builder.create();

			d.show();
			// Make the textview clickable. Must be called after show()
			((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
		}
		else
		{
			wait = false;
		}
	}

	final Handler progressHandler = new Handler() {
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case MSG_STATUS:
					message.setText(msg.getData().getString("message"));
					break;
				case MSG_PROGRESS:
					int total = msg.getData().getInt("total");
					progress.setProgress(total);
					break;
				case MSG_ASK:
					progress.setVisibility(View.GONE);
					savedMessage = message.getText().toString();
					message.setText(msg.getData().getString("message"));
					result = 0;
					yes.setVisibility(View.VISIBLE);
					no.setVisibility(View.VISIBLE);
					break;
				case MSG_SAY:
					progress.setVisibility(View.GONE);
					savedMessage = message.getText().toString();
					message.setText(msg.getData().getString("message"));
					result = 0;
					gotit.setVisibility(View.VISIBLE);
					break;
				case MSG_FINISH:
					startActivity(new Intent(Splash.this, MapActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK).putExtras(getIntent()));
					finish();
					break;
				case MSG_ERROR:
					progress.setVisibility(View.INVISIBLE);
					message.setText(msg.getData().getString("message"));
					quit.setVisibility(View.VISIBLE);
					break;
			}
		}
	};

	private class InitializationThread extends Thread
	{
		Handler mHandler;
		int total;

		InitializationThread(Handler h)
		{
			mHandler = h;
		}

		public void run()
		{
			while (wait)
			{
				try
				{
					sleep(100);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}

			total = 0;

			Message msg = mHandler.obtainMessage(MSG_STATUS);
			Bundle b = new Bundle();
			b.putString("message", getString(R.string.msg_initializingdata));
			msg.setData(b);
			mHandler.sendMessage(msg);

			Resources resources = getResources();
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Splash.this);

			// start location service
			application.enableLocating(settings.getBoolean(getString(R.string.lc_locate), true));

			// set root folder and check if it has to be created
			String rootPath = settings.getString(getString(R.string.pref_folder_root), Environment.getExternalStorageDirectory() + File.separator + resources.getString(R.string.def_folder_prefix));
			File root = new File(rootPath);
			if (!root.exists())
			{
				try
				{
					root.mkdirs();
					File nomedia = new File(root, ".nomedia");
					nomedia.createNewFile();
				}
				catch (IOException e)
				{
					msg = mHandler.obtainMessage(MSG_ERROR);
					b = new Bundle();
					b.putString("message", getString(R.string.err_nosdcard));
					msg.setData(b);
					mHandler.sendMessage(msg);
					return;
				}
			}

			// check maps folder existence
			File mapdir = new File(settings.getString(getString(R.string.pref_folder_map), Environment.getExternalStorageDirectory() + File.separator + resources.getString(R.string.def_folder_map)));
			String oldmap = settings.getString(getString(R.string.pref_folder_map_old), null);
			if (oldmap != null)
			{
				File oldmapdir = new File(root, oldmap);
				if (!oldmapdir.equals(mapdir))
				{
					mapdir = oldmapdir;
					Editor editor = settings.edit();
					editor.putString(getString(R.string.pref_folder_map), mapdir.getAbsolutePath());
					editor.putString(getString(R.string.pref_folder_map_old), null);
					editor.commit();
				}
			}
			if (!mapdir.exists())
			{
				mapdir.mkdirs();
			}

			// check data folder existence
			File datadir = new File(
					settings.getString(getString(R.string.pref_folder_data), Environment.getExternalStorageDirectory() + File.separator + resources.getString(R.string.def_folder_data)));
			if (!datadir.exists())
			{
				// check if there was an old data structure
				String wptdir = settings.getString(getString(R.string.pref_folder_waypoint), null);
				System.err.println("wpt: " + wptdir);
				if (wptdir != null)
				{
					wait = true;
					msg = mHandler.obtainMessage(MSG_SAY);
					b = new Bundle();
					b.putString("message", getString(R.string.msg_newdatafolder, datadir.getAbsolutePath()));
					msg.setData(b);
					mHandler.sendMessage(msg);

					while (wait)
					{
						try
						{
							sleep(100);
						}
						catch (InterruptedException e)
						{
							e.printStackTrace();
						}
					}
				}
				datadir.mkdirs();
			}

			// check icons folder existence
			File iconsdir = new File(settings.getString(getString(R.string.pref_folder_icon),
					Environment.getExternalStorageDirectory() + File.separator + resources.getString(R.string.def_folder_icon)));
			if (!iconsdir.exists())
			{
				try
				{
					iconsdir.mkdirs();
					File nomedia = new File(iconsdir, ".nomedia");
					nomedia.createNewFile();
					application.copyAssets("icons", iconsdir);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}

			// initialize paths
			application.setRootPath(root.getAbsolutePath());
			application.setMapPath(mapdir.getAbsolutePath());
			application.setDataPath(Androzic.PATH_DATA, datadir.getAbsolutePath());
			application.setDataPath(Androzic.PATH_ICONS, iconsdir.getAbsolutePath());

			// initialize data
			application.installData();

			// start tracking service
			application.enableTracking(settings.getBoolean(getString(R.string.lc_track), true));

			// read default waypoints
			File wptFile = new File(application.dataPath, "myWaypoints.wpt");
			if (wptFile.exists() && wptFile.canRead())
			{
				try
				{
					application.addWaypoints(OziExplorerFiles.loadWaypointsFromFile(wptFile, application.charset));
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			// read track tail
			if (settings.getBoolean(getString(R.string.pref_showcurrenttrack), true))
			{
				application.currentTrackOverlay = new CurrentTrackOverlay(Splash.this);
				if (settings.getBoolean(getString(R.string.pref_tracking_currentload), resources.getBoolean(R.bool.def_tracking_currentload)))
				{
					int length = Integer.parseInt(settings.getString(getString(R.string.pref_tracking_currentlength), getString(R.string.def_tracking_currentlength)));
					// TODO Move this to proper class
					File path = new File(application.dataPath, "myTrack.db");
					try
					{
						SQLiteDatabase trackDB = SQLiteDatabase.openDatabase(path.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
						Cursor cursor = trackDB.rawQuery("SELECT * FROM track ORDER BY _id DESC LIMIT " + length, null);
						if (cursor.getCount() > 0)
						{
							Track track = new Track();
							for (boolean hasItem = cursor.moveToLast(); hasItem; hasItem = cursor.moveToPrevious())
							{
								double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
								double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
								double altitude = cursor.getDouble(cursor.getColumnIndex("elevation"));
								double speed = cursor.getDouble(cursor.getColumnIndex("speed"));
								double bearing = cursor.getDouble(cursor.getColumnIndex("track"));
								double accuracy = cursor.getDouble(cursor.getColumnIndex("accuracy"));
								int code = cursor.getInt(cursor.getColumnIndex("code"));
								long time = cursor.getLong(cursor.getColumnIndex("datetime"));
								track.addPoint(code == 0, latitude, longitude, altitude, speed, bearing, accuracy, time);
							}
							track.show = true;
							application.currentTrackOverlay.setTrack(track);
						}
						cursor.close();
						trackDB.close();
					}
					catch (Exception e)
					{
						Log.e("Splash", "Read track tail", e);
					}
				}
			}
			// load routes
			if (settings.getBoolean(getString(R.string.pref_route_preload), resources.getBoolean(R.bool.def_route_preload)))
			{
				boolean hide = settings.getBoolean(getString(R.string.pref_route_preload_hidden), resources.getBoolean(R.bool.def_route_preload_hidden));
				List<File> files = FileList.getFileListing(new File(application.dataPath), new AutoloadedRouteFilenameFilter());
				for (File file : files)
				{
					List<Route> routes = null;
					try
					{
						String lc = file.getName().toLowerCase();
						if (lc.endsWith(".rt2") || lc.endsWith(".rte"))
						{
							routes = OziExplorerFiles.loadRoutesFromFile(file, application.charset);
						}
						else if (lc.endsWith(".kml"))
						{
							routes = KmlFiles.loadRoutesFromFile(file);
						}
						else if (lc.endsWith(".gpx"))
						{
							routes = GpxFiles.loadRoutesFromFile(file);
						}
						application.addRoutes(routes);
						for (Route route : routes)
						{
							route.show = !hide;
							RouteOverlay newRoute = new RouteOverlay(Splash.this, route);
							application.routeOverlays.add(newRoute);
						}
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}

			total += PROGRESS_STEP;
			msg = mHandler.obtainMessage(MSG_PROGRESS);
			b = new Bundle();
			b.putInt("total", total);
			msg.setData(b);
			mHandler.sendMessage(msg);

			// load maps if no any found
			if (mapdir.list().length == 0)
			{
				wait = true;
				msg = mHandler.obtainMessage(MSG_ASK);
				b = new Bundle();
				b.putString("message", getString(R.string.nomaps_explained, application.getMapPath()));
				msg.setData(b);
				mHandler.sendMessage(msg);

				while (wait)
				{
					try
					{
						sleep(100);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}

				if (result == RES_YES)
				{
					try
					{
						// URL u = new URL("http://androzic.googlecode.com/files/world.ozfx3");
						URL u = new URL("https://docs.google.com/uc?export=download&id=0Bxnm5oGXU2cja2lQMzVvWFNpZjQ");
						HttpURLConnection c = (HttpURLConnection) u.openConnection();
						c.setRequestMethod("GET");
						c.setDoOutput(true);
						c.connect();
						FileOutputStream f = new FileOutputStream(new File(application.getMapPath(), "world.ozf2"));

						InputStream in = c.getInputStream();

						msg = mHandler.obtainMessage(MSG_STATUS);
						b = new Bundle();
						b.putString("message", getString(R.string.msg_loadingimagefile));
						msg.setData(b);
						mHandler.sendMessage(msg);

						byte[] buffer = new byte[1024];
						int len = 0;
						while ((len = in.read(buffer)) > 0)
						{
							f.write(buffer, 0, len);

							total += len;
							msg = mHandler.obtainMessage(MSG_PROGRESS);
							b = new Bundle();
							b.putInt("total", total);
							msg.setData(b);
							mHandler.sendMessage(msg);
						}
						f.close();

						msg = mHandler.obtainMessage(MSG_STATUS);
						b = new Bundle();
						b.putString("message", getString(R.string.msg_loadingmapfile));
						msg.setData(b);
						mHandler.sendMessage(msg);

						// u = new URL("http://androzic.googlecode.com/files/world.map");
						u = new URL("https://docs.google.com/uc?export=download&id=0Bxnm5oGXU2cjWllteG4tSDBxekU");
						c = (HttpURLConnection) u.openConnection();
						c.setRequestMethod("GET");
						c.setDoOutput(true);
						c.connect();
						f = new FileOutputStream(new File(application.getMapPath(), "world.map"));

						in = c.getInputStream();

						buffer = new byte[1024];
						len = 0;
						while ((len = in.read(buffer)) > 0)
						{
							f.write(buffer, 0, len);

							total += len;
							msg = mHandler.obtainMessage(MSG_PROGRESS);
							b = new Bundle();
							b.putInt("total", total);
							msg.setData(b);
							mHandler.sendMessage(msg);
						}
						f.close();
					}
					catch (IOException e)
					{
						e.printStackTrace();
						msg = mHandler.obtainMessage(MSG_ERROR);
						b = new Bundle();
						b.putString("message", getString(R.string.err_noconnection));
						msg.setData(b);
						mHandler.sendMessage(msg);
						File dir = new File(application.getMapPath());
						File[] files = dir.listFiles();
						if (files != null)
						{
							for (int i = 0; i < files.length; i++)
							{
								files[i].delete();
							}
						}
						return;
					}
				}
			}
			else
			{
				total += DOWNLOAD_SIZE;
				msg = mHandler.obtainMessage(MSG_PROGRESS);
				b = new Bundle();
				b.putInt("total", total);
				msg.setData(b);
				mHandler.sendMessage(msg);
			}

			msg = mHandler.obtainMessage(MSG_STATUS);
			b = new Bundle();
			b.putString("message", getString(R.string.msg_initializingmaps));
			msg.setData(b);
			mHandler.sendMessage(msg);

			// initialize maps
			application.initializeMaps();

			total += PROGRESS_STEP;
			msg = mHandler.obtainMessage(MSG_PROGRESS);
			b = new Bundle();
			b.putInt("total", total);
			msg.setData(b);
			mHandler.sendMessage(msg);

			msg = mHandler.obtainMessage(MSG_STATUS);
			b = new Bundle();
			b.putString("message", getString(R.string.msg_initializingplugins));
			msg.setData(b);
			mHandler.sendMessage(msg);

			// initialize plugins
			application.initializePlugins();

			total += PROGRESS_STEP;
			msg = mHandler.obtainMessage(MSG_PROGRESS);
			b = new Bundle();
			b.putInt("total", total);
			msg.setData(b);
			mHandler.sendMessage(msg);

			msg = mHandler.obtainMessage(MSG_STATUS);
			b = new Bundle();
			b.putString("message", getString(R.string.msg_initializingview));
			msg.setData(b);
			mHandler.sendMessage(msg);

			// initialize current map
			application.initializeMapCenter();

			total += PROGRESS_STEP;
			msg = mHandler.obtainMessage(MSG_PROGRESS);
			b = new Bundle();
			b.putInt("total", total);
			msg.setData(b);
			mHandler.sendMessage(msg);

			mHandler.sendEmptyMessage(MSG_FINISH);
		}
	}

	@Override
	public void onClick(View v)
	{
		switch (v.getId())
		{
			case R.id.yes:
				result = RES_YES;
				break;
			case R.id.no:
				result = RES_NO;
				break;
			case R.id.quit:
				finish();
				break;
		}
		gotit.setVisibility(View.GONE);
		yes.setVisibility(View.GONE);
		no.setVisibility(View.GONE);
		progress.setVisibility(View.VISIBLE);
		message.setText(savedMessage);
		wait = false;
	}
}
