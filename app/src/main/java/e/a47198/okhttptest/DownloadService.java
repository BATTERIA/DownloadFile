package e.a47198.okhttptest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

public class DownloadService extends Service {
    private DownloadTask downLoadTask;
    private String downloadUrl;


    private DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1, getNotification("Downloading...", progress));
        }

        @Override
        public void onSuccess() {
            downLoadTask = null;
//            下载成功时将前台服务通知关闭，并创建一个下载成功的通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download Success", -1));
            Toast.makeText(DownloadService.this, "Download Success", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onFailed() {
            downLoadTask = null;
//            下载失败时通知将前台服务关闭，并创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download Fail", -1));

            Toast.makeText(DownloadService.this, "Download Fail", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onPaused() {
            downLoadTask = null;
            Toast.makeText(DownloadService.this, "Download Paused", Toast.LENGTH_SHORT).show();


        }

        @Override
        public void onCanceled() {
            downLoadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "Download Cancle", Toast.LENGTH_SHORT).show();


        }
    };


    private DownloadBinder binder = new DownloadBinder();
    /**
     *
     */
    class DownloadBinder extends Binder {
        public void startDownload(String url) {
            if (downLoadTask == null) {
                downloadUrl = url;
                downLoadTask = new DownloadTask(downloadListener);
//                在这里开启下载
                downLoadTask.execute(downloadUrl);
//                让这个服务成为一个前台服务。这样就可以在系统状态栏中创建一个持续运行的通知了
                startForeground(1, getNotification("Downloading...", 0));
                Toast.makeText(DownloadService.this, "Downing...", Toast.LENGTH_SHORT);
            }
        }


        public void pauseDownload() {
            if (downLoadTask != null) {
                downLoadTask.pauseDownload();
            }
        }


        public void cancelDownload() {
            if (downLoadTask != null) {
                downLoadTask.cancleDownload();
            } else {
                if (downloadUrl != null) {
//                    取消下载时需将文件删除，并将通知关闭
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    Toast.makeText(DownloadService.this, "Cancled", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    /**
     * 用于显示下载进度的通知
     * @param title
     * @param progress
     * @return
     */

    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentTitle(title);
        builder.setContentIntent(pendingIntent);
        if (progress > 0) {
//            当progress大于或等于0时才需要显示下载进度
            builder.setContentText(progress + "%");
//           参数1:通知的做大进度。 2.通知的当前进度。 3.是否使用模糊进度条
            builder.setProgress(100, progress, false);
        }

        return builder.build();
    }


    public DownloadService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return binder;
    }
}
