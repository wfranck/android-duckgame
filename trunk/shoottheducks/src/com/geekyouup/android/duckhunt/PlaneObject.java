package com.geekyouup.android.duckhunt;

import java.util.Random;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

public class PlaneObject {

    private float mTargetTopX=480;
    private float mTargetTopY=0;
	
    private Drawable mPlaneImage;
    private Drawable mPlaneCrackedImage;
    private Random mRand = new Random();
    private int mTargetWidth;
    private int mTargetHeight;
    private long mHitTime;
    private int mTimeToStayCracked = 500;
    private int mCanvasWidth = 480;
    private float mSpeed = 1;
    private long mTimeAppear = 0;

    public PlaneObject(Context context, int canvasWidth)
    {
        mPlaneImage = context.getResources().getDrawable(R.drawable.plane);
        mPlaneCrackedImage = context.getResources().getDrawable(R.drawable.planecracked);
        mTargetWidth = mPlaneImage.getIntrinsicWidth();
        mTargetHeight = mPlaneImage.getIntrinsicWidth();
        mTargetTopX=canvasWidth;
        mCanvasWidth = canvasWidth;
    }
    
    public void draw(Canvas canvas)
    {
    	if(isInHitState())
    	{
	    	mPlaneCrackedImage.setBounds((int) mTargetTopX-3, (int) mTargetTopY-3, (int) mTargetTopX + 53, (int) mTargetTopY + 52);
	    	mPlaneCrackedImage.draw(canvas);
    	}else
    	{
	    	mPlaneImage.setBounds((int) mTargetTopX, (int) mTargetTopY, (int) mTargetTopX + mTargetWidth, (int) mTargetTopY + mTargetHeight);
	    	mPlaneImage.draw(canvas);
    	}
    }
    
    private boolean isInHitState()
    {
    	return (mHitTime > 0 && System.currentTimeMillis() -mTimeToStayCracked < mHitTime);
    }
    
    //returns the score change if there is one
    public int updatePhysics(boolean firing, int fireX, int fireY)
    {
    	if(!isInHitState() && mTimeAppear == 0)
    	{
    		mTimeAppear = System.currentTimeMillis() + mRand.nextInt(10000)+5000;
    		mSpeed = mRand.nextInt(5)+1.5f;
    		mTargetTopX = mCanvasWidth+mTargetWidth+20;
    		mTargetTopY = mRand.nextInt(100);
    		mHitTime=0;
    	}
    	else if(System.currentTimeMillis() > mTimeAppear && mTargetTopX>-mTargetWidth) // if appeared and onscreen
    	{
    		if(mHitTime ==0 && firing 
    				&& fireX>mTargetTopX && fireX<mTargetTopX+mTargetWidth
    				&& fireY>mTargetTopY && fireY<mTargetTopY+mTargetHeight)
    		{
    			mHitTime = System.currentTimeMillis();
    			mTimeAppear = 0;
    			return 50;
    		}	
    		mTargetTopX -= mSpeed;
    		if(mHitTime  != 0)
    		{
    			mTargetTopY+=mSpeed;
    		}
    	}
    	return 0;
    }
}
