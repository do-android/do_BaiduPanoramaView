package doext.implement;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.widget.FrameLayout;

import com.baidu.lbsapi.BMapManager;
import com.baidu.lbsapi.MKGeneralListener;
import com.baidu.lbsapi.panoramaview.ImageMarker;
import com.baidu.lbsapi.panoramaview.OnTabMarkListener;
import com.baidu.lbsapi.panoramaview.PanoramaView;
import com.baidu.lbsapi.panoramaview.PanoramaView.ImageDefinition;
import com.baidu.lbsapi.panoramaview.PanoramaViewListener;
import com.baidu.lbsapi.panoramaview.TextMarker;
import com.baidu.lbsapi.tools.Point;

import core.DoServiceContainer;
import core.helper.DoIOHelper;
import core.helper.DoImageLoadHelper;
import core.helper.DoJsonHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoUIModule;
import doext.define.do_BaiduPanoramaView_IMethod;
import doext.define.do_BaiduPanoramaView_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,
 * do_BaiduPanoramaView_IMethod接口； #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_BaiduPanoramaView_View extends FrameLayout implements DoIUIModuleView, do_BaiduPanoramaView_IMethod {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_BaiduPanoramaView_MAbstract model;
	private BMapManager mBMapManager = null;
	private Map<String, com.baidu.pano.platform.comapi.a.a> markers;
	private PanoramaView mPanoramaView;

	public do_BaiduPanoramaView_View(Context context) {
		super(context);
		if (mBMapManager == null) {
			mBMapManager = new BMapManager(context.getApplicationContext());
		}

		if (!mBMapManager.init(new MyGeneralListener())) {
			DoServiceContainer.getLogEngine().writeInfo("do_BaiduPanoramaView_View \n\t", "BMapManager  初始化错误!");
		}

		mPanoramaView = new PanoramaView(context);
		FrameLayout.LayoutParams fParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
		this.addView(mPanoramaView, fParams);

		markers = new HashMap<String, com.baidu.pano.platform.comapi.a.a>();
	}

	// 常用事件监听，用来处理通常的网络错误，授权验证错误等
	private static class MyGeneralListener implements MKGeneralListener {

		@Override
		public void onGetPermissionState(int iError) {
			// 非零值表示key验证未通过
			if (iError != 0) {
				// 授权Key错误：
				DoServiceContainer.getLogEngine().writeInfo("do_BaiduPanoramaView_View \n\t", "key error,并检查您的网络连接是否正常！error: " + iError);
			}
		}
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_BaiduPanoramaView_MAbstract) _doUIModule;

		mPanoramaView.setPanoramaZoomLevel(2);
		mPanoramaView.setPanoramaImageLevel(ImageDefinition.ImageDefinitionMiddle);
		mPanoramaView.setPanoramaViewListener(new PanoramaViewListener() {

			@Override
			public void onLoadPanoramaError(String error) {

			}

			@Override
			public void onLoadPanoramaEnd(String json) {
				model.setPropertyValue("zoomLevel", mPanoramaView.getPanoramaZoomLevel() + "");
			}

			@Override
			public void onLoadPanoramaBegin() {

			}
		});
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("zoomLevel")) {
			int _zoomLevel = DoTextHelper.strToInt(_changedValues.get("zoomLevel"), 2);
			mPanoramaView.setPanoramaZoomLevel(_zoomLevel);
		}

		if (_changedValues.containsKey("imageLevel")) {
			// 设置全景图片的显示级别，1为较低清晰度, 2为中等清晰度 , 3为较高清晰度
			int _imageLevel = DoTextHelper.strToInt(_changedValues.get("imageLevel"), 2);
			switch (_imageLevel) {
			case 1:
				mPanoramaView.setPanoramaImageLevel(ImageDefinition.ImageDefinitionLow);
				break;
			case 2:
				mPanoramaView.setPanoramaImageLevel(ImageDefinition.ImageDefinitionMiddle);
				break;
			case 3:
				mPanoramaView.setPanoramaImageLevel(ImageDefinition.ImageDefinitionHigh);
				break;
			}
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("showPanoramaView".equals(_methodName)) {
			this.showPanoramaView(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("addImageMarkers".equals(_methodName)) {
			this.addImageMarkers(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("addTextMarkers".equals(_methodName)) {
			this.addTextMarkers(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("removeMarker".equals(_methodName)) {
			this.removeMarker(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("removeAll".equals(_methodName)) {
			this.removeAll(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}

		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V），获取参数值使用API提供DoJsonHelper类；
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		// ...do something
		return false;
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		if (null != mPanoramaView) {
			mPanoramaView.destroy();
		}
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	/**
	 * 添加一组缩略图标记；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void addImageMarkers(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		try {
			JSONArray dataArray = DoJsonHelper.getJSONArray(_dictParas, "data");
			for (int i = 0; i < dataArray.length(); i++) {
				JSONObject _obj = dataArray.getJSONObject(i);
				String _id = DoJsonHelper.getString(_obj, "id", "");
				Double _latitude = DoJsonHelper.getDouble(_obj, "latitude", 39.915174);
				Double _longitude = DoJsonHelper.getDouble(_obj, "longitude", 116.403901);
				String _url = DoJsonHelper.getString(_obj, "url", "");

				// 构建Marker图标
				ImageMarker _marker = new ImageMarker();
				_marker.setMarkerPosition(new Point(_longitude, _latitude));
				_marker.setMarker(new BitmapDrawable(getLocalBitmap(_url)));
				_marker.setOnTabMarkListener(new MyOnTabMarkListener(_id, _latitude, _longitude, "ImageMark", _url));
				mPanoramaView.addMarker(_marker);
				markers.put(_id, _marker);
			}
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("添加一组标记异常", e);
			_invokeResult.setResultBoolean(false);
		}
		_invokeResult.setResultBoolean(true);
	}

	private class MyOnTabMarkListener implements OnTabMarkListener {

		private JSONObject obj;
		private DoInvokeResult invokeResult;

		private MyOnTabMarkListener(String _id, double _latitude, double _longitude, String _type, String _info) {
			this.obj = new JSONObject();
			this.invokeResult = new DoInvokeResult(model.getUniqueKey());
			try {
				this.obj.put("id", _id);
				this.obj.put("latitude", _latitude);
				this.obj.put("longitude", _longitude);
				this.obj.put("type", _type);
				this.obj.put("info", _info);
			} catch (Exception e) {
				this.invokeResult.setException(e);
			}
		}

		@Override
		public void onTab() {
			this.invokeResult.setResultNode(obj);
			model.getEventCenter().fireEvent("touchMarker", this.invokeResult);
		}
	}

	/**
	 * 添加一组文本标记；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void addTextMarkers(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {

		try {
			JSONArray _array = DoJsonHelper.getJSONArray(_dictParas, "data");
			for (int i = 0; i < _array.length(); i++) {
				JSONObject _obj = _array.getJSONObject(i);
				String _id = DoJsonHelper.getString(_obj, "id", "");
				Double _latitude = DoJsonHelper.getDouble(_obj, "latitude", 39.915174);
				Double _longitude = DoJsonHelper.getDouble(_obj, "longitude", 116.403901);
				String _text = DoJsonHelper.getString(_obj, "text", "");
				String _fontColor = DoJsonHelper.getString(_obj, "fontColor", "");
				String _fontSize = DoJsonHelper.getString(_obj, "fontSize", "");

				// 构建Marker图标
				TextMarker _marker = new TextMarker();
				_marker.setMarkerPosition(new Point(_longitude, _latitude));
				_marker.setText(_text);
				_marker.setFontColor(DoUIModuleHelper.getColorFromString(_fontColor, Color.BLACK));
				_marker.setFontSize(DoUIModuleHelper.getDeviceFontSize(model, _fontSize));

				_marker.setOnTabMarkListener(new MyOnTabMarkListener(_id, _latitude, _longitude, "TextMark", _text));
				mPanoramaView.addMarker(_marker);
				markers.put(_id, _marker);
			}
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("添加一组标记异常", e);
			_invokeResult.setResultBoolean(false);
		}
		_invokeResult.setResultBoolean(true);
	}

	/**
	 * 移除所有标记；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void removeAll(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		mPanoramaView.removeAllMarker();
	}

	/**
	 * 移除一组指定标记；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void removeMarker(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {

		JSONArray _array = DoJsonHelper.getJSONArray(_dictParas, "ids");
		for (int i = 0; i < _array.length(); i++) {
			String _id = _array.getString(i);
			if (markers.containsKey(_id)) {
				mPanoramaView.removeMarker(markers.get(_id));
				markers.remove(_id);
			} else {
				DoServiceContainer.getLogEngine().writeError("do_BaiduPanoramaView_View removeMarker \r\n", new Exception("标记id:" + _array.get(i) + "不存在"));
			}
		}
	}

	/**
	 * 显示全景图；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void showPanoramaView(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		Double latitude = DoJsonHelper.getDouble(_dictParas, "latitude", -1);
		Double longitude = DoJsonHelper.getDouble(_dictParas, "longitude", -1);

		if (latitude > 0 && longitude > 0) {
			mPanoramaView.setPanorama(longitude, latitude);
		} else {
			throw new Exception("中心点经纬度不合法");
		}
	}

	public Bitmap getLocalBitmap(String local) throws Exception {
		Bitmap bitmap = null;
		if (null == DoIOHelper.getHttpUrlPath(local) && local != null && !"".equals(local)) {
			String path = DoIOHelper.getLocalFileFullPath(this.model.getCurrentPage().getCurrentApp(), local);
			bitmap = DoImageLoadHelper.getInstance().loadLocal(path, -1, -1);
		} else {
			throw new Exception("标记缩略图,只支持本地图片");
		}
		return bitmap;
	}
}