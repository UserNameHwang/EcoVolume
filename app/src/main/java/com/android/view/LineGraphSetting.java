package com.android.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Color;
import android.util.Log;

import com.android.inputsound.SaveDCB;
import com.handstudio.android.hzgrapherlib.vo.GraphNameBox;
import com.handstudio.android.hzgrapherlib.vo.linegraph.LineGraph;
import com.handstudio.android.hzgrapherlib.vo.linegraph.LineGraphVO;

public class LineGraphSetting {

	public LineGraphVO makeLineGraphDefaultSetting() {

		String[] legendArr = { "1", "2", "3", "4", "5" };
		float[] graph1 = { 500, 100, 300, 200, 100 };
		float[] graph2 = { 000, 100, 200, 100, 200 };
		float[] graph3 = { 200, 500, 300, 400, 000 };

		List<LineGraph> arrGraph = new ArrayList<LineGraph>();
		arrGraph.add(new LineGraph("android", 0xaa66ff33, graph1));
		arrGraph.add(new LineGraph("ios", 0xaa00ffff, graph2));
		arrGraph.add(new LineGraph("tizen", 0xaaff0066, graph3));

		LineGraphVO vo = new LineGraphVO(legendArr, arrGraph);
		return vo;
	}

	/**
	 * make line graph using options
	 * 
	 * @return
	 */
	public LineGraphVO makeLineGraphAllSetting() {
		// 레이아웃 세팅
		// padding
	//	int paddingBottom = LineGraphVO.DEFAULT_PADDING;
		int pad = 70;
		int paddingBottom = pad;
		int paddingTop = pad;
		int paddingLeft = pad+30;
		int paddingRight = pad;

		// graph margin
	//	int marginTop = LineGraphVO.DEFAULT_MARGIN_TOP;
		int marginTop = 10;
		int marginRight = 10;

		// max value
		int maxValue = 120;

		// Y축 증가치
		int increment = 10;

		// 그래프에 담길 컨텐츠 정의

		int[] invalue = SaveDCB.getInDCB();
		int[] outvalue = SaveDCB.getOutDCB();

		String[] legendArr = { "1분 30초 전", "1분 15초 전", "1분 전",
				"30초 전", "15초 전"};
		
		float[] graph1 = { (float)invalue[0],
				(float)invalue[1],
				(float)invalue[2],
				(float)invalue[3],
				(float)invalue[4] };

		float[] graph2 = { (float)outvalue[0],
				(float)outvalue[1],
				(float)outvalue[2],
				(float)outvalue[3],
				(float)outvalue[4] };

		List<LineGraph> arrGraph = new ArrayList<LineGraph>();

		arrGraph.add(new LineGraph("외부 소음", 0xaaff0066, graph1));
		arrGraph.add(new LineGraph("청취 음량", Color.parseColor("#3D00F5"), graph2));

		LineGraphVO vo = new LineGraphVO(paddingBottom, paddingTop,
				paddingLeft, paddingRight, marginTop, marginRight, maxValue,
				increment, legendArr, arrGraph);

		// 그래프 범례 표기 설정
		int boxColor = Color.parseColor("#000000");
		int MarginTop = 70;
		int MarginRight = 70;
		int Padding = 10;
		int TextSize = 20;
		int TextColor = -16777216;
		int IconWidth = 30;
		int IconHeight = 10;
		int TextIconMargin = 10;
		int IconMargin = 10;
		GraphNameBox gb = new GraphNameBox(boxColor, MarginTop, MarginRight, Padding, TextSize, TextColor, IconWidth, IconHeight, TextIconMargin, IconMargin);

		vo.setGraphNameBox(gb);

		// 애니메이션 설정
	//	vo.setAnimation(new GraphAnimation(GraphAnimation.LINEAR_ANIMATION,	GraphAnimation.DEFAULT_DURATION));
		
		// 그래프 범례 표기 설정
	//	vo.setGraphNameBox(new GraphNameBox());
		
		// set draw graph region
		// vo.setDrawRegion(true);

		// use icon
		// arrGraph.add(new Graph(0xaa66ff33, graph1, R.drawable.icon1));
		// arrGraph.add(new Graph(0xaa00ffff, graph2, R.drawable.icon2));
		// arrGraph.add(new Graph(0xaaff0066, graph3, R.drawable.icon3));

		// LineGraphVO vo = new LineGraphVO(
		// paddingBottom, paddingTop, paddingLeft, paddingRight,
		// marginTop, marginRight, maxValue, increment, legendArr, arrGraph,
		// R.drawable.bg);
		return vo;
	}

	/*
	private class Monthly_Info extends Thread
	{
		private ArrayList<HashMap<String, String>> cost_array;
		private String lastMonth="";
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			HttpClient client = new DefaultHttpClient();
			
			String getURL = "http://14.63.212.236/index.php/cost/getRecentCost/?id="
					+ "pest";

			HttpConnectionParams.setConnectionTimeout(client.getParams(), 5000);
			HttpGet get = new HttpGet(getURL);

			HttpResponse responseGet;
			
			try {
				responseGet = client.execute(get);
				HttpEntity resEn = responseGet.getEntity();
				JSONObject object = new JSONObject(EntityUtils.toString(resEn));
				
				JSONArray cost_info = new JSONArray(object.getString("cost"));
							
				cost_array = new ArrayList<HashMap<String,String>>();
				StringCalc calc = new StringCalc();
				
				for (int i=0; i<5; i++) {
					
					JSONArray arrayData = cost_info.getJSONArray(i);
					JSONObject data = arrayData.getJSONObject(0);
					
					if( data.get("expense").equals("No_data"))
						continue;

					lastMonth = data.getString("MONTH(reg_month)");
					String sum = "0원";
					String month = "";
					for( int j=0; j<arrayData.length(); j++)
					{
						JSONObject objectData = arrayData.getJSONObject(j);
						String cost = objectData.getString("expense");
						month = objectData.getString("MONTH(reg_month)");
						
						sum = calc.getStringSum(sum, cost);
						
					}
					// 원과 자릿수 구분을 제거
					sum = sum.substring(0, sum.length() - 1);
					sum = sum.replace(",", "");
					
					HashMap<String, String> value = new HashMap<String, String>();
					value.put(month, sum);
					
					Log.e("msg", month+", "+sum);
					
					cost_array.add(value);
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return;
		}
		
		public ArrayList<HashMap<String, String>> getMonthly()
		{
			return cost_array;
		}
		
		public String getLastMonth()
		{
			return lastMonth;
		}
	}
	*/
}
