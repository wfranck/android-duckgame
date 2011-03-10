package com.geekyouup.android.duckhunt;

import java.util.Random;

import com.geekyouup.android.duckhunt.DuckHuntView.DuckThread;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class ClayObject {

    private float mClayTopX=0;
    private float mClayTopY=0;
  
    private Drawable mClayImage;
    private Drawable mHitImage;
    private Random mRand = new Random();
    
    private int mClayWidth;
    private int mClayHeight;
    
    private float mDX = 0;
    private float mDY = 0;
    
    private int mState = 0;
    private int STATE_WAITING=0;
    private int STATE_ONSCREEN = 1;
    private int STATE_HIT = 2;
    private long mStateStartTime=0;
    private int mTimeBetweenTargets = 0;
    private int mCanvasHeight = 320;
    private int mTimeToStayCracked = 500;
    
    private boolean mFiresLeft = false;
    
    public ClayObject(Context context, int canvasHeight, boolean firesLeft)
    {
    	mFiresLeft = firesLeft;
        mClayImage = context.getResources().getDrawable(mFiresLeft?R.drawable.clayleft:R.drawable.clay);
        mHitImage = context.getResources().getDrawable(mFiresLeft?R.drawable.claycrackleft:R.drawable.claycrack);

        mClayWidth = mClayImage.getIntrinsicWidth();
        mClayHeight = mClayImage.getIntrinsicWidth();
        
        mCanvasHeight = canvasHeight;
        mTimeBetweenTargets=1000;
    }
    
    public void draw(Canvas canvas)
    {
    	if(mState == STATE_ONSCREEN)
    	{
        	mClayImage.setBounds((int) mClayTopX, (int) mClayTopY, (int) mClayTopX + mClayWidth, (int) mClayTopY + mClayHeight);
        	mClayImage.draw(canvas);
    	}else if(mState == STATE_HIT)
    	{
        	mHitImage.setBounds((int) mClayTopX, (int) mClayTopY, (int) mClayTopX + 35, (int) mClayTopY + 25);
        	mHitImage.draw(canvas);
    	}
    }
       
    public boolean isHit(int shotX, int shotY)
    {
		if(shotX>mClayTopX 
    			&& shotX<(mClayTopX+mClayWidth)
    			&& shotY>mClayTopY 
    			&& shotY<(mClayTopY+mClayHeight) )
		{
			return true;
		}
		return false;
    }
    
    public void moveIncrementally()
    {
    	mClayTopX += mDX;
   		mClayTopY-=mDY;
    }
    
    public void resetLevelAndLocation(int level)
    {
    	setState(STATE_ONSCREEN);
    	float xy  = (mRand.nextFloat()*(level+1))+level/2;
    	
    	mDX = mFiresLeft?-xy:xy;
    	mDY = 4.5f;
    	
    	mClayTopX = mFiresLeft?480:-mClayWidth;
    	mClayTopY = 280;
    	
    	mTimeBetweenTargets = 1000 - ( level*50);
    	if(mTimeBetweenTargets<300) mTimeBetweenTargets=300;
    }
    
    public int updatePhysics(DuckThread view, boolean isFiring, int mFireX, int mFireY, int level)
    {
    	int score = 0;
    	long now = System.currentTimeMillis();
    	if(mStateStartTime == 0) mStateStartTime = now+mRand.nextInt(1000);
    	
    	if(mState == STATE_WAITING  && now - mTimeBetweenTargets>=mStateStartTime)
    	{
    		resetLevelAndLocation(level);
    	}else if(mState == STATE_ONSCREEN)
    	{
    		if((mFiresLeft && mClayTopX<-mClayWidth) //moved off screen, complete object
    			|| (!mFiresLeft && mClayTopX>480) ) 
    		{
    			setState(STATE_WAITING);
    			view.objectComplete(false);
    		}else //still on screen check if hit and move
    		{
	    		if(isFiring)
	    		{
	        		if(isHit(mFireX,mFireY))
	        		{
	        			score=(10*(level+1)); //HIT!! give the user some points
	        			view.playSound(DuckThread.SOUND_CRACK);
	        			setState(STATE_HIT);
	         		}
	        	}
	    		
	    		mDY-=0.05;
	    		moveIncrementally();
    		}
    	}else if(mState == STATE_HIT)
    	{
    		if(now - mTimeToStayCracked < mStateStartTime ) //while duck hit, but still on screen then drop
    		{
    			moveIncrementally();
    		}else //once duck dropped off, complete the object
    		{
	    		view.objectComplete(true);
	    		setState(STATE_WAITING);
    		}
    	}
    	
    	return score;
    }
    
    private void setState(int state)
    {
		mState = state;
		mStateStartTime = System.currentTimeMillis();
    }
}