package net.muxi.huashiapp.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import net.muxi.huashiapp.App;
import net.muxi.huashiapp.Constants;

import net.muxi.huashiapp.R;
import net.muxi.huashiapp.ui.card.CardActivity;
import net.muxi.huashiapp.common.data.CardData;
import net.muxi.huashiapp.common.data.Course;
import net.muxi.huashiapp.common.data.PersonalBook;
import net.muxi.huashiapp.common.data.Scores;
import net.muxi.huashiapp.common.data.User;
import net.muxi.huashiapp.common.db.HuaShiDao;
import net.muxi.huashiapp.common.net.CampusFactory;
import net.muxi.huashiapp.util.Base64Util;
import net.muxi.huashiapp.util.DateUtil;
import net.muxi.huashiapp.util.Logger;
import net.muxi.huashiapp.util.NotifyUtil;
import net.muxi.huashiapp.util.PreferenceUtil;
import net.muxi.huashiapp.util.TimeTableUtil;
import net.muxi.huashiapp.ui.score.ScoreActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ybao on 16/5/16.
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "alarm";

    private User mUser;
    private User mLibUser;
    private PreferenceUtil sp;
    private Context mContext;
    //设置学生卡提醒的额度
    private static final int CARD_LEAVE_MONEY = 20;
    private int mCurYear;
    private int mCurTerm;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mUser = new User();
        mLibUser = new User();
        sp = new PreferenceUtil();
        mUser.setSid(sp.getString(PreferenceUtil.STUDENT_ID));
        mUser.setPassword(sp.getString(PreferenceUtil.STUDENT_PWD));
        mLibUser.setSid(sp.getString(PreferenceUtil.LIBRARY_ID));
        mLibUser.setPassword(sp.getString(PreferenceUtil.LIBRARY_PWD));

        Logger.d(mUser.getSid());
        //判断对应的登录状态以及当前时间,还有用户是否设置提醒
        if (!mUser.getSid().equals("")) {
            Log.d(TAG, "check sid");
            if (intent.getIntExtra(Constants.ALARMTIME, 0) == 2) {
                if (sp.getBoolean(App.getContext().getString(R.string.pre_schedule), true)) {
//                    checkCourses();
                    Log.d(TAG, "check course");
                }
            }
            if (intent.getIntExtra(Constants.ALARMTIME, 0) == 1) {
                if (sp.getBoolean(App.getContext().getString(R.string.pre_score), true)) {
                    checkScores();
                    Log.d(TAG, "check score");
                }
            }
            if (intent.getIntExtra(Constants.ALARMTIME, 0) == 0) {
                if (sp.getBoolean(App.getContext().getString(R.string.pre_card), true)) {
                    checkCard();
                    Log.d(TAG, "check card");
                }
                if (sp.getBoolean(App.getContext().getString(R.string.pre_score), true)) {
                    checkScores();
                }
                if (mLibUser.getSid() != "") {
                    if (sp.getBoolean(App.getContext().getString(R.string.pre_library), true)) {
                        checkLib();
                    }
                }
            }
        }
    }

    private void checkCard() {
        CampusFactory.getRetrofitService().getCardBalance(mUser.getSid(), "60", "0", "10")
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<CardData>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(List<CardData> cardDatas) {
                        try {
                            if (Integer.valueOf(cardDatas.get(0).getOutMoney()) < CARD_LEAVE_MONEY) {
                                NotifyUtil.show(mContext, CardActivity.class,
                                        mContext.getResources().getString(R.string.notify_title_card),
                                        mContext.getResources().getString(R.string.notify_content_card));
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        } catch (Resources.NotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    private void checkLib() {
        CampusFactory.getRetrofitService().getPersonalBook(Base64Util.createBaseStr(mUser))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<List<PersonalBook>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(List<PersonalBook> personalBooks) {
                        List<String> books = new ArrayList<>();
                        boolean isRemind = false;
                        for (int i = 0, j = personalBooks.size(); i < j; i++) {
                            if (Integer.valueOf(personalBooks.get(i).time) < 4) {
                                isRemind = true;
                                books.add(personalBooks.get(i).book);
                            }
                        }
                        if (isRemind) {
                            String content = String.format(App.sContext.getString(R.string.notify_content_lib), connectBooks(books));
                        }
                    }
                });
    }

//    private void checkCourses() {
//        HuaShiDao dao = new HuaShiDao();
//        List<Course> courses = dao.loadCustomCourse();
//        Logger.d(courses.size() + "");
//        int day = DateUtil.getDayInWeek(new Date(System.currentTimeMillis()));
//        String defalutDate = DateUtil.getTheDateInYear(new Date(System.currentTimeMillis()), 1 - day);
//        String startDate = sp.getString(PreferenceUtil.FIRST_WEEK_DATE, defalutDate);
//        int curWeek = (int) DateUtil.getDistanceWeek(startDate, DateUtil.toDateInYear(new Date(System.currentTimeMillis()))) + 1;
//        Logger.d(curWeek + "");
//        int today = DateUtil.getDayInWeek(new Date(System.currentTimeMillis()));
//        //如果今天是周日则另做判断
//        if (today == 7) {
//            today = 1;
//            curWeek++;
//        } else {
//            today++;
//        }
//        List<String> courseName = new ArrayList<>();
//        for (int i = 0, j = courses.size(); i < j; i++) {
//            Course course = courses.get(i);
//            if (course.getRemind().equals("true") &&
//                    !course.getCourse().equals(Constants.INIT_COURSE) &&
//                    (Constants.WEEKDAYS[today - 1]).equals(course.getDay()) &&
//                    TimeTableUtil.isThisWeek(curWeek, course.getWeeks())) {
//                courseName.add(course.getCourse());
//            }
//        }
//        Logger.d("courseName size " + courseName.size());
//        if (courseName.size() > 0) {
//            String content = String.format(mContext.getString(R.string.notify_content_course), connectStrings(courseName));
//            NotifyUtil.show(mContext, ScheduleActivity.class,
//                    mContext.getString(R.string.notify_title_course),
//                    content);
//        }
//    }

    private void checkScores() {
        judgeYearAndTerm();
        CampusFactory.getRetrofitService().getScores(Base64Util.createBaseStr(mUser), mCurYear + "", mCurTerm + "")
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Scores>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(List<Scores> scoresList) {
                        Logger.d(scoresList.size() + "  " + sp.getInt(PreferenceUtil.SCORES_NUM));
                        if (scoresList.size() != sp.getInt(PreferenceUtil.SCORES_NUM) && scoresList.size() != 0) {
                            sp.saveInt(PreferenceUtil.SCORES_NUM, scoresList.size());
                            NotifyUtil.show(mContext, ScoreActivity.class,
                                    mContext.getString(R.string.notify_title_score),
                                    mContext.getString(R.string.notify_content_score));
                        }
                        if (scoresList.size() == 0) {
                            sp.saveInt(PreferenceUtil.SCORES_NUM, scoresList.size());
                        }
                    }
                });
    }

    private void judgeYearAndTerm() {
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        int date = calendar.get(Calendar.DAY_OF_MONTH);
        if (month < 9 && month > 3) {
            mCurTerm = 12;
        } else {
            mCurTerm = 3;
        }
        if (month < 9) {
            mCurYear = year - 1;
        } else {
            mCurYear = year;
        }
    }

    public String connectStrings(List<String> stringList) {
        String s;
        s = TextUtils.join(",", stringList);
        return s;
    }

    public String connectBooks(List<String> books) {
        String s = "";
        for (int i = 0; i < books.size(); i++) {
            s = s + "《" + books.get(i) + "》";
            if (i < books.size() - 1) {
                s += "，";
            }
        }
        return s;
    }

}
