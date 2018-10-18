package e.a47198.okhttptest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class DownloadService extends Service {
    private ArrayList<DownloadTask> downLoadTasks = new ArrayList<>();
    private String downloadUrl;
    private int THREAD_NUMBER = 3;
    private SharedHelper sh = new SharedHelper(ContextUtil.getContext());

    private int successedNumber = 0;
    private int failedNumber = 0;
    private int pausedNumber = 0;
    private int canceledNumber = 0;

    private DownloadListener downloadListener = new DownloadListener(){
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1, getNotification("Downloading...", progress));
        }

        @Override
        public void onSuccess() {
            synchronized (DownloadListener.class){
                ++successedNumber;
            }
            if(successedNumber==THREAD_NUMBER){
                sh.clear();
                downLoadTasks.clear();
                stopForeground(true);
                getNotificationManager().notify(1, getNotification("Download Success", -1));
                Toast.makeText(DownloadService.this, "Download Success", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailed() {
            synchronized (DownloadListener.class){
                ++failedNumber;
            }
            if(failedNumber==THREAD_NUMBER) {
                downLoadTasks.clear();
                stopForeground(true);
                getNotificationManager().notify(1, getNotification("Download Fail", -1));
                Toast.makeText(DownloadService.this, "Download Fail", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPaused() {
            synchronized (DownloadListener.class){
                ++pausedNumber;
            }
            if(pausedNumber==THREAD_NUMBER) {
                downLoadTasks.clear();
                Toast.makeText(DownloadService.this, "Download Paused", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onCanceled() {
            synchronized (DownloadListener.class){
                ++canceledNumber;
            }
            if(canceledNumber==THREAD_NUMBER) {
                downLoadTasks.clear();
                stopForeground(true);
                Toast.makeText(DownloadService.this, "Download Cancel", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private DownloadBinder binder = new DownloadBinder();

    class DownloadBinder extends Binder {
        public void startDownload(String url) {
            if(downLoadTasks.isEmpty()){
                for(int i=0;i<THREAD_NUMBER;++i){
                    downloadUrl = url;
                    DownloadTask downLoadTask = new DownloadTask(downloadListener);
                    String params[] = new String[THREAD_NUMBER];
                    params[0] = downloadUrl;
                    params[1] = String.valueOf(THREAD_NUMBER);
                    params[2] = String.valueOf(i);
                    downLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
                    downLoadTasks.add(downLoadTask);
                }
                startForeground(1, getNotification("Downloading...", 0));
                Toast.makeText(DownloadService.this, "Downloading...", Toast.LENGTH_SHORT);
            }
        }
        public void pauseDownload() {
            if(!downLoadTasks.isEmpty()) {
                for (int i=0; i < THREAD_NUMBER; ++i) {
                    downLoadTasks.get(i).pauseDownload();
                }
            }
        }
        public void cancelDownload() {
            if(!downLoadTasks.isEmpty()) {
                for (int i=0; i < THREAD_NUMBER; ++i) {
                    downLoadTasks.get(i).cancelDownload();
                }
            }

            if (downloadUrl != null) {
                String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                File file = new File(directory + fileName);
                if (file.exists()) {
                    file.delete();
                }
                getNotificationManager().cancel(1);
                stopForeground(true);
                Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, int progress) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"default");
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
        return binder;
    }
}
