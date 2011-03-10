package com.geekyouup.android.duckhunt;

import java.util.ArrayList;

import com.geekyouup.android.duckhunt.DuckHuntView.DuckThread;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class DuckHunt extends Activity {
    private static final int MENU_EXIT = 4;
    private static final int MENU_HIGHSCORCES = 5;
    private static final int MENU_START = 6;
    private static final int MENU_SOUND = 7;
    private MenuItem mSoundMenuItem;
    private boolean isSoundOn = true;
    private static final String PREFS_NAME = "DUCK_PREFS";
    private static final String KEY_HIGHSCORE = "HIGHSCORE_";
    private static final String PREFS_SOUND = "sound";
    private static final int DIALOG_HIGHSCORES = 0;
    private boolean doSave = true;
    private ArrayList<Integer> mHighScores = new ArrayList<Integer>();
    
    /** A handle to the thread that's actually running the animation. */
    private DuckThread mDuckThread;
    /** A handle to the View in which the game is running. */
    private DuckHuntView mDuckView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        menu.add(0, MENU_START, 0, R.string.menu_start);
        mSoundMenuItem= menu.add(0, MENU_SOUND, 1, R.string.menu_sound).setIcon(isSoundOn?android.R.drawable.button_onoff_indicator_on:android.R.drawable.button_onoff_indicator_off);
        menu.add(0, MENU_HIGHSCORCES, 2, R.string.menu_highscores);
        menu.add(0, MENU_EXIT, 3, R.string.menu_exit);
        return true;
    }

    /**
     * Invoked when the user selects an item from the Menu.
     * 
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_START:
                mDuckThread.doStart();
                return true;
            case MENU_SOUND:
        		isSoundOn = !isSoundOn;
        		if(mSoundMenuItem!=null) mSoundMenuItem.setIcon(isSoundOn?android.R.drawable.button_onoff_indicator_on:android.R.drawable.button_onoff_indicator_off);
        		mDuckThread.setSoundState(isSoundOn);
        		
        		Toast.makeText(this, "Sound " + (isSoundOn?"on":"off"), Toast.LENGTH_SHORT).show();
        		saveBoolean(PREFS_SOUND, isSoundOn);
                return true;
            case MENU_EXIT:
            	doSave=false;
                finish();
                return true;
            case MENU_HIGHSCORCES:
            	showDialog(DIALOG_HIGHSCORES);
                return true;
        }

        return false;
    }

    /**
     * Invoked when the Activity is created.
     * 
     * @param savedInstanceState a Bundle containing state saved from a previous
     *        execution, or null if this is a new execution
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void onResume() {
        super.onResume();

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.main);

        // get handles to the LunarView from XML, and its LunarThread
        mDuckView = (DuckHuntView) findViewById(R.id.duckview);
        mDuckThread = mDuckView.getThread();
        mDuckThread.setDuckHuntApp(this);
        // give the LunarView a handle to the TextView used for messages
        mDuckView.setTextView((TextView) findViewById(R.id.text));
        
        //Make sure the welcome message only appears on first launch
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if(settings !=null)
        {
	     	  isSoundOn = settings.getBoolean(PREFS_SOUND, true);
	     	  mDuckThread.setSoundState(isSoundOn);
	     	 
	     	  //read the high scores
	     	 mHighScores = new ArrayList<Integer>();
	     	  for(int i=0;i<10;i++)
	     	  {
	     		  int highScore = settings.getInt(KEY_HIGHSCORE+i, -1);
	     		  if(highScore != -1)mHighScores.add(highScore);
	     		  else break;
	     	  }
	     	  
	     	  int oldState = settings.getInt(DuckThread.KEY_MODE, DuckThread.STATE_READY);
	     	  if(oldState == DuckThread.STATE_PAUSE)
	     	  {
	     		  mDuckThread.restoreState(settings);
	     		  mDuckThread.setState(DuckThread.STATE_PAUSE);
	     	  }else
	     	  {
	     		  mDuckThread.setState(DuckThread.STATE_READY);
	     	  }
        }else
        {
 			mDuckThread.setState(DuckThread.STATE_READY);
        }
    }

	protected Dialog onCreateDialog(int id) {
		if(id == DIALOG_HIGHSCORES)
		{
			StringBuffer message = new StringBuffer();//"1. 432432\n2. 13122\n3. 90\n4. 60\n5. 20\n6. 19\n7. 17\n8. 15\n9. 10\n10. 5\n\n";
			if(mHighScores != null && mHighScores.size()>0)
			{
				for(int i=0;i<mHighScores.size();i++)
				{
					message.append(i+1).append(". ").append(mHighScores.get(i)).append("\n");
				}
			}else
			{
				message.append("No high scores!");
			}
			
			AlertDialog dialog = new AlertDialog.Builder(DuckHunt.this).create();//new AlertDialog(Bookmarker.this);
			dialog.setTitle("High Scores");
            dialog.setMessage(message.toString());
            dialog.setButton("OK", new DialogInterface.OnClickListener() {
	             public void onClick(DialogInterface dialog, int whichButton) {
	            	//nothing
	             }
	         });
            
            dialog.setCancelable(true);
            return dialog;
		}else return null;
	}
    
    
    /**
     * Invoked when the Activity loses user focus.
     */
    @Override
    protected void onPause() {
        super.onPause();
        
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(settings != null)
        {
	        SharedPreferences.Editor editor = settings.edit();
	        if(editor != null)
	        {
	        	//save the highscores
	        	for(int i=0;i<mHighScores.size();i++)
	        	{
	        		editor.putInt(KEY_HIGHSCORE+i, mHighScores.get(i));
	        	}
	        	
	        	if(doSave)
	        	{
	        		mDuckThread.saveState(editor);
	        	}else //if no KEY_MODE then doesn't read rest
	        	{
	        		editor.remove(DuckThread.KEY_MODE);
	        	}
		        editor.commit();
	        }
        }
        
        mDuckView.getThread().pause(); // pause game when Activity pauses
    }

    
    private void saveBoolean(String key, boolean value)
    {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        if(settings !=null)
        {
           SharedPreferences.Editor editor = settings.edit();
           editor.putBoolean(key, value);
           editor.commit();
        }
    }
    
	/*** Capture Back Button and use for browser back, else quit ****/
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			doSave=false;
			finish();
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
        	try{((AudioManager)getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);}catch(Exception e){}
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
		{
        	try{((AudioManager)getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);}catch(Exception e){}
			return true;
		}else //not back button or no history to go back to
		{
			return super.onKeyDown(keyCode, event);
		}
	}
	
	//returns true if added to high scores
	public int addHighScore(int highScore)
	{
		if(mHighScores == null) mHighScores = new ArrayList<Integer>();
		int addedPos = -1;
		//if we have less than 10 high scores then definitely add it in
		if(mHighScores.size()<10)
		{
			for(int i=0;i<mHighScores.size();i++)
			{
				if(mHighScores.get(i)<highScore)
				{
					mHighScores.add(i,highScore);
					addedPos=(i+1);
					break;
				}
			}
			if(addedPos==-1)
			{
				mHighScores.add(highScore);
				addedPos=mHighScores.size();
			}
		}else //see if we can shift them down a bit
		{
			for(int i=0;i<mHighScores.size();i++)
			{
				if(mHighScores.get(i)<highScore)
				{
					mHighScores.add(i,highScore);
					addedPos = (i+1);
					break;
				}
			}
			if(mHighScores.size()>10)
			{
				mHighScores.remove(mHighScores.size()-1);
			}
		}
		
		
		return addedPos;
	}
}