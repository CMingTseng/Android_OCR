package com.mikewong.tool.tesseract.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mikewong.tool.tesseract.BuildConfig;
import com.mikewong.tool.tesseract.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import idv.neo.utils.BitmapUtils;
import idv.neo.utils.FolderFileUtils;
import idv.neo.utils.GetBitmapTask;
import idv.neo.utils.GetOCRResultTask;

import static com.mikewong.tool.tesseract.C.PHOTO_CAPTURE;
import static com.mikewong.tool.tesseract.C.PHOTO_RESULT;
import static idv.neo.utils.FolderFileUtils.getSDPath;

public class MainFragment extends Fragment {
    private static final String TAG = MainFragment.class.getSimpleName();
    private static String LANGUAGE = "eng";
    private static String IMG_PATH = getSDPath() + File.separator + BuildConfig.OCRIMAGEFOLDERNAME;
    private static TextView mResult;
    private static ImageView mSelectImage;
    private static ImageView ivTreated;
    private static CheckBox chPreTreat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = container.getContext();
        final View root = inflater.inflate(R.layout.fragment_main, container, false);
        mResult = (TextView) root.findViewById(R.id.tv_result);
        mSelectImage = (ImageView) root.findViewById(R.id.iv_selected);
        ivTreated = (ImageView) root.findViewById(R.id.iv_treated);
        final Button camera = (Button) root.findViewById(R.id.btn_camera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(IMG_PATH, BuildConfig.CAMERAFILE)));
                startActivityForResult(intent, PHOTO_CAPTURE);
            }
        });
        final Button select = (Button) root.findViewById(R.id.btn_select);
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                intent.putExtra("crop", "true");
                intent.putExtra("scale", true);
                intent.putExtra("return-data", false);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(IMG_PATH, BuildConfig.OCRIMAGEFILENAME)));
                intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                intent.putExtra("noFaceDetection", true); // no face detection
                startActivityForResult(intent, PHOTO_RESULT);
            }
        });
        chPreTreat = (CheckBox) root.findViewById(R.id.ch_pretreat);
        final RadioGroup choice_lang = (RadioGroup) root.findViewById(R.id.choice_lang);
        final String[] ocr_langs = context.getResources().getStringArray(R.array.lang_array);
        final String[] ocr_lang_files = context.getResources().getStringArray(R.array.lang_file_array);
        final int count = ocr_langs.length;
        for (int i = 0; i < count; i++) {
            final RadioButton rd = new RadioButton(context);
            rd.setId(i);
            rd.setText(ocr_langs[i]);
            choice_lang.addView(rd);
        }
        choice_lang.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                LANGUAGE = ocr_lang_files[checkedId];
            }

        });
        return root;
    }

    /**
     * 調用系統圖片編輯進行裁剪
     */
    public void startPhotoCrop(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(IMG_PATH, BuildConfig.OCRIMAGEFILENAME)));
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, PHOTO_RESULT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Context context = getContext();
        if (resultCode == Activity.RESULT_CANCELED)
            return;
        if (requestCode == PHOTO_CAPTURE) {
            mResult.setText("abc");
            startPhotoCrop(Uri.fromFile(new File(IMG_PATH, BuildConfig.CAMERAFILE)));
        }
        // 處理結果
        if (requestCode == PHOTO_RESULT) {
            new GetBitmapTask(new GetBitmapTask.OnTaskCompleted() {
                @Override
                public void onTaskCompleted(final Bitmap bitmap) {

                    if (chPreTreat.isChecked())
                        mResult.setText(getString(R.string.preprocess));
                    else
                        mResult.setText(getString(R.string.identificationings));
                    // 顯示選擇的圖片

                    final ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    Glide.with(context).load(stream.toByteArray()).asBitmap().into(mSelectImage);
                    Bitmap treated = BitmapUtils.doPretreatment(bitmap);
                    mResult.setText(getString(R.string.identificationings));
                    treated.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    Glide.with(context).load(stream.toByteArray()).asBitmap().into(ivTreated);
                    if (chPreTreat.isChecked()) {
                        treated = BitmapUtils.doPretreatment(bitmap);
                    } else {
                        treated = BitmapUtils.converyToGrayImg(bitmap);
                    }
                    new GetOCRResultTask(new GetOCRResultTask.OnTaskCompleted() {
                        @Override
                        public void onTaskCompleted(String result) {
                            Log.d(TAG, "Show  onTaskCompleted : " + result);
                            if (result.equals(""))
                                mResult.setText(getString(R.string.identification_fail));
                            else
                                mResult.setText(result);
                        }
                    }).execute(treated, LANGUAGE);
                }
            }).execute(context, new File(IMG_PATH, BuildConfig.OCRIMAGEFILENAME));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final Context context = getContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (FolderFileUtils.checkSDCardExist()) {
                    if (!FolderFileUtils.checkTesseractOCRFolderExist()) {
                        FolderFileUtils.createTesseractOCRFolder();
                    }
                    final ArrayList<File> folders = FolderFileUtils.getExternalStorageDirectorys(new ArrayList<File>());
                    File target = null;
                    for (File folder : folders) {
                        if (FolderFileUtils.isTesseractOCRFolder(folder)) {
                            target = folder;
                            break;
                        }
                    }
                    final AssetManager am = context.getAssets();
                    final String[] files = FolderFileUtils.getAssetsFilterFiles(am, new FolderFileUtils.TesseractOCRTrainedDataFilter());
                    if (files != null) {
                        for (String filename : files) {
                            final File outFile = new File(target.toString() + File.separator + filename);
                            if (!outFile.exists()) {
                                InputStream in = null;
                                OutputStream out = null;
                                try {
                                    in = am.open(filename);
                                    out = new FileOutputStream(outFile);
                                    FolderFileUtils.writeFile(in, out);
                                } catch (IOException e) {
                                    Log.e(TAG, "Failed to copy asset file: " + filename, e);
                                } finally {
                                    if (in != null) {
                                        try {
                                            in.close();
                                        } catch (IOException e) {
                                            // NOOP
                                        }
                                    }
                                    if (out != null) {
                                        try {
                                            out.flush();
                                            out.close();
                                        } catch (IOException e) {
                                            // NOOP
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 若檔夾不存在 首先創建檔夾
                final File path = new File(IMG_PATH);
                if (!path.exists()) {
                    path.mkdirs();
                }
            }
        }).start();
    }
}
