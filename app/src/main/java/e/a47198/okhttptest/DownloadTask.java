package e.a47198.okhttptest;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {
    private static final String TAG = "DownloadTask";
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener listener;

    private boolean isCanceled = false;
    private boolean isPaused = false;
    private int lastProgress;
    private SharedHelper sh;

    public DownloadTask(DownloadListener listener){
        this.listener = listener;
        sh = new SharedHelper(ContextUtil.getContext());
    }

    @Override
    protected Integer doInBackground(String... params) {

        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;

        try{
            long downloadedLength = 0;
            String downloadUrl = params[0];
            int threadNum = Integer.valueOf(params[1]);
            int threadNo = Integer.valueOf(params[2]);

            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

            file = new File(directory+fileName);

            long contentLength=getContentLength(downloadUrl);
            if(contentLength==0)
                return TYPE_FAILED;

            long start=0,end=0;
            end = (contentLength/threadNum) * (threadNo+1) - 1;
            if(threadNum == threadNo+1)
                end = contentLength-1;
            start = (contentLength/threadNum) * threadNo;
            if(file.exists()){
                downloadedLength=file.length();
                if(contentLength==downloadedLength)
                    return TYPE_SUCCESS;
                if(sh.isExist()){
                    //计算断点续传
                    start = sh.read(params[2]);
                    Log.d(TAG, "doInBackground: ?????? "+start);
                }
            }
            Log.d(TAG, "doInBackground: start: " + start + "  end: " + end);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(downloadUrl)
                    //此处从断点处开始下载，第几个字节，这是在http1.1之后支持的断点续传功能（也支持并行下载）
                    .addHeader("RANGE","bytes="+start+"-"+end)
                    .build();
            Response response = client.newCall(request).execute();
            if(response!=null){
                is=response.body().byteStream();
                savedFile=new RandomAccessFile(file,"rw");
                savedFile.seek(start);
                byte[] b=new byte[512];
                int total=0;
                int len;
                int sleep = 0;
                while((len=is.read(b))!=-1 && total <= end-start+1){
                    if (isCanceled) {
                        sh.clear();
                        is.close();
                        savedFile.close();
                        return TYPE_CANCELED;
                    } else if (isPaused) {
                        Log.d("Download", "下载暂停: "+Thread.currentThread().getId());
                        sh.revise(params[2], start+total);
                        is.close();
                        savedFile.close();
                        Log.d(TAG, "doInBackground: " + sh.read(params[2]));
                        return TYPE_PAUSED;
                    } else {
                        total += len;
                        savedFile.write(b, 0, len);
                        //
                        ++sleep;
                        if(sleep == 100){
                            sleep = 0;
                            int progress = (int) (file.length() * 100 / contentLength);
                            publishProgress(progress);
                        }
                    }
                }
            }
            response.body().close();
            return TYPE_SUCCESS;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            try {
                if(is!=null)
                    is.close();
                if(savedFile!=null)
                    savedFile.close();
                if(isCanceled&&file!=null)
                    file.delete();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];
        if(progress >= lastProgress){
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status)
        {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            default:
                break;
        }
    }

    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }

    public void pauseDownload(){
        isPaused =true;
    }

    public void cancelDownload(){
        isCanceled =true;
    }
}
