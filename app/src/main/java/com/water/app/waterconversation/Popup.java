package com.water.app.waterconversation;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;

public class Popup extends PopupWindow {
    private View view;
    private TextView choosePhoto;
    private TextView takePhoto;
    private TextView cancle;
    private TextView del_select;
    // layout : popupwindow顯示時方便設定背景
    // putPos ：根據傳入的資料長度，判斷顯示刪除和預覽按鈕
    public Popup(){}
    public  Popup(Activity context, View.OnClickListener btnOnClick, final View layout, int whichPhoto, int photoSize) {
        super(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.activity_pop, null);
        choosePhoto = view.findViewById(R.id.tv_pick_photo);
        takePhoto = view.findViewById(R.id.tv_take_photo);
        cancle = view.findViewById(R.id.tv_cancel);
        del_select.setOnClickListener(btnOnClick);
        choosePhoto.setOnClickListener(btnOnClick);
        takePhoto.setOnClickListener(btnOnClick);
        cancle.setOnClickListener(btnOnClick);
        this.setContentView(view);
        //設定SelectPicPopupWindow彈出窗體的寬
        this.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        //設定SelectPicPopupWindow彈出窗體的高
        this.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        //設定SelectPicPopupWindow彈出窗體可點選
        this.setFocusable(true);
        // 重新整理狀態
        this.update();
        this.setOutsideTouchable(true);
        this.setOnDismissListener(new OnDismissListener() {
            @Override public void onDismiss() {
                //消除RelativeLayout半透明
                layout.setAlpha(1); } });
        //例項化一個ColorDrawable顏色為半透明
        ColorDrawable dw = new ColorDrawable(0xb0ffffff);
        //設定SelectPicPopupWindow彈出窗體的背景
        this.setBackgroundDrawable(dw);
        this.setAnimationStyle(R.style.pop_anim);
    }
    /**
     *  顯示popupWindow
     *
     * @param parent */
    public void showPopupWindow(View parent) {
        if (!this.isShowing()) {
            this.showAtLocation(parent, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);
        }
    }
    /**
     * 關閉popupWindow */ public void dismissPopupWindow(View layout)
    { layout.setAlpha(1); this.dismiss(); }
}
