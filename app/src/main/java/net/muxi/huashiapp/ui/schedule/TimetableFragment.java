package net.muxi.huashiapp.ui.schedule;

import static net.muxi.huashiapp.widget.IndicatedView.IndicatedView.DIRECTION_DOWN;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.muxi.huashiapp.App;
import net.muxi.huashiapp.R;
import net.muxi.huashiapp.RxBus;
import net.muxi.huashiapp.common.base.BaseActivity;
import net.muxi.huashiapp.common.base.BaseFragment;
import net.muxi.huashiapp.common.data.Course;
import net.muxi.huashiapp.common.db.HuaShiDao;
import net.muxi.huashiapp.common.net.CampusFactory;
import net.muxi.huashiapp.event.AddCourseEvent;
import net.muxi.huashiapp.event.DeleteCourseOkEvent;
import net.muxi.huashiapp.event.RefreshFinishEvent;
import net.muxi.huashiapp.util.Base64Util;
import net.muxi.huashiapp.util.DimensUtil;
import net.muxi.huashiapp.util.Logger;
import net.muxi.huashiapp.util.PreferenceUtil;
import net.muxi.huashiapp.util.TimeTableUtil;
import net.muxi.huashiapp.util.TipViewUtil;
import net.muxi.huashiapp.widget.IndicatedView.IndicatedView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by ybao on 17/1/25.
 */

public class TimetableFragment extends BaseFragment {

    @BindView(R.id.timetable)
    TimeTable mTimetable;
    @BindView(R.id.week_selected_view)
    WeekSelectedView mWeekSelectedView;
    @BindView(R.id.tv_select_week)
    TextView mTvSelectWeek;
    @BindView(R.id.iv_select_week)
    ImageView mIvSelectWeek;
    @BindView(R.id.tv_current_week)
    TextView mTvCurrentWeek;
    @BindView(R.id.iv_menu)
    ImageButton mIvMenu;
    @BindView(R.id.tool_layout)
    RelativeLayout mToolLayout;
    @BindView(R.id.table_menu_view)
    TableMenuView mTableMenuView;
    @BindView(R.id.shade_view)
    View mShadeView;

    /**
     * 本学期所有的课程
     */
    private List<Course> mCourses;

    //false表示初始状态
    private boolean selectedIvStatus = false;
    private int selectedWeek;
    private int curWeek;

    private HuaShiDao dao;

    private boolean handlingRefresh = false;

    public static TimetableFragment newInstance() {
        Bundle args = new Bundle();
        TimetableFragment fragment = new TimetableFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timetable, container, false);
        ButterKnife.bind(this, view);

        getActivity().getWindow().getDecorView().setBackgroundColor(Color.argb(255, 250, 250, 250));
        dao = new HuaShiDao();

        initData();
        initView();
        initListener();

        return view;
    }

    private void initData() {
        curWeek = TimeTableUtil.getCurWeek();
        selectedWeek = curWeek;
        mCourses = dao.loadAllCourses();
    }

    private void initView() {
        if (mCourses.size() == 0) {
            loadTable();
        } else {
            renderCourseView(mCourses);
        }

        Logger.d(TimeTableUtil.getCurWeek() + "");
        renderCurweekView(TimeTableUtil.getCurWeek());
        renderSelectedWeekView(TimeTableUtil.getCurWeek());
//        if (PreferenceUtil.getBoolean(PreferenceUtil.IS_FIRST_ENTER_TABLE)) {
        if (PreferenceUtil.getBoolean(PreferenceUtil.IS_FIRST_ENTER_TABLE, true)) {
            IndicatedView indicatedView = new IndicatedView(getContext());
            indicatedView.setTipViewText("设置当前周也可以点这里噢");
            TipViewUtil.addToContent(getContext(), indicatedView, DIRECTION_DOWN,
                    DimensUtil.getScreenWidth() - DimensUtil.dp2px(38), DimensUtil.dp2px(38));
        }

    }

    private void initListener() {

        mWeekSelectedView.setOnWeekSelectedListener(week -> {
            rotateSelectView();
            selectedWeek = week;
            renderCourseView(mCourses);
            renderSelectedWeekView(week);
        });
        mIvMenu.setOnClickListener(v -> {
            if (selectedIvStatus) {
                mWeekSelectedView.slideUp();
                rotateSelectView();
            }
            mTableMenuView.show();
            mTableMenuView.setCurweek(curWeek);
        });
        mTimetable.setOnCourseClickListener(v -> {
            String id = ((CourseView) v).getCourseId();
            DetailCoursesDialog dialog = DetailCoursesDialog.newInstance(getSameTimeCourses(id),
                    selectedWeek);
            dialog.show(getChildFragmentManager(), "detail_courses");
        });
        mShadeView.setOnClickListener(v -> {
            mTvSelectWeek.performClick();
            if (selectedIvStatus) {
                mWeekSelectedView.slideUp();
                mShadeView.setVisibility(View.GONE);
                rotateSelectView();
            }

        });

        Subscription subscription1 = RxBus.getDefault().toObservable(AddCourseEvent.class)
                .subscribe(addCourseEvent -> {
                    mCourses = dao.loadAllCourses();
                    renderCourseView(mCourses);
                });
        ((BaseActivity) getContext()).addSubscription(subscription1);
        Subscription subscription2 = RxBus.getDefault().toObservable(DeleteCourseOkEvent.class)
                .subscribe(deleteCourseOkEvent -> {
                    mCourses = dao.loadAllCourses();
                    renderCourseView(mCourses);
                });
        ((BaseActivity) getContext()).addSubscription(subscription2);
        mTimetable.setOnRefreshListener(() -> {
            handlingRefresh = true;
            loadTable();
        });
    }

    public List<Course> getSameTimeCourses(String id) {
        List<Course> courseList = new ArrayList<>();
        int start = 1;
        int duration = 2;
        String weekday = "星期一";
        for (Course course : mCourses) {
            if (course.id.equals(id)) {
                start = course.start;
                duration = course.during;
                weekday = course.day;
                break;
            }
        }
        for (Course course : mCourses) {
            if (course.start == start && course.during == duration && course.day.equals(weekday)) {
                courseList.add(course);
            }
        }
        return courseList;
    }

    public void loadTable() {
        CampusFactory.getRetrofitService().getSchedule(Base64Util.createBaseStr(App.sUser),
                App.sUser.sid)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(courseList -> {
                    courseList.remove(0);
                    updateDB(courseList);
                    mCourses = courseList;
                    renderCourseView(courseList);
                    if (handlingRefresh) {
                        handlingRefresh = false;
                        RxBus.getDefault().send(new RefreshFinishEvent(courseList.size() != 0));
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (handlingRefresh) {
                        handlingRefresh = false;
                        RxBus.getDefault().send(new RefreshFinishEvent(false));
                    }
                });
    }


    /**
     * 渲染显示的课程： 更改日期，显示课程
     */
    private void renderCourseView(List<Course> courseList) {
        mTimetable.getWeekLayout().setWeekDate(selectedWeek - curWeek);
        mTimetable.getTableContent().updateCourses(courseList, selectedWeek);
    }

    private void updateDB(List<Course> courseList) {
        dao.deleteAllCourse();
        for (Course course : courseList) {
            dao.insertCourse(course);
        }
    }

    private void rotateSelectView() {
        if (!selectedIvStatus) {
            mIvSelectWeek.setRotation(180);
        } else {
            mIvSelectWeek.setRotation(0);
        }
        selectedIvStatus = !selectedIvStatus;
    }

    /**
     * @param week 从1开始计数
     */
    public void renderSelectedWeekView(int week) {
        mTvSelectWeek.setText(String.format("第%d周", week));
    }

    /**
     * @param week 从1开始计数
     */
    public void renderCurweekView(int week) {
        mTvCurrentWeek.setText("当前周设置为" + week);
    }

    @Override
    public void onResume() {
        super.onResume();
        mCourses = dao.loadAllCourses();
        renderCourseView(mCourses);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (PreferenceUtil.getBoolean(PreferenceUtil.IS_FIRST_ENTER_TABLE, true)) {
            PreferenceUtil.saveBoolean(PreferenceUtil.IS_FIRST_ENTER_TABLE, false);
        }
    }

    @OnClick({R.id.tv_select_week, R.id.iv_select_week})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_select_week:
            case R.id.iv_select_week:
                if (!selectedIvStatus) {
                    mWeekSelectedView.slideDown();
                    mWeekSelectedView.setSelectedWeek(selectedWeek);
                    mShadeView.setVisibility(View.VISIBLE);
                } else {
                    mWeekSelectedView.slideUp();
                    mShadeView.setVisibility(View.GONE);
                }
                rotateSelectView();
                break;
        }
    }
}
