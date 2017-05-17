package com.mikewong.tool.tesseract;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;

public class MainActivity extends Activity {

	private static final int PHOTO_CAPTURE = 0x11;// 拍照
	private static final int PHOTO_RESULT = 0x12;// 結果

	private static String LANGUAGE = "eng";
	private static String IMG_PATH = getSDPath() + java.io.File.separator
			+ "ocrtest";

	private static TextView tvResult;
	private static ImageView ivSelected;
	private static ImageView ivTreated;
	private static Button btnCamera;
	private static Button btnSelect;
	private static CheckBox chPreTreat;
	private static RadioGroup radioGroup;
	private static String textResult;
	private static Bitmap bitmapSelected;
	private static Bitmap bitmapTreated;
	private static final int SHOWRESULT = 0x101;
	private static final int SHOWTREATEDIMG = 0x102;

	// 該handler用於處理修改結果的任務
	public static Handler myHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SHOWRESULT:
					if (textResult.equals(""))
						tvResult.setText("識別失敗");
					else
						tvResult.setText(textResult);
					break;
				case SHOWTREATEDIMG:
					tvResult.setText("識別中......");
					showPicture(ivTreated, bitmapTreated);
					break;
			}
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 若檔夾不存在 首先創建檔夾
		File path = new File(IMG_PATH);
		if (!path.exists()) {
			path.mkdirs();
		}

		tvResult = (TextView) findViewById(R.id.tv_result);
		ivSelected = (ImageView) findViewById(R.id.iv_selected);
		ivTreated = (ImageView) findViewById(R.id.iv_treated);
		btnCamera = (Button) findViewById(R.id.btn_camera);
		btnSelect = (Button) findViewById(R.id.btn_select);
		chPreTreat = (CheckBox) findViewById(R.id.ch_pretreat);
		radioGroup = (RadioGroup) findViewById(R.id.radiogroup);

		btnCamera.setOnClickListener(new cameraButtonListener());
		btnSelect.setOnClickListener(new selectButtonListener());

		// 用於設置解析語言
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
					case R.id.rb_en:
						LANGUAGE = "eng";
						break;
					case R.id.rb_ch:
						LANGUAGE = "chi_sim";
						break;
				}
			}

		});

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == Activity.RESULT_CANCELED)
			return;

		if (requestCode == PHOTO_CAPTURE) {
			tvResult.setText("abc");
			startPhotoCrop(Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
		}

		// 處理結果
		if (requestCode == PHOTO_RESULT) {
			bitmapSelected = decodeUriAsBitmap(Uri.fromFile(new File(IMG_PATH,
					"temp_cropped.jpg")));
			if (chPreTreat.isChecked())
				tvResult.setText("預處理中......");
			else
				tvResult.setText("識別中......");
			// 顯示選擇的圖片
			showPicture(ivSelected, bitmapSelected);

			// 新線程來處理識別
			new Thread(new Runnable() {
				@Override
				public void run() {
					if (chPreTreat.isChecked()) {
						bitmapTreated = ImgPretreatment
								.doPretreatment(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					} else {
						bitmapTreated = ImgPretreatment
								.converyToGrayImg(bitmapSelected);
						Message msg = new Message();
						msg.what = SHOWTREATEDIMG;
						myHandler.sendMessage(msg);
						textResult = doOcr(bitmapTreated, LANGUAGE);
					}
					Message msg2 = new Message();
					msg2.what = SHOWRESULT;
					myHandler.sendMessage(msg2);
				}

			}).start();

		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	// 拍照識別
	class cameraButtonListener implements OnClickListener {

		@Override
		public void onClick(View arg0) {
			Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp.jpg")));
			startActivityForResult(intent, PHOTO_CAPTURE);
		}
	};

	// 從相冊選取照片並裁剪
	class selectButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType("image/*");
			intent.putExtra("crop", "true");
			intent.putExtra("scale", true);
			intent.putExtra("return-data", false);
			intent.putExtra(MediaStore.EXTRA_OUTPUT,
					Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
			intent.putExtra("outputFormat",
					Bitmap.CompressFormat.JPEG.toString());
			intent.putExtra("noFaceDetection", true); // no face detection
			startActivityForResult(intent, PHOTO_RESULT);
		}

	}

	// 將圖片顯示在view中
	public static void showPicture(ImageView iv, Bitmap bmp){
		iv.setImageBitmap(bmp);
	}

	/**
	 * 進行圖片識別
	 *
	 * @param bitmap
	 *            待識別?計?
	 * @param language
	 *            識別語言
	 * @return 識別結果字串
	 */
	public String doOcr(Bitmap bitmap, String language) {
		TessBaseAPI baseApi = new TessBaseAPI();

		baseApi.init(getSDPath(), language);

		// 必須加此行，tess-two要求BMP必須為此配置
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		baseApi.setImage(bitmap);

		String text = baseApi.getUTF8Text();

		baseApi.clear();
		baseApi.end();

		return text;
	}

	/**
	 * 獲取sd卡的路徑
	 *
	 * @return 路徑的字串
	 */
	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判斷sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 獲取外存目錄
		}
		return sdDir.toString();
	}

	/**
	 * 調用系統圖片編輯進行裁剪
	 */
	public void startPhotoCrop(Uri uri) {
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri, "image/*");
		intent.putExtra("crop", "true");
		intent.putExtra("scale", true);
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(new File(IMG_PATH, "temp_cropped.jpg")));
		intent.putExtra("return-data", false);
		intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
		intent.putExtra("noFaceDetection", true); // no face detection
		startActivityForResult(intent, PHOTO_RESULT);
	}

	/**
	 * 根據URI獲取點陣圖
	 *
	 * @param uri
	 * @return 對應的點陣圖
	 */
	private Bitmap decodeUriAsBitmap(Uri uri) {
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(getContentResolver()
					.openInputStream(uri));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		return bitmap;
	}
}
