/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.geekyouup.android.duckhunt;

import java.util.HashMap;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

/**
 * View that draws, takes trackball, touch, etc.
 */
class DuckHuntView extends SurfaceView implements SurfaceHolder.Callback {
    class DuckThread extends Thread implements OnTouchListener {

        /*
         * State-tracking constants
         */
        public static final int STATE_LOSE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        public static final int STATE_WIN = 5;
        
        // Saving and restoring game state
        private static final String KEY_X = "mX";
        private static final String KEY_Y = "mY";
        private static final String KEY_LEVEL = "level";
        private static final String KEY_SCORE = "score";
        private static final String KEY_LIVES = "lives";
        private static final String KEY_OBJECTS_COMPLETE = "objects_done";
        public static final String KEY_MODE = "mode";
        private static final String KEY_LEVEL_TYPE = "level_type";
        
        /*
         * Member (state) fields
         */
        private int mLevel = 0;
        private int mLives = 5;
        private int mObjectsComplete = 0;

        /** The drawable to use as the background of the animation canvas */
        private Bitmap mBackgroundImage;
        private Bitmap mForegroundImage;
        public int mCanvasHeight = 320;
        public int mCanvasWidth = 480;
        private Drawable mFireImage;
        private Drawable mLifeImage;
        private int mFireHeight;
        private int mFireWidth;
        private int mLifeWidth;

        private DuckObject mDuck;
        private TargetObject mTarget;
        private PlaneObject mPlane;
        private ClayObject mSkeetLeft;
        private ClayObject mSkeetRight;
        
        private int mScore = 0;
        
        /** Message handler used by thread to interact with TextView */
        private Handler mHandler;

        private Drawable mCrossHairImage;
        private int mCrossHairHeight;
        private int mCrossHairWidth;

        /** Paint to draw the lines and text on screen. */
        private Paint mTextPaint;
        
        /** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
        private int mMode;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder mSurfaceHolder;

        /** X/Y of crosshair. */
        private int mCrossHairX;
        private int mCrossHairY;
        private boolean mFiring =false;
        private boolean mDrawFire = false;
        
        private SoundPool soundPool; 
        public static final int SOUND_QUACK = 0;
        public static final int SOUND_FIRE = 1;
        public static final int SOUND_CRACK = 2;
        
        private HashMap<Integer, Integer> soundPoolMap; 
        private boolean isSoundOn = true;
        private DuckHunt mDuckHunt;
        private AudioManager mAudioMgr;
        
        private static final int LEVEL_DUCKS = 0;
        private static final int LEVEL_TARGETS = 1;
        private static final int LEVEL_SKEET = 2;
        private int mLevelType = 0;
        private long mLostTime=0;
        private String[] mLevelTitles = new String[]{"Shoot the Ducks!","Target Practise","Skeet Shoot"};
        private Rect mFullScreenRect;
        private Rect mForeScreenRect;
        
        public DuckThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;

            Resources res = context.getResources();
            // cache handles to our key sprites & other drawables
            mCrossHairImage = context.getResources().getDrawable(R.drawable.crosshair);
            mFireImage = context.getResources().getDrawable(R.drawable.fire);
            mLifeImage  = context.getResources().getDrawable(R.drawable.life);
            // load background image as a Bitmap instead of a Drawable b/c
            // we don't need to transform it and it's faster to draw this way
            mBackgroundImage = BitmapFactory.decodeResource(res,R.drawable.background2);
            mForegroundImage = BitmapFactory.decodeResource(res,R.drawable.foreground2);

            mCanvasHeight = getHeight()<=0?320:getHeight();
            mCanvasWidth = getWidth()<=0?480:getWidth();
            
            mFullScreenRect = new Rect(0,0,mCanvasWidth,mCanvasHeight);
            mForeScreenRect = new Rect(0,mCanvasHeight-88,mCanvasWidth,mCanvasHeight);
            mCrossHairWidth = mCrossHairImage.getIntrinsicWidth();
            mCrossHairHeight = mCrossHairImage.getIntrinsicHeight();
            mFireWidth = mFireImage.getIntrinsicWidth();
            mFireHeight = mFireImage.getIntrinsicHeight();
            mLifeWidth = mLifeImage.getIntrinsicWidth();
            
            // Initialize paints for text
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(20);
            mTextPaint.setARGB(255,255, 0, 0);
            
            mAudioMgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 100);
            soundPoolMap = new HashMap<Integer, Integer>();
            soundPoolMap.put(SOUND_QUACK, soundPool.load(context, R.raw.quack, 0));
            soundPoolMap.put(SOUND_FIRE, soundPool.load(context, R.raw.fire, 1));
            soundPoolMap.put(SOUND_CRACK, soundPool.load(context, R.raw.crack, 2));
            
            mDuck = new DuckObject(context,mCanvasHeight, mCanvasWidth);
            mPlane = new PlaneObject(context,mCanvasWidth);
            
            resetVars();
        }

        private void resetVars()
        {
            mCrossHairX = 240;
            mCrossHairY = 160;
            mFiring = false;
        }
        
        /**
         * Starts the game, setting parameters for the current difficulty.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
                // First set the game for Medium difficulty
                mFiring = false;
                mScore=0;
                mLevel=0;
                mLevelType = 0;
                mLives=5;
                mBackgroundImage = BitmapFactory.decodeResource(getContext().getResources(),R.drawable.background2);
           		mForegroundImage = BitmapFactory.decodeResource(getContext().getResources(),R.drawable.foreground2);

                // pick a convenient initial location for the bike sprite
                mCrossHairX = 225;
                mCrossHairY = 145;
            	mObjectsComplete=0;

                setState(STATE_RUNNING);
            }
        }
        
        public void nextLevel()
        {
        	mLevel++;
        	mLevelType = mLevel%3;
        	setState(STATE_PAUSE,"Level Complete!\n\nLevel "+(mLevel+1)+": " +mLevelTitles[mLevelType]+ "\nTouch to Start");
        	mObjectsComplete=0;
        	
        	loadLevel();
        }
        
        private void loadLevel()
        {
        	Context context = getContext();
        	if(mLevelType == LEVEL_TARGETS) //level 2
        	{
        		mBackgroundImage = BitmapFactory.decodeResource(context.getResources(),R.drawable.targetbackgnd);
        		mForegroundImage = null;
        		
        		if(mTarget == null) mTarget = new TargetObject(context,mCanvasHeight, mCanvasWidth);
        	}else if(mLevelType == LEVEL_DUCKS)
        	{
           		mBackgroundImage = BitmapFactory.decodeResource(context.getResources(),R.drawable.background2);
           		mForegroundImage = BitmapFactory.decodeResource(context.getResources(),R.drawable.foreground2);
        	
                if(mDuck == null) mDuck = new DuckObject(getContext(),mCanvasHeight, mCanvasWidth);
                if(mTarget == null) mTarget = new TargetObject(getContext(),mCanvasHeight, mCanvasWidth);
        	}else if(mLevelType == LEVEL_SKEET)
        	{
        		mBackgroundImage = BitmapFactory.decodeResource(context.getResources(),R.drawable.background2);
        		mForegroundImage = null;

                if(mSkeetLeft == null) mSkeetLeft = new ClayObject(context, mCanvasHeight, true);
                if(mSkeetRight==null) mSkeetRight = new ClayObject(context, mCanvasHeight, false);
        	}
        }
        
        public void setDuckHuntApp(DuckHunt dh)
        {
        	this.mDuckHunt = dh;
        }
        
        public void setSoundState(boolean soundState)
        {
        	this.isSoundOn = soundState;
        }
        
        public void playSound(int sound) 
        {
            if(isSoundOn)
            {
	            float streamVolume = mAudioMgr.getStreamVolume(AudioManager.STREAM_MUSIC);
	            soundPool.play(soundPoolMap.get(sound), streamVolume, streamVolume, 1, 0, 1f);
            }
        } 

        /**
         * Pauses the physics update & animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
            }
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) updatePhysics();
                        doDraw(c);
                    }
                } finally {
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         * 
         * @return Bundle with this view's state
         */      
        public void saveState(SharedPreferences.Editor map) {
            synchronized (mSurfaceHolder) {
                if (map != null) {
                    map.putInt(KEY_X, mCrossHairX);
                    map.putInt(KEY_Y, mCrossHairY);
                    map.putInt(KEY_LEVEL,mLevel);
                    map.putInt(KEY_SCORE,mScore);
                    map.putInt(KEY_LIVES,mLives);
                    map.putInt(KEY_OBJECTS_COMPLETE,mObjectsComplete);
                    map.putInt(KEY_MODE, mMode==STATE_RUNNING?STATE_PAUSE:STATE_READY);
                    map.putInt(KEY_LEVEL_TYPE, mLevelType);
                    
                    if(mTarget != null) mTarget.saveState(map);
                    if(mDuck != null) mDuck.saveState(map);
                }
            }
        }
        
        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         * 
         * @param savedState Bundle containing the game state
         */       
        public synchronized void restoreState(SharedPreferences map) {
            synchronized (mSurfaceHolder) {
            	if(map!=null)
            	{
                    mCrossHairX = map.getInt(KEY_X,0);
                    mCrossHairY = map.getInt(KEY_Y,0);
                    mLevel = map.getInt(KEY_LEVEL,0);
                    mScore = map.getInt(KEY_SCORE,0);
                    mLives = map.getInt(KEY_LIVES,5);
                    mObjectsComplete = map.getInt(KEY_OBJECTS_COMPLETE,0);
                    mLevelType = map.getInt(KEY_LEVEL_TYPE,0);
                    
                    if(mDuck == null) mDuck = new DuckObject(getContext(),mCanvasHeight, mCanvasWidth);
                    mDuck.restoreState(map);
                    
                    if(mTarget == null) mTarget = new TargetObject(getContext(),mCanvasHeight, mCanvasWidth);
                    mTarget.restoreState(map);
                    
                    loadLevel();
            	}
            }
        }

        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         * 
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            mRun = b;
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         * 
         * @see #setState(int, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                setState(mode, null);
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         * 
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        public void setState(int mode, CharSequence message) {
            synchronized (mSurfaceHolder) {
                mMode = mode;

                if (mMode == STATE_RUNNING) {
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", "");
                    b.putInt("viz", View.INVISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                } else {
                	resetVars();
                    
                    Resources res = mContext.getResources();
                    CharSequence str = "";
                    if (mMode == STATE_READY)
                        str = res.getText(R.string.mode_ready);
                    else if (mMode == STATE_PAUSE)
                        str = res.getText(R.string.mode_pause);
                    else if (mMode == STATE_LOSE || mMode == STATE_WIN)
                    {
                    	mLostTime = System.currentTimeMillis();
                    	if(mMode ==STATE_LOSE)
                    	{
	                        str = res.getText(R.string.mode_lose);
                    	}else
                    	{
                            str = res.getString(R.string.mode_win_prefix)+ " "
                            + res.getString(R.string.mode_win_suffix);
                    	}
                    	
                        if(mDuckHunt!=null)
                        {
                        	String levelText = "\nRank: Hot Shot!";
                        	if(mScore<300) levelText = "\nRank: Out for a Duck";
                        	else if(mScore <500) levelText = "\nRank: New kid on the Duck";
                        	else if(mScore < 1000) levelText = "\nRank: Hot Shot!";
                        	else if(mScore < 1500) levelText = "\nRank: Well Mall'ard";
                        	else if(mScore < 2000) levelText = "\nRank: Top Gun";
                        	else levelText = "\nRank: Master Shooter!";
                        	
                        	
                        	int newHighScorePos = mDuckHunt.addHighScore(mScore);
                        	String end="th";
                        	if(newHighScorePos ==1) end="st";
                        	else if(newHighScorePos==2) end ="nd";
                        	else if (newHighScorePos==3) end="rd";
 
                        	if(newHighScorePos >0 ) str= str+ levelText+("\n\n"+mScore+" points puts you in "+newHighScorePos+end);
                        }
                    }

                    if (message != null) {
                        str = message;
                    }

                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", str.toString());
                    b.putInt("viz", View.VISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }
        }

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;

                // don't forget to resize the background image
                //mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, width, height, true);
                mFullScreenRect = new Rect(0,0,getWidth(),getHeight());
                mForeScreenRect = new Rect(0,getHeight()-88,getWidth(),getHeight());
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
                //mLastTime = System.currentTimeMillis();
            }
            if(mRun == false) setRunning(true);
            setState(STATE_RUNNING);
        }

        //callback to say object has been hit or moved offscreen
        public void objectComplete(boolean objectHit)
        {
        	if(!objectHit) mLives--;
        	mObjectsComplete++;
        }
        
        /**
         * Draws the layers
         */
        private void doDraw(Canvas canvas) {
            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
            canvas.drawBitmap(mBackgroundImage, null, mFullScreenRect, null);
            canvas.drawText( "Level: "+(mLevel+1)+"  Score: "+mScore,5, 20, mTextPaint);

            //draw level elements
            if(mLevelType == LEVEL_DUCKS)
            {
            	mDuck.draw(canvas);
            	mPlane.draw(canvas);
            }else if(mLevelType == LEVEL_TARGETS)
            {
            	mTarget.draw(canvas);
            }else if(mLevelType == LEVEL_SKEET)
            {
            	mSkeetLeft.draw(canvas);
            	mSkeetRight.draw(canvas);
            }

            //draw lives
            for(int i=1;i<mLives+1;i++)
            {
            	int xStart = mCanvasWidth - (mLifeWidth+5)*i;
            	mLifeImage.setBounds(xStart, 5,xStart+mLifeWidth,23);
            	mLifeImage.draw(canvas);
            }

        	if(mForegroundImage != null) canvas.drawBitmap(mForegroundImage,null,mForeScreenRect,null);

        	if(mMode == STATE_RUNNING)
        	{
	        	mCrossHairImage.setBounds(mCrossHairX, mCrossHairY, mCrossHairX+mCrossHairWidth, mCrossHairY+mCrossHairHeight);
	            mCrossHairImage.draw(canvas);
        	}
            
            if(mDrawFire)
            {              
                mFireImage.setBounds(mCrossHairX+1, mCrossHairY+5, mCrossHairX+1+mFireWidth, mCrossHairY+5+mFireHeight);
                mFireImage.draw(canvas);
                mDrawFire=false;
            }
        }
        
        /**
         * Figures the bike state (x, y, ...) based on the passage of
         * realtime. Does not invalidate(). Called at the start of draw().
         * Detects the end-of-game and sets the UI to the next state.
         */
        private void updatePhysics() {
        	//try {sleep(20);} catch (InterruptedException e) {}
        	
            //duck appears, then either flies off top, or is shot and falls off bottom
            if(mLevelType == LEVEL_DUCKS)
            {
            	mScore += mDuck.updatePhysics(this, mFiring, mCrossHairX+mCrossHairWidth/2, mCrossHairY+mCrossHairHeight/2, mLevel);
            	mScore += mPlane.updatePhysics(mFiring, mCrossHairX+mCrossHairWidth/2,mCrossHairY+mCrossHairHeight/2);
            }else if(mLevelType == LEVEL_TARGETS)
            {
            	mScore += mTarget.updatePhysics(this, mFiring, mCrossHairX+mCrossHairWidth/2, mCrossHairY+mCrossHairHeight/2, mLevel);
            }else if(mLevelType == LEVEL_SKEET)
            {
            	mScore += mSkeetLeft.updatePhysics(this, mFiring, mCrossHairX+mCrossHairWidth/2, mCrossHairY+mCrossHairHeight/2, mLevel);
            	mScore += mSkeetRight.updatePhysics(this, mFiring, mCrossHairX+mCrossHairWidth/2, mCrossHairY+mCrossHairHeight/2, mLevel);
            }
            if(mFiring) mFiring=false;
            
            //Detect Level Complete Scenario
            if(mLives==0) setState(STATE_LOSE);
            else if(mObjectsComplete>=8) nextLevel();
        }
        
        //FIRE
		public boolean onTouch(View v, MotionEvent event) {
			if (mMode == STATE_RUNNING) {
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					if(event.getX() > 90 || event.getY()<mCanvasHeight-90)
					{
						mCrossHairX = (int) event.getX()-17;
						mCrossHairY = (int) event.getY()-17;
						doFire();
					}else
					{
						doFire();
					}
				}
			}else if(mMode ==STATE_PAUSE)
			{
				unpause();
			}
			else
			{
				//give 3 seconds without being able to restart
				if(mLostTime==0 || (System.currentTimeMillis()-3000>mLostTime)) doStart();
			}
			return true;
		}
		
		public boolean doTrackBall(MotionEvent event)
		{
			if (mMode == STATE_RUNNING)
			{
				if(event.getAction()==MotionEvent.ACTION_DOWN)
				{
					//allow fire using trackball
					doFire();
				}else
				{
					float xPos = event.getX();
					float yPos = event.getY();
					
					mCrossHairX += (int) (xPos*100.0);
					mCrossHairY += (int) (yPos*100.0);
					
					if(mCrossHairX<-mCrossHairWidth) mCrossHairX=-mCrossHairWidth; if(mCrossHairX>mCanvasWidth) mCrossHairX=mCanvasWidth;
					if(mCrossHairY<-mCrossHairHeight) mCrossHairY=-mCrossHairHeight; if(mCrossHairY>mCanvasHeight) mCrossHairY=mCanvasHeight;
				}
			}
			
			return true;
		}
		
		public boolean doKeypress(int keyCode)
		{
			int x = 0;
			int y = 0;
			if(keyCode ==KeyEvent.KEYCODE_DPAD_UP) x=1;
			else if(keyCode ==KeyEvent.KEYCODE_DPAD_DOWN) x=-1;
			else if(keyCode ==KeyEvent.KEYCODE_DPAD_LEFT) y=-1;
			else if(keyCode ==KeyEvent.KEYCODE_DPAD_RIGHT) y=1;
			
			if(x!=0 || y!=0)
			{
				mCrossHairX += (int) (x*10.0);
				mCrossHairY += (int) (y*10.0);
				
				if(mCrossHairX<-mCrossHairWidth) mCrossHairX=-mCrossHairWidth; if(mCrossHairX>mCanvasWidth) mCrossHairX=mCanvasWidth;
				if(mCrossHairY<-mCrossHairHeight) mCrossHairY=-mCrossHairHeight; if(mCrossHairY>mCanvasHeight) mCrossHairY=mCanvasHeight;
				return true;
			}else if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
			{
				doFire();
				return true;
			}else
			{
				return false;
			}
		}
		
		private void doFire()
		{
			mFiring=true;
			mDrawFire=true;
			if(mScore>0) mScore--;
			
			playSound(SOUND_FIRE);
		}
		
		public void destroy()
		{
			super.destroy();
			if(soundPool != null) soundPool.release();
		} 
    }

    /** Handle to the application context, used to e.g. fetch Drawables. */
    private Context mContext;
    private TextView mStatusText;
    private DuckThread thread;

    public DuckHuntView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new DuckThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(m.getData().getString("text"));
            }
        });

        setOnTouchListener(thread);
        setFocusableInTouchMode(true);
        setFocusable(true); // make sure we get key events
    }

    /**
     * Fetches the animation thread corresponding to this LunarView.
     * 
     * @return the animation thread
     */
    public DuckThread getThread() {
        return thread;
    }

    @Override 
    public boolean onTrackballEvent(MotionEvent event) {
    	return thread.doTrackBall(event);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	boolean consumed = thread.doKeypress(keyCode);
    	if(!consumed) return super.onKeyDown(keyCode, event);
    	else return consumed;
    }
    
    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }

    /**
     * Installs a pointer to the text view used for messages.
     */
    public void setTextView(TextView textView) {
        mStatusText = textView;
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
}
