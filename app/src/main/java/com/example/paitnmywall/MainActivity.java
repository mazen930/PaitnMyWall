package com.example.paitnmywall;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {

    enum LoadImage {
        PICK_FROM_CAMERA,
        PICK_FROM_GALLERY
    }

    int touchCount = 0;
    Point t1;
    Bitmap bitmap;
    int chosenColor = Color.RED;
    private String imageFilePath;
    private boolean texture = false;
    private String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };
    ImageView imageFromData, inputImage, greyScaleImage, floodFillImage, HSVImage, cannyEdgeImage,
            outputImage;
    LinearLayout topLayout, middleLayout, bottomLayout;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("Error Initialization", "Can't load opencv library");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    void initialize() {
        t1 = new Point();
        imageFromData = findViewById(R.id.imageFromData);
        inputImage = findViewById(R.id.inputImage);
        greyScaleImage = findViewById(R.id.greyScaleImage);
        floodFillImage = findViewById(R.id.floodFillImage);
        HSVImage = findViewById(R.id.HSVImage);
        cannyEdgeImage = findViewById(R.id.cannyEdgeImage);
        outputImage = findViewById(R.id.outputImage);

        topLayout = findViewById(R.id.topLayout);
        middleLayout = findViewById(R.id.middleLayout);
        bottomLayout = findViewById(R.id.bottomLayout);
    }

    private void openCamera() throws IOException {
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Real time get permission
            ActivityCompat.requestPermissions(this, PERMISSIONS, LoadImage.PICK_FROM_CAMERA.ordinal());
        } else {
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = null;
            photoFile = createImageFile();
            Uri photoURI = FileProvider.getUriForFile(this, "com.example.paitnmywall.provider", photoFile);
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(captureIntent, LoadImage.PICK_FROM_CAMERA.ordinal());
        }

    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        );
        imageFilePath = image.getAbsolutePath();
        return image;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_open_img:
                showImage();
                Toast.makeText(this, "1", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_process_image:
                showResultLayouts();
                Toast.makeText(this, "2", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_take_photo:
                try {
                    openCamera();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(this, "3", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_get_gallery:
                openGallery();
                Toast.makeText(this, "4", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_get_color:
                chooseColor();
                Toast.makeText(this, "5", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_get_texture:
                chooseTexture();
                Toast.makeText(this, "6", Toast.LENGTH_SHORT).show();
                return true;
            default:
                Toast.makeText(this, "No match found", Toast.LENGTH_SHORT).show();
                return false;
        }
    }

    private void chooseTexture() {
        texture = true;
    }

    private void chooseColor() {
        texture = false;
        // this is used library to get color picker
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, chosenColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                // setting choosing color with the color selected by user
                chosenColor = color;

            }
        });
        colorPicker.show();
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, LoadImage.PICK_FROM_GALLERY.ordinal());
    }

    private void showResultLayouts() {
        imageFromData.setVisibility(View.GONE);
        topLayout.setVisibility(View.VISIBLE);
        middleLayout.setVisibility(View.VISIBLE);
        bottomLayout.setVisibility(View.VISIBLE);
    }

    private void showImage() {
        imageFromData.setVisibility(View.VISIBLE);
        topLayout.setVisibility(View.GONE);
        middleLayout.setVisibility(View.GONE);
        bottomLayout.setVisibility(View.GONE);
        try {
            imageFromData.setImageBitmap(bitmap);
        } catch (Exception e) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        }
    }

    // show images that accept matrix of image on order to apply filters on it
    private void showImage(Mat image, ImageView imageView) throws IOException {
        Bitmap bitmap1;
        bitmap1 = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap1);
        imageView.setImageBitmap(bitmap1);
        this.bitmap = bitmap1;
        saveImage(bitmap);
    }

    private void saveImage(Bitmap image) throws IOException {
        File pictureFile = createImageFile();
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("Error here", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.e("Error here", "Error accessing file: " + e.getMessage());
        }
    }

    private Bitmap getResizedBitmap(Bitmap bitmap, float w, float h) {
        float width = bitmap.getWidth();
        float height = bitmap.getHeight();
        float scaleWidth = w / width;
        float scaleHeight = h / height;

        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();

        // RESIZE THE BIT MAP
        // this function can also accept focus point and zooming in and out through it
        matrix.postScale(scaleWidth, scaleHeight);

        // “RECREATE” THE NEW BITMAP
        return Bitmap.createScaledBitmap(bitmap, 120, 120, true);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LoadImage.PICK_FROM_CAMERA.ordinal() && resultCode == RESULT_OK) {
            imageFromData.setImageURI(Uri.parse(imageFilePath));
            bitmap = drawableToBitmap(imageFromData.getDrawable());
            bitmap = getResizedBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight());
            showImage();
        } else if (requestCode == LoadImage.PICK_FROM_GALLERY.ordinal() && resultCode == RESULT_OK) {
            LoadImage.PICK_FROM_GALLERY.ordinal();
            assert data != null;
            loadFromGallery(data);
        }
        imageFromData.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (touchCount == 0) {
                        t1.x = motionEvent.getX();
                        t1.y = motionEvent.getY();
                        if (texture) {
                            try {
                                applyTexture(bitmap, t1);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                rpPaintHSV(bitmap, t1);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return false;
            }
        });
    }

    private Mat rpPaintHSV(Bitmap bitmap, Point p) throws IOException {
        float cannyMinThreshold = (float) 30.0;
        float ratio = (float) 2.5;
        // show intermediate step results
        // grid created here to do that
        // Creating the mask
        showResultLayouts();
        Mat mRgbMat = new Mat();
        Utils.bitmapToMat(bitmap, mRgbMat);
        showImage(mRgbMat, inputImage);
        Imgproc.cvtColor(mRgbMat, mRgbMat, Imgproc.COLOR_RGBA2RGB);
        Mat mask = new Mat(new Size(mRgbMat.width() / 8.0, mRgbMat.height() / 8.0), CvType.CV_8UC1, new Scalar(0.0));


        Mat img = new Mat();
        mRgbMat.copyTo(img);
        // grayscale
        Mat mGreyScaleMat = new Mat();
        Imgproc.cvtColor(mRgbMat, mGreyScaleMat, Imgproc.COLOR_RGB2GRAY, 3);
        Imgproc.medianBlur(mGreyScaleMat, mGreyScaleMat, 3);
        Mat cannyGreyMat = new Mat();
        Imgproc.Canny(mGreyScaleMat, cannyGreyMat, cannyMinThreshold, cannyMinThreshold * ratio, 3);
        showImage(cannyGreyMat, greyScaleImage);
        //hsv
        Mat hsvImage = new Mat();
        Imgproc.cvtColor(img, hsvImage, Imgproc.COLOR_RGB2HSV);
        //got the hsv values
        ArrayList<Mat> list = new ArrayList<Mat>(3);
        Core.split(hsvImage, list);
        Mat sChannelMat = new Mat();
        Core.merge(new ArrayList<Mat>(Collections.singletonList(list.get(1))), sChannelMat);
        Imgproc.medianBlur(sChannelMat, sChannelMat, 3);
        showImage(sChannelMat, floodFillImage);
        // canny
        Mat cannyMat = new Mat();
        Imgproc.Canny(sChannelMat, cannyMat, cannyMinThreshold, cannyMinThreshold * ratio, 3);
        showImage(cannyMat, HSVImage);
        Core.addWeighted(cannyMat, 0.5, cannyGreyMat, 0.5, 0.0, cannyMat);
        Imgproc.dilate(cannyMat, cannyMat, mask, new Point(0.0, 0.0), 5);
        showImage(cannyMat, cannyEdgeImage);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float height = displayMetrics.heightPixels;
        float width = displayMetrics.widthPixels;
        Point seedPoint = new Point(p.x * (mRgbMat.width() / width), p.y * (mRgbMat.height() / height));
        Imgproc.resize(cannyMat, cannyMat, new Size(cannyMat.width() + 2.0, cannyMat.height() + 2.0));
        Imgproc.medianBlur(mRgbMat, mRgbMat, 15);
        int floodFillFlag = 8;
        Imgproc.floodFill(
                mRgbMat,
                cannyMat,
                seedPoint,
                new Scalar(Color.red(chosenColor), Color.green(chosenColor), Color.blue(chosenColor)),
                new Rect(),
                new Scalar(5.0, 5.0, 5.0),
                new Scalar(5.0, 5.0, 5.0),
                floodFillFlag
        );
        // showImage(mRgbMat,floodFillImage)
        Imgproc.dilate(mRgbMat, mRgbMat, mask, new Point(0.0, 0.0), 5);
        //got the hsv of the mask image
        Mat rgbHsvImage = new Mat();
        Imgproc.cvtColor(mRgbMat, rgbHsvImage, Imgproc.COLOR_RGB2HSV);
        ArrayList<Mat> list1 = new ArrayList<Mat>(3);
        Core.split(rgbHsvImage, list1);
        //merged the “v” of original image with mRgb mat
        Mat result = new Mat();
        Core.merge(new ArrayList<Mat>(Arrays.asList(list1.get(0), list1.get(1), list.get(2))), result);
        // converted to rgb
        Imgproc.cvtColor(result, result, Imgproc.COLOR_HSV2RGB);
        Core.addWeighted(result, 0.7, img, 0.3, 0.0, result);
        showImage(result, outputImage);
        return result;
    }

    private void applyTexture(Bitmap bitmap, Point p) throws IOException {
        float cannyMinThreshold = (float) 30.0;
        float ratio = (float) 2.5;
        // show intermediate step results
        // grid created here to do that
        showResultLayouts();
        Mat mRgbMat = new Mat();
        Utils.bitmapToMat(bitmap, mRgbMat);
        showImage(mRgbMat, inputImage);
        Imgproc.cvtColor(mRgbMat, mRgbMat, Imgproc.COLOR_RGBA2RGB);
        Mat mask = new Mat(new Size(mRgbMat.width() / 8.0, mRgbMat.height() / 8.0), CvType.CV_8UC1, new Scalar(0.0));
        // Imgproc.dilate(mRgbMat, mRgbMat,mask, Point(0.0,0.0), 5)
        Mat img = new Mat();
        mRgbMat.copyTo(img);
        // grayscale
        Mat mGreyScaleMat = new Mat();
        Imgproc.cvtColor(mRgbMat, mGreyScaleMat, Imgproc.COLOR_RGB2GRAY, 3);
        Imgproc.medianBlur(mGreyScaleMat, mGreyScaleMat, 3);
        Mat cannyGreyMat = new Mat();
        Imgproc.Canny(mGreyScaleMat, cannyGreyMat, cannyMinThreshold, cannyMinThreshold * ratio, 3);
        showImage(cannyGreyMat, greyScaleImage);
        //hsv
        Mat hsvImage = new Mat();
        Imgproc.cvtColor(img, hsvImage, Imgproc.COLOR_RGB2HSV);
        //got the hsv values
        ArrayList<Mat> list = new ArrayList<Mat>(3);
        Core.split(hsvImage, list);
        Mat sChannelMat = new Mat();
        Core.merge(new ArrayList<Mat>(Collections.singletonList(list.get(1))), sChannelMat);
        Imgproc.medianBlur(sChannelMat, sChannelMat, 3);
        showImage(sChannelMat, floodFillImage);
        // canny
        Mat cannyMat = new Mat();
        Imgproc.Canny(sChannelMat, cannyMat, cannyMinThreshold, cannyMinThreshold * ratio, 3);
        showImage(cannyMat, HSVImage);
        Core.addWeighted(cannyMat, 0.5, cannyGreyMat, 0.5, 0.0, cannyMat);
        Imgproc.dilate(cannyMat, cannyMat, mask, new Point(0.0, 0.0), 5);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float height = displayMetrics.heightPixels;
        float width = displayMetrics.widthPixels;
        Point seedPoint = new Point(p.x * (mRgbMat.width() / width), p.y * (mRgbMat.height() / height));
        Imgproc.resize(cannyMat, cannyMat, new Size(cannyMat.width() + 2.0, cannyMat.height() + 2.0));
        Mat cannyMat1 = new Mat();
        cannyMat.copyTo(cannyMat1);
        Mat wallMask = new Mat(mRgbMat.size(), mRgbMat.type());
        int floodFillFlag = 8;
        Imgproc.floodFill(
                wallMask,
                cannyMat,
                seedPoint,
                new Scalar(255.0, 255.0, 255.0),
                new Rect(),
                new Scalar(5.0, 5.0, 5.0),
                new Scalar(5.0, 5.0, 5.0),
                floodFillFlag
        );
        showImage(wallMask, greyScaleImage);
        showImage(cannyMat, cannyEdgeImage);
        Imgproc.floodFill(
                mRgbMat,
                cannyMat1,
                seedPoint,
                new Scalar(0.0, 0.0, 0.0),
                new Rect(),
                new Scalar(5.0, 5.0, 5.0),
                new Scalar(5.0, 5.0, 5.0),
                floodFillFlag
        );
        showImage(mRgbMat, HSVImage);
        Mat texture = getTextureImage();
        Mat textureImgMat = new Mat();
        Core.bitwise_and(wallMask, texture, textureImgMat);
        showImage(textureImgMat, floodFillImage);
        Mat resultImage = new Mat();
        Core.bitwise_or(textureImgMat, mRgbMat, resultImage);
        showImage(resultImage, outputImage);
        ////alpha blending
        //got the hsv of the mask image
        Mat rgbHsvImage = new Mat();
        Imgproc.cvtColor(resultImage, rgbHsvImage, Imgproc.COLOR_RGB2HSV);
        ArrayList<Mat> list1 = new ArrayList<Mat>(3);
        Core.split(rgbHsvImage, list1);
        //merged the “v” of original image with mRgb mat
        Mat result = new Mat();
        Core.merge(new ArrayList<Mat>(Arrays.asList(list1.get(0), list1.get(1), list.get(2))), result);
        // converted to rgb
        Imgproc.cvtColor(result, result, Imgproc.COLOR_HSV2RGB);
        Core.addWeighted(result, 0.8, img, 0.2, 0.0, result);
        showImage(result, outputImage);
    }

    private void loadFromGallery(Intent data) {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(Objects.requireNonNull(data.getData()), filePathColumn, null, null, null);
        assert cursor != null;
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        bitmap = BitmapFactory.decodeFile(picturePath);
        bitmap = getResizedBitmap(bitmap, (float) (bitmap.getWidth() / 5.0), (float) (bitmap.getHeight() / 5.0));
        showImage();
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {//this function change from drawable to bitmap

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private Mat getTextureImage() {
        Bitmap textureImage = BitmapFactory.decodeResource(getResources(), R.drawable.texture_small_brick_red);
        textureImage = getResizedBitmap(textureImage, bitmap.getWidth(), bitmap.getHeight());
        Mat texture = new Mat();
        Utils.bitmapToMat(textureImage, texture);
        Imgproc.cvtColor(texture, texture, Imgproc.COLOR_RGBA2RGB);
        return texture;
    }

}

