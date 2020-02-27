package soup.tile.screenrecord;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import soup.tile.screenrecord.storage.MediaStorage;
import timber.log.Timber;

import static soup.tile.screenrecord.notification.NotificationInfo.CHANNEL_ID;
import static soup.tile.screenrecord.notification.NotificationInfo.NOTIFICATION_ID;

public class RecordingService extends Service {
    private static final String EXTRA_RESULT_CODE = "extra_resultCode";
    private static final String EXTRA_DATA = "extra_data";
    private static final String EXTRA_PATH = "extra_path";
    private static final String EXTRA_USE_AUDIO = "extra_useAudio";
    private static final int REQUEST_CODE = 2;

    private static final String ACTION_START = "soup.tile.screenrecord.START";
    private static final String ACTION_STOP = "soup.tile.screenrecord.STOP";
    private static final String ACTION_PAUSE = "soup.tile.screenrecord.PAUSE";
    private static final String ACTION_RESUME = "soup.tile.screenrecord.RESUME";
    private static final String ACTION_CANCEL = "soup.tile.screenrecord.CANCEL";
    private static final String ACTION_SHARE = "soup.tile.screenrecord.SHARE";
    private static final String ACTION_DELETE = "soup.tile.screenrecord.DELETE";

    private static final int TOTAL_NUM_TRACKS = 1;
    private static final int VIDEO_BIT_RATE = 25000000; //6000000;
    private static final int VIDEO_FRAME_RATE = 60; //30;
    private static final int AUDIO_BIT_RATE = 16;
    private static final int AUDIO_SAMPLE_RATE = 44100;

    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private Surface mInputSurface;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private NotificationCompat.Builder mRecordingNotificationBuilder;

    private boolean mUseAudio;
    private File mTempFile;

    /**
     * Get an intent to start the recording service.
     *
     * @param context    Context from the requesting activity
     * @param resultCode The result code from {@link Activity#onActivityResult(int, int, Intent)}
     * @param data       The data from {@link Activity#onActivityResult(int, int, Intent)}
     * @param useAudio   True to enable microphone input while recording
     */
    public static Intent getStartIntent(Context context, int resultCode, Intent data, boolean useAudio) {
        return new Intent(context, RecordingService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_DATA, data)
                .putExtra(EXTRA_USE_AUDIO, useAudio);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_NOT_STICKY;
        }
        String action = intent.getAction();
        Timber.d("onStartCommand " + action);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        switch (action) {
            case ACTION_START:
                int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED);
                mUseAudio = intent.getBooleanExtra(EXTRA_USE_AUDIO, false);
                Intent data = intent.getParcelableExtra(EXTRA_DATA);
                if (data != null) {
                    mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
                    startRecording();
                }
                break;

            case ACTION_CANCEL:
                stopRecording();

                // Delete temp file
                if (!mTempFile.delete()) {
                    Timber.e("Error canceling screen recording!");
                    Toast.makeText(this, R.string.screenrecord_delete_error, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, R.string.screenrecord_cancel_success, Toast.LENGTH_LONG).show();
                }

                // Close quick shade
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                break;

            case ACTION_STOP:
                stopRecording();
                saveRecording(notificationManager);
                break;

            case ACTION_PAUSE:
                mMediaRecorder.pause();
                setNotificationActions(true, notificationManager);
                break;

            case ACTION_RESUME:
                mMediaRecorder.resume();
                setNotificationActions(false, notificationManager);
                break;

            case ACTION_SHARE:
                Uri shareUri = Uri.parse(intent.getStringExtra(EXTRA_PATH));
                Intent shareIntent = new Intent(Intent.ACTION_SEND)
                        .setType("video/mp4")
                        .putExtra(Intent.EXTRA_STREAM, shareUri);
                String shareLabel = getResources().getString(R.string.screenrecord_share_label);

                // Close quick shade
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));

                // Remove notification
                notificationManager.cancel(NOTIFICATION_ID);

                startActivity(Intent.createChooser(shareIntent, shareLabel)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case ACTION_DELETE:
                // Close quick shade
                sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                ContentResolver resolver = getContentResolver();
                Uri uri = Uri.parse(intent.getStringExtra(EXTRA_PATH));
                resolver.delete(uri, null, null);

                Toast.makeText(this, R.string.screenrecord_delete_description, Toast.LENGTH_LONG).show();

                // Remove notification
                notificationManager.cancel(NOTIFICATION_ID);
                Timber.d("Deleted recording " + uri);
                break;
        }
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    /**
     * Begin the recording session
     */
    private void startRecording() {
        try {
            mTempFile = File.createTempFile("temp", ".mp4");
            Timber.d("Writing video output to: " + mTempFile.getAbsolutePath());

            // Set up media recorder
            mMediaRecorder = new MediaRecorder();
            if (mUseAudio) {
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            // Set up video
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            Size screenSize = getScreenSize(this);
            int screenWidth = screenSize.getWidth();
            int screenHeight = screenSize.getHeight();
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setVideoSize(screenWidth, screenHeight);
            mMediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
            mMediaRecorder.setVideoEncodingBitRate(VIDEO_BIT_RATE);

            // Set up audio
            if (mUseAudio) {
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mMediaRecorder.setAudioChannels(TOTAL_NUM_TRACKS);
                mMediaRecorder.setAudioEncodingBitRate(AUDIO_BIT_RATE);
                mMediaRecorder.setAudioSamplingRate(AUDIO_SAMPLE_RATE);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mMediaRecorder.setOutputFile(mTempFile);
            } else {
                mMediaRecorder.setOutputFile(mTempFile.getAbsolutePath());
            }
            mMediaRecorder.prepare();

            // Create surface
            mInputSurface = mMediaRecorder.getSurface();
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                    "Recording Display",
                    screenWidth,
                    screenHeight,
                    metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mInputSurface,
                    null,
                    null);

            mMediaRecorder.start();

            RecordingStateManager.setRecording(true);
        } catch (IOException e) {
            RecordingStateManager.setRecording(false);
            Timber.e("Error starting screen recording: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        createRecordingNotification();
    }

    private void createRecordingNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mRecordingNotificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_noti_video)
                .setContentTitle(getResources().getString(R.string.screenrecord_name))
                .setUsesChronometer(true)
                .setOngoing(true);
        setNotificationActions(false, notificationManager);
        Notification notification = mRecordingNotificationBuilder.build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void setNotificationActions(boolean isPaused, NotificationManager notificationManager) {
        mRecordingNotificationBuilder.addAction(
                new NotificationCompat.Action.Builder(
                        IconCompat.createWithResource(this, R.drawable.ic_android),
                        getResources().getString(R.string.screenrecord_stop_label),
                        PendingIntent
                                .getService(this, REQUEST_CODE, getStopIntent(this),
                                        PendingIntent.FLAG_UPDATE_CURRENT))
                        .build());
        mRecordingNotificationBuilder.addAction(
                new NotificationCompat.Action.Builder(
                        IconCompat.createWithResource(this, R.drawable.ic_android),
                        getResources().getString(R.string.screenrecord_cancel_label),
                        PendingIntent
                                .getService(this, REQUEST_CODE, getCancelIntent(this),
                                        PendingIntent.FLAG_UPDATE_CURRENT))
                        .build());
        notificationManager.notify(NOTIFICATION_ID, mRecordingNotificationBuilder.build());
    }

    private Notification createSaveNotification(Uri uri) {
        Intent viewIntent = new Intent(Intent.ACTION_VIEW)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .setDataAndType(uri, "video/mp4");

        NotificationCompat.Action shareAction = new NotificationCompat.Action.Builder(
                IconCompat.createWithResource(this, R.drawable.ic_android),
                getResources().getString(R.string.screenrecord_share_label),
                PendingIntent.getService(
                        this,
                        REQUEST_CODE,
                        getShareIntent(this, uri.toString()),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .build();

        NotificationCompat.Action deleteAction = new NotificationCompat.Action.Builder(
                IconCompat.createWithResource(this, R.drawable.ic_android),
                getResources().getString(R.string.screenrecord_delete_label),
                PendingIntent.getService(
                        this,
                        REQUEST_CODE,
                        getDeleteIntent(this, uri.toString()),
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .build();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_noti_video)
                .setContentTitle(getResources().getString(R.string.screenrecord_name))
                .setContentText(getResources().getString(R.string.screenrecord_save_message))
                .setContentIntent(PendingIntent.getActivity(
                        this,
                        REQUEST_CODE,
                        viewIntent,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION))
                .addAction(shareAction)
                .addAction(deleteAction)
                .setAutoCancel(true);
/* FIXME:
        // Add thumbnail if available
        Bitmap thumbnailBitmap = null;
        try {
            ContentResolver resolver = getContentResolver();
            Size size = new Size(512, 384);
            thumbnailBitmap = resolver.loadThumbnail(uri, size, null);
        } catch (IOException e) {
            Timber.e("Error creating thumbnail: " + e.getMessage());
            e.printStackTrace();
        }
        if (thumbnailBitmap != null) {
            Notification.BigPictureStyle pictureStyle = new Notification.BigPictureStyle()
                    .bigPicture(thumbnailBitmap)
                    .bigLargeIcon((Bitmap) null);
            builder.setLargeIcon(thumbnailBitmap).setStyle(pictureStyle);
        }
 */
        return builder.build();
    }

    private void stopRecording() {
        RecordingStateManager.setRecording(false);

        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
        mMediaProjection.stop();
        mMediaProjection = null;
        mInputSurface.release();
        mVirtualDisplay.release();
        stopSelf();
    }

    private void saveRecording(NotificationManager notificationManager) {
        String fileName = new SimpleDateFormat("'screen-'yyyyMMdd-HHmmss'.mp4'")
                .format(new Date());

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());

        ContentResolver resolver = getContentResolver();
        Uri itemUri = new MediaStorage(this).insertVideo();
        try {
            // Add to the mediastore
            OutputStream os = resolver.openOutputStream(itemUri, "w");
            Files.copy(mTempFile.toPath(), os);
            os.close();

            Notification notification = createSaveNotification(itemUri);
            notificationManager.notify(NOTIFICATION_ID, notification);

            mTempFile.delete();
        } catch (IOException e) {
            Timber.e("Error saving screen recording: " + e.getMessage());
            Toast.makeText(this, R.string.screenrecord_delete_error, Toast.LENGTH_LONG).show();
        }
    }

    public static Intent getStopIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction(ACTION_STOP);
    }

    private static Intent getCancelIntent(Context context) {
        return new Intent(context, RecordingService.class).setAction(ACTION_CANCEL);
    }

    private static Intent getShareIntent(Context context, String path) {
        return new Intent(context, RecordingService.class).setAction(ACTION_SHARE)
                .putExtra(EXTRA_PATH, path);
    }
    private static Intent getDeleteIntent(Context context, String path) {
        return new Intent(context, RecordingService.class).setAction(ACTION_DELETE)
                .putExtra(EXTRA_PATH, path);
    }

    private static Size getScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point screenSize = new Point();
        windowManager.getDefaultDisplay().getRealSize(screenSize);
        return new Size(screenSize.x, screenSize.y);
    }
}
