package net.fercanet.LNM;

import android.content.*;


public class Preferences {
	
	public static final String PREFS_NAME = "LMN_preferences";
	public long scoresnum;   // scores showed in hall of fame
	public int scoresnumpos;  // scores num position in spinner 
	public String notationstyle;   // Notation style (classic / letters)
	public boolean informer;  // informer in training mode 
	private Context ctx;
	
	// Constructor
	public Preferences (Context context){
		ctx = context;
		ctx = ctx.getApplicationContext();
		LoadPreferences();
	}
	
	public void LoadPreferences() {
	    SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, 0);
	    scoresnum = settings.getLong("hofentries", 1);                     // Getting number of top scores to store	
	    scoresnumpos = settings.getInt("hofentriespos", 1);                     // Getting number of top scores to store	
	    notationstyle = settings.getString("notationstyle", "seeuropean");    // Getting notation style
	    informer = settings.getBoolean("informer", true);
	}
	
	public void SavePreferences() {
		SharedPreferences settings = ctx.getSharedPreferences(PREFS_NAME, 0);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putLong("hofentries", scoresnum);
	    editor.putInt("hofentriespos", scoresnumpos);
	    editor.putString("notationstyle", notationstyle);
	    editor.putBoolean("informer", informer);
	    editor.commit();
	}
	
}
