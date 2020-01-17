package com.xabber.android.ui.widget;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.xabber.android.R;
import com.xabber.android.data.Application;
import com.xabber.android.utils.Utils;

import static androidx.recyclerview.widget.ItemTouchHelper.ACTION_STATE_SWIPE;

public class ReplySwipeCallback extends ItemTouchHelper.Callback implements View.OnTouchListener {

    private SwipeAction swipeListener;
    private ReplyArrowState currentReplyArrowState = ReplyArrowState.GONE;

    private static final Drawable replyIcon = Application.getInstance().getResources().getDrawable(R.drawable.ic_message_forwarded_14dp);
    private static final int size = Utils.dipToPx(32f, Application.getInstance());
    private static final int paddingRight = Utils.dipToPx(12f, Application.getInstance());
    private static final float MAX_SWIPE_DISTANCE_RATIO = 0.18f;
    private static final float ACTIVE_SWIPE_DISTANCE_RATIO = 0.14f;

    private RecyclerView.ViewHolder currentItemViewHolder = null;
    private boolean touchListenerIsSet = false;
    private boolean touchListenerIsEnabled = false;
    private boolean swipeEnabled = true;
    private boolean swipeBack;


    private RecyclerView recyclerView;
    private float dX;
    private float dY;
    private int actionState;

    public interface SwipeAction {
        void onFullSwipe(int position);
    }

    public ReplySwipeCallback(SwipeAction listener) {
        this.swipeListener = listener;
        replyIcon.setColorFilter(Application.getInstance().getResources().getColor(R.color.grey_400), PorterDuff.Mode.SRC_IN);
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, swipeEnabled ? ItemTouchHelper.LEFT : 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return defaultValue/2;
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        if (swipeBack) {
            swipeBack = false;
            return 0;
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public void onChildDraw(Canvas c,
                            RecyclerView recyclerView,
                            RecyclerView.ViewHolder viewHolder,
                            float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        setTouchListener(recyclerView);

        if (dX < -Math.min(recyclerView.getWidth(), recyclerView.getHeight())*MAX_SWIPE_DISTANCE_RATIO){
            dX = -Math.min(recyclerView.getWidth(), recyclerView.getHeight())*MAX_SWIPE_DISTANCE_RATIO;
        }

        if (actionState == ACTION_STATE_SWIPE && isCurrentlyActive) {
            touchListenerIsEnabled = true;
        }
        updateActionState(actionState);
        updateTouchData(dX, dY);
        currentItemViewHolder = viewHolder;
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    public void setSwipeEnabled(boolean enabled) {
        swipeEnabled = enabled;
    }

    //TODO add proper (dis)appearance animations
    private void drawReplyArrow(Canvas c, RecyclerView.ViewHolder viewHolder) {
        if (currentReplyArrowState == ReplyArrowState.GONE) return;
        View itemView = viewHolder.itemView;

        int right = itemView.getRight() - paddingRight;
        int left = right - size;

        int height = itemView.getBottom() - itemView.getTop();
        int center = itemView.getTop() + height/2;

        int top = center - size/2;
        int bottom = center + size/2;

        replyIcon.setBounds(left, top, right, bottom);
        replyIcon.draw(c);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setTouchListener(RecyclerView recyclerView) {
        if (!touchListenerIsSet) {
            recyclerView.setOnTouchListener(this);
            this.recyclerView = recyclerView;
            touchListenerIsSet = true;
        }
    }

    private void updateActionState(int actionState) {
        this.actionState = actionState;
    }

    private void updateTouchData(float dX, float dY) {
        this.dX = dX;
        this.dY = dY;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        swipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;

        if (actionState == ACTION_STATE_SWIPE && touchListenerIsEnabled) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    touchListenerIsEnabled = false;
                    setItemsClickable(recyclerView, true);

                    if (swipeListener != null) {
                        if (currentReplyArrowState == ReplyArrowState.VISIBLE && currentItemViewHolder != null) {
                            swipeListener.onFullSwipe(currentItemViewHolder.getAdapterPosition());
                        }
                    }
                    currentReplyArrowState = ReplyArrowState.GONE;
                    currentItemViewHolder = null;
                    break;
                default:
                    //We set max swipe distance as 0.2f, but since we don't want the user to drag
                    //each message all the way to the end for a reply, the "active" reply state starts at 0.15f,
                    //and is accompanied by the haptic feedback and the appearance of the reply icon.
                    //animations soon?
                    if (dX < -Math.min(recyclerView.getWidth(), recyclerView.getHeight()) * ACTIVE_SWIPE_DISTANCE_RATIO) {
                        if (currentReplyArrowState != ReplyArrowState.VISIBLE) {
                            currentReplyArrowState = ReplyArrowState.VISIBLE;
                            Utils.performHapticFeedback(recyclerView, HapticFeedbackConstants.KEYBOARD_TAP);
                            setItemsClickable(recyclerView, false);
                        }
                    } else {
                        currentReplyArrowState = ReplyArrowState.GONE;
                    }
            }
        } else {
            currentReplyArrowState = ReplyArrowState.GONE;
            currentItemViewHolder = null;
        }
        return false;
    }

    private void setItemsClickable(RecyclerView recyclerView,
                                   boolean isClickable) {
        for (int i = 0; i < recyclerView.getChildCount(); ++i) {
            recyclerView.getChildAt(i).setClickable(isClickable);
        }
    }

    public void onDraw(Canvas c) {
        if (currentItemViewHolder != null && currentReplyArrowState == ReplyArrowState.VISIBLE) {
            drawReplyArrow(c, currentItemViewHolder);
        }
    }

    enum ReplyArrowState {
        GONE,
        VISIBLE
    }
}