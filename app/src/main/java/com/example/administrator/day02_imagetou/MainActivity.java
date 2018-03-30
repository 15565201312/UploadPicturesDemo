package com.example.administrator.day02_imagetou;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.glide.transformations.CropCircleTransformation;
import jp.wasabeef.glide.transformations.CropSquareTransformation;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private final int IMAGE_REQUEST_CODE = 0;
    private final int RESIZE_REQUEST_CODE=3;
    private ImageView img;
    private LinearLayout linear;
    private PopupWindow window;
    private Uri imageUri;
    private final int CUT_PHOTO=2;
    private final int TAKE_PHOTO=1;
    private File file;
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");
    private  int a=1;
    private final OkHttpClient client = new OkHttpClient();
    private List<String> mImgUrls=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
    }

    private void initData() {

        img.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                View view1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_popu, null);
                TextView pai = view1.findViewById(R.id.text_pai);
                TextView xiang = view1.findViewById(R.id.text_xiang);
                TextView no= view1.findViewById(R.id.text_no);

                pai.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 启动系统提供的拍照Activity
                        Intent it = new Intent();
                        it.setAction(MediaStore.ACTION_IMAGE_CAPTURE);

                        file = new File(Environment.getExternalStorageDirectory().getPath(), "h"+(++a)+".png");
                        imageUri = Uri.fromFile(file);
                        // 以键值对的形式告诉系统照片保存的地址，键的名称不能随便写
                        it.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                        // 使用该API，it所对应的Activity销毁时会回调MainActivity的onActivityResult方法
                        // 第二个参数就是一个请求码，当有多个startActivityForResult方法的时候，就在onActivityResult用请求码判断是哪个Activity返回的数据
                        startActivityForResult(it, TAKE_PHOTO);

                        window.dismiss();

                    }
                });

                xiang.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(
                                Intent.ACTION_PICK,
                                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                 // 打开手机相册,设置请求码
                                  startActivityForResult(intent, IMAGE_REQUEST_CODE);
                    }
                });

                no.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        window.dismiss();
                    }
                });
                window = new PopupWindow(view1, GridLayout.LayoutParams.MATCH_PARENT, GridLayout.LayoutParams.WRAP_CONTENT);

                window.setBackgroundDrawable(new BitmapDrawable());
                window.setFocusable(true);
                window.setAnimationStyle(R.style.mymiao);
                window.showAtLocation(linear, Gravity.BOTTOM,0,0);

            }
        });

    }

    private void initView() {
        img = (ImageView) findViewById(R.id.img);
        linear = (LinearLayout) findViewById(R.id.linear);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //在相册里面选择好相片之后调回到现在的这个activity中
        switch (requestCode) {
            case IMAGE_REQUEST_CODE:
                resizeImage(data.getData());
                    Glide.with(this)
                            .load(data.getData())
                            .bitmapTransform(new CropCircleTransformation(this))
                            .into(img);

                //使用工具类拿到本机相册
                String realPathFromUri = RealPathFromUriUtils.getRealPathFromUri(this, data.getData());
                System.out.println("------------1>"+realPathFromUri);

                mImgUrls.add(realPathFromUri);
                uploadImg();
                    window.dismiss();
                break;
            case TAKE_PHOTO:

                // 如果发现拍照成功，则会再次构建出一个Intent对象，并把它的action指定为
                // com.android.camera.action.CROP。
                // 这个Intent是用于对拍出的照片进行裁剪的，因为摄像头拍出的照片都比较大，
                // 而我们可能只 希望截取其中的一小部分。
                // 然后给这个Intent设置一些必要的属性，并再次调用startActivityForResult()来启动裁剪程序。
                // 裁剪完成后的照片同时会输出到h1.jpg中。
                Intent intent = new Intent("com.android.camera.action.CROP");
                intent.setDataAndType(imageUri, "image/*");
                intent.putExtra("scale", true);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(intent, CUT_PHOTO);// 启动裁剪程序

                break;
            case CUT_PHOTO:

                // 发送通知，通知媒体数据库更新URL，否则图库无法显示图片
                Intent it = new Intent();
                it.setAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                it.setData(imageUri);
                sendBroadcast(it);

                // 裁剪完程序后，将图片显示在屏幕上
                Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
                img.setImageBitmap(bitmap);
                //拍照的图片上传
                mImgUrls.add(file.getPath());

                uploadImg();
              /*  Glide.with(this)
                        .load(bitmap)
                        .bitmapTransform(new CropCircleTransformation(this))
                        .into(img);*/
                break;
        }
        }
    //这里增加裁剪
    public void resizeImage(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        //裁剪的大小
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data", true);
        //设置返回码
        startActivityForResult(intent, RESIZE_REQUEST_CODE);
    }


    private void uploadImg() {

        // mImgUrls为存放图片的url集合

      //  mImgUrls.add(Environment.getExternalStorageDirectory().getAbsolutePath()+"/a.png");

        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        for (int i = 0; i < mImgUrls.size(); i++) {
            File f = new File(mImgUrls.get(i));
            if (f != null) {
                builder.addFormDataPart("img", f.getName(), RequestBody.create(MEDIA_TYPE_PNG, f));
            }
        }

        //添加其它信息
//        builder.addFormDataPart("time",takePicTime);
//        builder.addFormDataPart("mapX", SharedInfoUtils.getLongitude());
//        builder.addFormDataPart("mapY",SharedInfoUtils.getLatitude());
//        builder.addFormDataPart("name",SharedInfoUtils.getUserName());


        MultipartBody requestBody = builder.build();
        //构建请求
        Request request = new Request.Builder()
                .url("http://192.168.0.107:8080/UploadDemo4/UploadFile")//地址
                .post(requestBody)//添加请求体
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                System.out.println("上传失败:e.getLocalizedMessage() = " + e.getLocalizedMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                System.out.println("上传照片成功：response = " + response.body().string());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_LONG).show();


                    }
                });
            }
        });

    }

}


