package com.xyq.gayweather.fragment;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.xyq.gayweather.R;
import com.xyq.gayweather.db.City;
import com.xyq.gayweather.db.County;
import com.xyq.gayweather.db.Province;
import com.xyq.gayweather.util.HttpUtil;
import com.xyq.gayweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * 城市选择
 */
public class ChooseAreaFragment extends Fragment {

    private ProgressDialog progressDialog;

    private TextView titleText;

    private ImageView ivBack;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    private List<Province> provinceList;

    private List<City> cityList;

    private List<County> countyList;

    private Province selectedProvince;

    private City selectedCity;

    private @QueryType
    int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.choose_area, container, false);

        titleText = view.findViewById(R.id.tv_title);
        ivBack = view.findViewById(R.id.iv_back);
        listView = view.findViewById(R.id.list_view);

        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == QueryType.TYPE_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == QueryType.TYPE_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == QueryType.TYPE_COUNTY) {
                    queryCities();
                } else if (currentLevel == QueryType.TYPE_CITY) {
                    queryProvinces();
                }
            }
        });

        queryProvinces();
    }

    /**
     * 查询省
     */
    private void queryProvinces() {
        titleText.setText("中国");
        ivBack.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            notifyAndSetLevel(QueryType.TYPE_PROVINCE);
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, QueryType.TYPE_PROVINCE);

        }
    }


    /**
     * 查询城市
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        ivBack.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city :
                    cityList) {
                dataList.add(city.getCityName());
            }
            notifyAndSetLevel(QueryType.TYPE_CITY);
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, QueryType.TYPE_CITY);
        }
    }


    /**
     * 查询区县
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        ivBack.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county :
                    countyList) {
                dataList.add(county.getCountyName());
            }
            notifyAndSetLevel(QueryType.TYPE_COUNTY);
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, QueryType.TYPE_COUNTY);
        }
    }


    private void notifyAndSetLevel(int level) {
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel = level;
    }

    private void queryFromServer(String address, final @QueryType int type) {
        showProgressDialog();
        HttpUtil.sendOkhttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if (QueryType.TYPE_PROVINCE == type) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if (QueryType.TYPE_CITY == type) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if (QueryType.TYPE_COUNTY == type) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }

                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if (QueryType.TYPE_PROVINCE == type) {
                                queryProvinces();
                            } else if (QueryType.TYPE_CITY == type) {
                                queryCities();
                            } else if (QueryType.TYPE_COUNTY == type) {
                                queryCounties();
                            }

                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * 注解定义省市区3种类型
     */
    @IntDef({QueryType.TYPE_PROVINCE, QueryType.TYPE_CITY, QueryType.TYPE_COUNTY})
    @interface QueryType {

        int TYPE_PROVINCE = 0;

        int TYPE_CITY = 1;

        int TYPE_COUNTY = 2;
    }
}
