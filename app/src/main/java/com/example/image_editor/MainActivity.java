package com.example.image_editor;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import com.example.image_editor.adapters.RecyclerViewAdapter;
import com.example.image_editor.utils.ColorFIltersCollection;
import com.example.image_editor.utils.drag.DrivingViews;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ImageView mImageView;
    private Bitmap mBitmap;
    private Bitmap mBitmapDrawing;

    private RecyclerView mRecyclerView;
    private LinearLayout mPlaceHolder;
    private LinearLayout mHeader;

    private ArrayList<String> mNames = new ArrayList<>();
    private ArrayList<Integer> mImageUrls = new ArrayList<>();
    private ArrayList<Controller> mClasses = new ArrayList<>();

    private ProgressBar mProgressBar;

    private String currentPhotoPath;
    private static final String IMAGE_DIRECTORY = "/Awesome";
    private final int GALLERY = 1, CAMERA = 2;
    private String TAG = "MainActivity";

    public boolean inMethod = false; // set true if you in method
    public boolean imageChanged = false; // check image for changes
    public boolean algorithmExecuted = false; // check algorithm is executed
    public boolean algoInWork = false; // check task in background
    private boolean mPhotoChosen = false; // false if photo is default

    public History history;
    public DrivingViews drivingViews;

    private Settings mSetting;

    private Controller Controller;
    private Controller A_Star;
    private Controller Algem;
    private Controller Rotation;
    private Controller LinearAlgebra;
    private Controller Color_Filters;
    private Controller Retouch;
    private Controller Scaling;
    private Controller Segmentation;
    private Controller Usm;

    public LinearLayout getmPlaceHolder() {
        return mPlaceHolder;
    }

    public LinearLayout getmHeader() {
        return mHeader;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        requestMultiplePermissions();

        mHeader = findViewById(R.id.linear_layout_header);
        mSetting = new Settings();

        final LayoutInflater factory = getLayoutInflater();
        final View menu = factory.inflate(R.layout.main_head, null);
        mHeader.addView(menu, 0);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        mImageView = findViewById(R.id.iv);

        mPlaceHolder = findViewById(R.id.method_layout);
        mRecyclerView = findViewById(R.id.recyclerView);

        mImageView.setMaxHeight((int) (height * 0.585));

        mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
        mBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true); // to make it mutable

        mBitmapDrawing = ((BitmapDrawable) mImageView.getDrawable()).getBitmap().copy(Bitmap.Config.ARGB_8888, true);

        mProgressBar = findViewById(R.id.progressbar_main);
        switchProgressBarVisibilityInvisible();

        drivingViews = new DrivingViews(this);

        history = new History();
        history.clearAllAndSetOriginal(((BitmapDrawable) mImageView.getDrawable()).getBitmap());

        getLayoutInflater().inflate(
                R.layout.apply_menu,
                (LinearLayout) findViewById(R.id.apply_layout));

        initClassesMain();
        updateAccordingSettings();
        getImages();
    }

    // some service methods
    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap btmp) {
        mBitmap = btmp.copy(Bitmap.Config.ARGB_8888, true);
    }

    public void invalidateImageView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mImageView.invalidate();
            }
        });
    }

    public Bitmap getBitmapBefore() {
        return history.showHead().copy(Bitmap.Config.ARGB_8888, true);
    }

    public void resetBitmap() {
        mBitmap = history.showHead().copy(Bitmap.Config.ARGB_8888, true);
    }

    public void resetDrawing() {
        mBitmapDrawing = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    public Bitmap getBitmapDrawing() {
        return mBitmapDrawing;
    }

    public ImageView getImageView() {
        return mImageView;
    }

    public void setBitmapFromImageView() {
        mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
    }

    public void switchProgressBarVisibilityVisible() {
        mProgressBar.setVisibility(View.VISIBLE);
    }

    public void switchProgressBarVisibilityInvisible() {
        mProgressBar.setVisibility(View.GONE);
    }

    public int getPixelBitmap(int x, int y) {
        if (0 > x || x >= mBitmap.getWidth() ||
                0 > y || y >= mBitmap.getHeight()) {
            Log.w(TAG, "Reference to pixel beyond borders");
            return 0;
        }
        return mBitmap.getPixel(x, y);
    }

    public void setPixelBitmap(int x, int y, int color) {
        if (0 > x || x >= mBitmap.getWidth() ||
                0 > y || y >= mBitmap.getHeight()) {
            Log.w(TAG, "Reference to pixel beyond borders");
            return;
        }
        mBitmap.setPixel(x, y, color);
    }

    public void initClasses() {
        if (mClasses.size() == 0) {
            for (int i = 0; i < 9; i++)
                mClasses.add(new Controller(this));
        }
        mClasses.set(0, A_Star);
        mClasses.set(1, Algem);
        mClasses.set(2, Rotation);
        mClasses.set(3, LinearAlgebra);
        mClasses.set(4, Color_Filters);
        mClasses.set(5, Retouch);
        mClasses.set(6, Scaling);
        mClasses.set(7, Segmentation);
        mClasses.set(8, Usm);
        initRecyclerView();
    }

    // called on create
    private void initClassesMain() {
        Controller = new Controller(this);
        A_Star = new A_Star(this);
        Algem = new Algem(this);
        Rotation = new Rotation(this);
        LinearAlgebra = new LinearAlgebra(this);
        Color_Filters = new ColorFilters(this);
        Retouch = new Retouch(this);
        Scaling = new Scaling(this);
        Segmentation = new Segmentation(this);
        Usm = new Usm(this);
    }

    private void initRecyclerView() {
        Log.d("upd", "initRecyclerView: init recyclerview");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        RecyclerViewAdapter adapter = new RecyclerViewAdapter(this,
                mNames, mImageUrls, mClasses);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        if (inMethod) {
            if (algoInWork) return; // алгоритм ещё работает!
            else if (imageChanged) openQuitFromMethodDialog(); // уверен, что выйти из метода
            else {
                Controller.setDefaultState(null); // выход из метода, если изменений не было
            }
        } else openQuitDialog(); // уверен, что выйти из приложения
    }

    // history buttons
    public void redoOnClick(View view) {
        mBitmap = history.takeFromBuffer();
        if (mBitmap == null) { // if stack is empty
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.nothing_to_show), Toast.LENGTH_SHORT).show();
            mBitmap = history.showHead().copy(Bitmap.Config.ARGB_8888, true);
        } else {
            mImageView.setImageBitmap(mBitmap);
        }
    }

    public void undoOnClick(View view) {
        mBitmap = history.popBitmap();
        mImageView.setImageBitmap(mBitmap);
    }

    // quit dialogs
    private void openQuitDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(this);
        quitDialog.setTitle(getResources().getString(R.string.exit_sure));

        quitDialog.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        quitDialog.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        quitDialog.show();
    }

    public void openQuitFromMethodDialog() {
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(this);
        quitDialog.setTitle(getResources().getString(R.string.continue_q));

        quitDialog.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Controller.setDefaultState(null); // выход из метода, если изменений не было
                mBitmap = history.showHead().copy(Bitmap.Config.ARGB_8888, true);
                mImageView.setImageBitmap(mBitmap);
            }
        });

        quitDialog.setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        quitDialog.show();
    }

    private void openBigPictureDialog() {
        AlertDialog.Builder bigPictureDialog = new AlertDialog.Builder(this);
        bigPictureDialog.setTitle(getResources().getString(R.string.big_picture));

        bigPictureDialog.setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mBitmap = ColorFIltersCollection.resizeBicubic(mBitmap,
                        mBitmap.getWidth() / 2, MainActivity.this);
                mImageView.setImageBitmap(mBitmap);
                mPhotoChosen = true;
                history.clearAllAndSetOriginal(mBitmap);
            }
        });

        bigPictureDialog.setNegativeButton(getResources().getString(R.string.choose_another), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        bigPictureDialog.show();
    }

    private void getImages() {
        Log.d("upd", "initImageBitmaps: preparing bitmaps.");
        initClasses();

        mImageUrls.add(R.drawable.icon_a_star); // 0
        mNames.add(getResources().getString(R.string.title_activity_a__star));

        mImageUrls.add(R.drawable.icon_spline); // 1
        mNames.add(getResources().getString(R.string.title_activity_spline));

        mImageUrls.add(R.drawable.icon_rotate); // 2
        mNames.add(getResources().getString(R.string.title_activity_rotation));

        mImageUrls.add(R.drawable.icon_billinear_filter); // 3
        mNames.add(getResources().getString(R.string.title_activity_bi__filters));

        mImageUrls.add(R.drawable.icon_color_filters); // 4
        mNames.add(getResources().getString(R.string.title_activity_color__filters));

        mImageUrls.add(R.drawable.icon_retouch); // 5
        mNames.add(getResources().getString(R.string.title_activity_retouch));

        mImageUrls.add(R.drawable.icon_scale); // 6
        mNames.add(getResources().getString(R.string.title_activity_scaling));

        mImageUrls.add(R.drawable.icon_segmentation); // 7
        mNames.add(getResources().getString(R.string.title_activity_segmentation));

        mImageUrls.add(R.drawable.icon_sharpness); // 8
        mNames.add(getResources().getString(R.string.title_activity_usm));

        initRecyclerView();
    }

    // upload and save image methods
    public void choosePhotoFromGallery(View view) {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    private File createImageFile() {
        // Create an image file name
        File image;

        String imageFileName = "JPEG_" + "_AWESOME";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            return null;
        }
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void takePhotoFromCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                FileProvider.getUriForFile(this,
                        "com.example.image_editor.fileprovider", createImageFile()));
        startActivityForResult(intent, CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.failed),
                            Toast.LENGTH_SHORT).show();
                }
                if (mBitmap.getByteCount() > 10000000) {
                    openBigPictureDialog();
                    return;
                }
            }

        } else if (requestCode == CAMERA && resultCode == RESULT_OK) {
            mBitmap = BitmapFactory.decodeFile(currentPhotoPath);
        }

        mBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
        mImageView.setImageBitmap(mBitmap);
        mPhotoChosen = true;
        history.clearAllAndSetOriginal(mBitmap);
        Controller.setDefaultState(null);
    }

    public void shareImage(View view) {
        Bitmap icon = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/jpeg");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = new File(Environment.getExternalStorageDirectory() + File.separator + "temporary_file.jpg");
        try {
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        // hardcoded path
        share.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///sdcard/temporary_file.jpg"));
        startActivity(Intent.createChooser(share, getResources().getString(R.string.share_image)));
    }

    public void saveImage(View view) {
        mBitmap = ((BitmapDrawable) mImageView.getDrawable()).getBitmap();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("upd", "File Saved::--->" + f.getAbsolutePath());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.image_saved_in) + IMAGE_DIRECTORY, Toast.LENGTH_SHORT).show();
    }

    private void requestMultiplePermissions() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            Log.i("upd", "All permissions are granted by user!");
                        }
                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            Log.i("upd", "problem with permission");
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(),
                                getResources().getString(R.string.some_error),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }

    // Settings part
    class Settings {
        int language;
        boolean theme;

        Settings() {
            language = R.id.rb_eng;
            theme = false;
        }
    }

    private Dialog openSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.main_settings, null))
                .setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final AlertDialog alertDialog = (AlertDialog) dialog;

                        RadioGroup rg_lang = alertDialog.findViewById(R.id.rg_language);
                        mSetting.language = rg_lang.getCheckedRadioButtonId();

                        Switch switch_theme = alertDialog.findViewById(R.id.switch_theme);
                        mSetting.theme = switch_theme.isChecked();

                        updateAccordingSettings();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // nothing to say, just exit
                    }
                });
        return builder.create();
    }

    void updateAccordingSettings() {
        Locale locale;
        if (mSetting.language == R.id.rb_eng) {
            locale = new Locale("en");
        } else locale = new Locale("ru");

        View v = findViewById(R.id.main_constraint_layout);
        if (mSetting.theme) {
            v.setBackgroundColor(getResources().getColor(R.color.colorBackground3));
        } else v.setBackgroundColor(getResources().getColor(R.color.colorBackground2));

        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getBaseContext().getResources().updateConfiguration(configuration, null);
    }

    public void mainSettingOnClick(View view) {
        Dialog dialog = openSettingsDialog();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                final AlertDialog alertDialog = (AlertDialog) dialog;

                RadioGroup rg_lang = alertDialog.findViewById(R.id.rg_language);
                rg_lang.check(mSetting.language);

                Switch switch_theme = alertDialog.findViewById(R.id.switch_theme);
                switch_theme.setChecked(mSetting.theme);
            }
        });
        dialog.show();
    }
}