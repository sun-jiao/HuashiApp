package com.muxistudio.appcommon.utils;

import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * just for fun
 * author:kolibreath
 */
public class CommonTextUtils {

    public static void setText(TextView view){
        int random = (int) (System.currentTimeMillis()/10);
        switch (random){
            case 1:
                view.setText("———发现了一条彩蛋！点击我试试！———");
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(view.getContext())
                                .setTitle("一条来自木犀团队的消息")
                                .setMessage("Have a nice day!")
                                .setNegativeButton("取消", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .setPositiveButton("确定", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .create().show();
                    }
                });
                default:
                    view.setText("———别笑，我也是有底线的———");
        }
    }
}