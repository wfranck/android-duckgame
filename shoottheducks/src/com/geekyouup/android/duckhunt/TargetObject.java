package com.geekyouup.android.duckhunt;

import java.util.Random;

import com.geekyouup.android.duckhunt.DuckHuntView.DuckThread;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class TargetObject {

    private float mTargetTopX=0;
    private float mTargetTopY=0;
    private static final String KEY_TARGET_X = "target_x";
    private static final String KEY_TARGET_Y = "target_y";
    private static final String KEY_STATE = "target_state";
    private static final String KEY_STATE_START_TIME = "target_state_start_time";
    private static final String KEY_STATE_END_TIME = "target_state_end_time";
    
    private Drawable mTargetImage;
    private Drawable mTargetCrackedImage;
    private Random mRand = new Random();
    private int mTargetWidth;
    private int mTargetHeight;
    
    private int mCanvasWidth = 480;
    private int mCanvasHeight= 320;
    
    private long mStateStartTime;
    private int mTimeToStayOnScreen = 2000;
    private int mTimeBetweenTargets = 0;
    private int mState = 0;
    private int STATE_WAITING=0;
    private int STATE_ONSCREEN = 1;
    private int STATE_CRACKED = 2;
    private int STATE_CRACKED_DURATION = 250;
    
    public TargetObject(Context context, int canvasHeight, int canvasWidth)
    {
        mTargetImage = context.getResources().getDrawable(R.drawable.target);
        mTargetCrackedImage = context.getResources().getDrawable(R.drawable.targetcracked);
        mTargetWidth = mTargetImage.getIntrinsicWidth();
        mTargetHeight = mTargetImage.getIntrinsicWidth();
        mCanvasWidth=canvasWidth;
        mCanvasHeight=canvasHeight;
        mTimeBetweenTargets = 1000;
    }
    
    public void saveState(SharedPreferences.Editor map) 
    {
        if (map != null) {
            map.putFloat(KEY_TARGET_X,mTargetTopX);
            map.putFloat(KEY_TARGET_Y,mTargetTopY);
            map.putInt(KEY_STATE, mState);
            map.putLong(KEY_STATE_START_TIME, mStateStartTime);
            map.putInt(KEY_STATE_END_TIME, mTimeToStayOnScreen);
        }
    }
    
    public synchronized void restoreState(SharedPreferences map) {
    	if(map!=null)
    	{
            mTargetTopX = map.getFloat(KEY_TARGET_X,0);
            mTargetTopY = map.getFloat(KEY_TARGET_Y,0);
            
            mState = map.getInt(KEY_STATE, 0);
            mStateStartTime = map.getLong(KEY_STATE_START_TIME, 0);
            mTimeToStayOnScreen = map.getInt(KEY_STATE_END_TIME, 0);
    	}
    }

    public void draw(Canvas canvas)
    {
    	if(mState == STATE_CRACKED)
    	{
	    	mTargetCrackedImage.setBounds((int) mTargetTopX-3, (int) mTargetTopY-3, (int) mTargetTopX + mTargetWidth+3, (int) mTargetTopY + mTargetHeight+2);
	    	mTargetCrackedImage.draw(canvas);
    	}else if(mState == STATE_ONSCREEN)
    	{
	    	mTargetImage.setBounds((int) mTargetTopX, (int) mTargetTopY, (int) mTargetTopX + mTargetWidth, (int) mTargetTopY + mTargetHeight);
	    	mTargetImage.draw(canvas);
    	}
    }

    public boolean isHit(int shotX, int shotY)
    {
    	return(shotX>mTargetTopX 
    			&& shotX<(mTargetTopX+mTargetHeight)
    			&& shotY>mTargetTopY 
    			&& shotY<(mTargetTopY+mTargetHeight) );
    }

    public void resetLevelAndLocation(int level)
    {
		setState(STATE_ONSCREEN);
    	mTimeBetweenTargets = 1000 - ( level*75);
    	if(mTimeBetweenTargets<400) mTimeBetweenTargets=400;
    	
    	mTimeToStayOnScreen = 2000-75*(level);
    	if(mTimeToStayOnScreen < 1000) mTimeToStayOnScreen = 1000;
    	
    	mTargetTopX = mRand.nextInt(mCanvasWidth-mTargetWidth);
    	mTargetTopY = mRand.nextInt(mCanvasHeight-mTargetHeight);
    	
    	mTargetWidth = 35 + mRand.nextInt(35);
    	mTargetHeight = mTargetWidth;
    }

    public int updatePhysics(DuckThread view, boolean isFiring, int mFireX, int mFireY, int level)
    {
    	int returnScore = 0;
    	long now = System.currentTimeMillis();
    	if(mState == STATE_ONSCREEN)
    	{
    		//out of time
    		if((now - mStateStartTime) > mTimeToStayOnScreen)
    		{
    			setState(STATE_WAITING);
    			view.objectComplete(false);
    		}else if(isFiring) //or check if hit
    		{
        		if(isHit(mFireX,mFireY))
        		{
        			returnScore = 10*(level+1);
            		view.playSound(DuckThread.SOUND_CRACK);
            		setState(STATE_CRACKED);
        		}
    		}
    	}else if(mState == STATE_CRACKED && now-STATE_CRACKED_DURATION>=mStateStartTime)
    	{
    		view.objectComplete(true);
    		setState(STATE_WAITING);
    	}else if(mState == STATE_WAITING && now - mTimeBetweenTargets>=mStateStartTime)
    	{
    		resetLevelAndLocation(level);
    	}

    	return returnScore;
    }
    
    private void setState(int state)
    {
		mState = state;
		mStateStartTime = System.currentTimeMillis();
    }
}
