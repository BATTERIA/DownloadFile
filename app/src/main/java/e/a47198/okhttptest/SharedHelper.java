package e.a47198.okhttptest;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SharedHelper {
    private  static  final String TAG = "SharedHelper";
    Context mContext;
    public SharedHelper() {
    }
    public SharedHelper(Context mContext) {
        this.mContext = mContext;
    }

    public void clear(){
        SharedPreferences sp = mContext.getSharedPreferences("DownloadRecord", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();
    }

    //定义一个保存数据的方法
    public void save(String key, long value) {
        SharedPreferences sp = mContext.getSharedPreferences("DownloadRecord", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    //修改
    public void revise(String key, long value){
        save(key, value);
    }

    //定义一个读取SP文件的方法
    public long read(String key) {
        SharedPreferences sp = mContext.getSharedPreferences("DownloadRecord", Context.MODE_PRIVATE);
        if (!sp.contains(key))
            return 0;
        else
            return sp.getLong(key, 0);
    }

    public boolean isExist(){
        SharedPreferences sp = mContext.getSharedPreferences("DownloadRecord", Context.MODE_PRIVATE);
        if (sp.contains("0"))
            return true;
        else
            return false;
    }
}
