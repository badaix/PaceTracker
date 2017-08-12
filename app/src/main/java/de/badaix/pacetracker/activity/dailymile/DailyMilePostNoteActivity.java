package de.badaix.pacetracker.activity.dailymile;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.social.dailymile.DailyMile;
import de.badaix.pacetracker.social.dailymile.Location;
import de.badaix.pacetracker.social.dailymile.PersonEntry;
import de.badaix.pacetracker.social.dailymile.Position;
import de.badaix.pacetracker.social.dailymile.PostEntry;
import de.badaix.pacetracker.social.dailymile.User;
import de.badaix.pacetracker.util.FileUtils;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;
import de.badaix.pacetracker.views.dailymile.DailyMileEntry;
import de.badaix.pacetracker.views.dailymile.DailyMileNote;

public class DailyMilePostNoteActivity extends AppCompatActivity implements OnClickListener,
        android.content.DialogInterface.OnClickListener, MenuItem.OnMenuItemClickListener {
    private static final int SELECT_PHOTO = 100;
    private static final int ACTION_TAKE_PHOTO_B = 1;
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String CAMERA_DIR = "/dcim/";
    // private ClickAction photoAction;
    private MenuItem photoItem;
    private DailyMileNote note;
    private Button btnSend;
    private ScrollView scrollView;
    private AlertDialog alert;
    private String mCurrentPhotoPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (GlobalSettings.getInstance().getContext() == null)
            this.finish();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dailymile_post_note);

        btnSend = (Button) findViewById(R.id.btnPostNote);
        btnSend.setOnClickListener(this);

        note = new DailyMileNote(this.getApplicationContext());
        note.getImageView().setOnClickListener(this);

        DailyMile dm = new DailyMile(this.getApplicationContext());
        PersonEntry person = null;
        User user = dm.getMe();
        person = new PersonEntry(user);
        person.setAt(new Date());
        if (user != null)
            person.setLocation(new Location(user.getLocation()));
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        DailyMileEntry item = new DailyMileEntry(getApplicationContext(), person, false);
        item.linearLayout.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        item.addChildView(note);
        scrollView.addView(item);

        // actionBar = (ActionBar) findViewById(R.id.actionbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.postNote));
        // actionBar.setDisplayHomeAsUpEnabled(false);
        // photoAction = new ClickAction(this,
        // R.drawable.ic_action_device_access_camera);
        // if
        // (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        // actionBar.addAction(photoAction);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
            return true;

        photoItem = menu.add(getString(R.string.photo));
        photoItem.setOnMenuItemClickListener(this).setIcon(R.drawable.ic_action_device_access_camera)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        return true;
    }

    private File getAlbumStorageDir(String albumName) {
        return new File(Environment.getExternalStorageDirectory() + CAMERA_DIR + albumName);
    }

    /* Photo album for this application */
    private String getAlbumName() {
        return getString(R.string.album_name);
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = getAlbumStorageDir(getAlbumName());

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d("CameraSample", "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File setUpPhotoFile() throws IOException {

        File f = createImageFile();
        mCurrentPhotoPath = f.getAbsolutePath();

        return f;
    }

    private void setPic(Uri pic) throws FileNotFoundException {
        /* There isn't enough memory to open up more than a couple camera photos */
		/* So pre-scale the target bitmap into which the file is decoded */

		/* Get the size of the ImageView */
        int targetW = 520;// actionBar.getWidth();// imageView.getWidth();
        int targetH = 520;// note.getImageView().getHeight();

		/* Get the size of the image */
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(pic), null, bmOptions);
        // BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
        int scaleFactor = 1;
        if ((targetW > 0) || (targetH > 0)) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        }

		/* Set bitmap options to scale the image decode target */
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(pic), null, bmOptions);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, FileUtils.openCacheOutput(this, "notes", "image.jpg"));
		/* Associate the Bitmap to the ImageView */
        note.setBitmap(bitmap);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.parse("file://"
                + mCurrentPhotoPath));
        this.sendBroadcast(mediaScanIntent);
    }

    @SuppressWarnings("unused")
    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = this.getWindowManager().getDefaultDisplay().getWidth();

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth;// , height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE)
                break;
            // || height_tmp / 2 < REQUIRED_SIZE) {
            // break;
            // }
            width_tmp /= 2;
            // height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case ACTION_TAKE_PHOTO_B: {
                if (resultCode == RESULT_OK) {
                    handleBigCameraPhoto();
                }
                break;
            } // ACTION_TAKE_PHOTO_B

            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        // mCurrentPhotoPath = new File(new
                        // URI(imageReturnedIntent.getData().toString())).getAbsolutePath();
                        setPic(imageReturnedIntent.getData());
                        mCurrentPhotoPath = null;
                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        Hint.show(this, e);
                    }
                    // Uri selectedImage = imageReturnedIntent.getData();
                    // // InputStream imageStream;
                    // try {
                    // imageView.setImageBitmap(decodeUri(selectedImage));
                    // imageView.setVisibility(View.VISIBLE);
                    // // imageStream =
                    // getContentResolver().openInputStream(selectedImage);
                    // // Bitmap yourSelectedImage =
                    // BitmapFactory.decodeStream(imageStream);
                    // } catch (FileNotFoundException e) {
                    // e.printStackTrace();
                    // Hint.show(this, e);
                    // }
                }
        }
    }

    // @Override
    // public void onActionClicked(Action action) {
    // if (action == photoAction) {
    // dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
    // }
    // }

    private void dispatchTakePictureIntent(int actionCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        switch (actionCode) {
            case ACTION_TAKE_PHOTO_B:
                File f = null;

                try {
                    f = setUpPhotoFile();
                    mCurrentPhotoPath = f.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                } catch (IOException e) {
                    e.printStackTrace();
                    f = null;
                    mCurrentPhotoPath = null;
                }
                break;

            default:
                break;
        } // switch

        startActivityForResult(takePictureIntent, actionCode);
    }

    private void handleBigCameraPhoto() {
        if (mCurrentPhotoPath != null) {
            try {
                setPic(Uri.parse("file://" + mCurrentPhotoPath));
                galleryAddPic();
                mCurrentPhotoPath = null;
            } catch (FileNotFoundException e) {
                Hint.show(this, e);
                e.printStackTrace();
            }
        }
    }

    // 09-05 09:53:09.787: D/DailyMilePostNoteActivity(424):
    // {"id":17763079,"url":"http://www.dailymile.com/entries/17763079","at":"2012-09-05T07:53:10Z","message":"my shoes...","comments":[],"likes":[],"location":{"name":"Aachen, DE"},"user":{"username":"JohannesP","display_name":"Johannes P.","photo_url":"http://s1.dmimg.com/pictures/users/321959/1340955012_avatar.jpg","url":"http://www.dailymile.com/people/JohannesP"},"media":[{"preview":{"type":"image","height":75,"width":75,"url":"http://www.dailymile.com/images/photo_previews/502472/b61d515e2af28b67f9fd9764d9aa8969_preview.jpg"},"content":{"type":"image","url":"http://www.dailymile.com/images/photos/502472/b61d515e2af28b67f9fd9764d9aa8969.jpg","height":311,"width":520}}]}

    @Override
    public void onClick(View view) {
        if (view == note.getImageView()) {
            if (note.hasImage()) {
                final CharSequence[] items = {getResources().getString(R.string.replaceImage),
                        getResources().getString(R.string.deleteImage)};
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.image)).setItems(items, this);
                alert = builder.create();
                alert.show();
            } else {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        } else if (view == btnSend) {

            class PostTask extends AsyncTask<DailyMileNote, Void, String> {
                private ProgressDialog dialog = null;
                private Exception e = null;

                public PostTask(ProgressDialog dialog) {
                    this.dialog = dialog;
                }

                @Override
                protected String doInBackground(DailyMileNote... item) {
                    DailyMileNote note = item[0];

                    String result = "";
                    try {
///TODO!!!
						DailyMile dailyMile = new DailyMile(note.getContext());
						if (note.hasImage()) {
							File imageFile = new File(FileUtils.getCacheStreamPath(note.getContext(), "notes"),
									"image.jpg");
							result = dailyMile.addNoteWithImage(note.getNote(), imageFile,
									LocationUtils.getLastKnownLocation());
						} else {
							android.location.Location location = LocationUtils.getLastKnownLocation();
							Position pos = null;
							if (location != null)
								pos = new Position(location);
							result = dailyMile.postSession(new PostEntry(note.getNote(), pos, null));
						}
                        // dailyMile.postSession(new
                    } catch (Exception e) {
                        this.e = e;
                    }

                    return result;
                }


                @Override
                protected void onPostExecute(String item) {
                    if (dialog != null)
                        dialog.dismiss();
                    if (e != null)
                        Hint.show(note.getContext(), e);
                    else
                        finish();
                }
            }

            ProgressDialog dialog = ProgressDialog.show(this, "", getResources().getString(R.string.postingNote), true);
            dialog.show();
            PostTask postTask = new PostTask(dialog);
            postTask.execute(note);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int item) {
        if (dialog == alert) {
            if (item == 1) {
                note.setBitmap(null);
            } else if (item == 0) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            }
        }
        // TODO Auto-generated method stub

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item == photoItem) {
            dispatchTakePictureIntent(ACTION_TAKE_PHOTO_B);
        }
        return true;
    }

}
