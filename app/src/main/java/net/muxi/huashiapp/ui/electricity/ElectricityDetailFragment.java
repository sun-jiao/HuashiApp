package net.muxi.huashiapp.ui.electricity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.muxistudio.appcommon.appbase.BaseAppFragment;
import com.muxistudio.appcommon.data.Electricity;

import net.muxi.huashiapp.R;

/**
 * Created by december on 16/7/6.
 */
public class ElectricityDetailFragment extends BaseAppFragment {
    private TextView mTvDegreeLeft;
    private TextView mTvDegreeLastMonth;
    private TextView mTvDegreeCurMonth;
    private TextView mTvMoneyLeft;
    private TextView mTvMoneyLastMonth;
    private TextView mTvMoneyCurMonth;


    private CardView mCardMoneyLeft;
    private CardView mCardDegreeLeft;
    private CardView mCardTotalUse;


    private int type;

    private static final int TYPE_LIGHT = 0;
    private static final int TYPE_AIR = 1;


    //推荐写法！！！
    public static ElectricityDetailFragment newInstance(int type) {
        Bundle args = new Bundle();
        args.putInt("type", type);
        ElectricityDetailFragment fragment = new ElectricityDetailFragment();
        //防止丢失数据 setArguments
        fragment.setArguments(args);
        return fragment;
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_electricity_detail, container, false);
        mTvDegreeLeft = view.findViewById(R.id.tv_degree_left);
        mTvDegreeLastMonth = view.findViewById(R.id.tv_degree_last_month);
        mTvDegreeCurMonth = view.findViewById(R.id.tv_degree_cur_month);
        mTvMoneyLeft = view.findViewById(R.id.tv_money_left);
        mTvMoneyLastMonth = view.findViewById(R.id.tv_money_last_month);
        mTvMoneyCurMonth = view.findViewById(R.id.tv_money_cur_month);

        mCardMoneyLeft = view.findViewById(R.id.card_money_left);
        mCardDegreeLeft = view.findViewById(R.id.card_degree_left);
        mCardTotalUse = view.findViewById(R.id.card_total_use);

        return view;
    }


    @Override
    public void onStart() {
        super.onStart();

        type = getArguments().getInt("type");

        if (type == TYPE_LIGHT) {
            mCardMoneyLeft.setCardBackgroundColor(getResources().getColor(R.color.color_card_light_one));
            mCardDegreeLeft.setCardBackgroundColor(getResources().getColor(R.color.color_card_light_one));
            mCardTotalUse.setCardBackgroundColor(getResources().getColor(R.color.color_card_light_two));
        } else if (type == TYPE_AIR) {
            mCardMoneyLeft.setCardBackgroundColor(getResources().getColor(R.color.color_card_air_one));
            mCardDegreeLeft.setCardBackgroundColor(getResources().getColor(R.color.color_card_air_one));
            mCardTotalUse.setCardBackgroundColor(getResources().getColor(R.color.color_card_air_two));
        }
    }

    public void setEleDetail(Electricity eleData) {
        int index = eleData.getDegree().getBefore().indexOf(".");
        int index2 = eleData.getDegree().getCurrent().indexOf(".");
        int index3 = eleData.getEle().getBefore().indexOf(".");
        int index4 = eleData.getEle().getCurrent().indexOf(".");
        mTvDegreeLeft.setText(eleData.getDegree().getRemain() + "");
        mTvDegreeLastMonth.setText(eleData.getEle().getBefore().substring(0, index3 + 2));
        mTvDegreeCurMonth.setText(eleData.getEle().getCurrent().substring(0, index4 + 2));
        mTvMoneyLeft.setText(eleData.getEle().getRemain());
        mTvMoneyLastMonth.setText(eleData.getDegree().getBefore().substring(0, index + 2));
        mTvMoneyCurMonth.setText(eleData.getDegree().getCurrent().substring(0, index2 + 2));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
