使用方法：
// MyApplication.java
import android.app.Application;
import android.util.Log;

import java.util.List;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // 安装布局膨胀拦截器
        LayoutInflateInterceptor.install(this);

        // 注册Activity生命周期回调，可以在Activity销毁时打印或清除数据
        // 或者在特定按钮点击时触发检测报告
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(android.app.Activity activity) { }
            @Override
            public void onActivityResumed(android.app.Activity activity) {
                // Activity 每次 resume 时可以打印当前已检测到的布局
                Log.d(TAG, "--- Detected Delayed Loadable Layouts for " + activity.getClass().getSimpleName() + " ---");
                List<DelayedLoadableLayoutInfo> detected = LayoutDetector.getDetectedLayouts();
                if (detected.isEmpty()) {
                    Log.d(TAG, "No delayed loadable layouts detected yet.");
                } else {
                    for (DelayedLoadableLayoutInfo info : detected) {
                        Log.d(TAG, info.toString());
                    }
                }
                // 如果每次Activity显示都清空，则只看当前Activity的加载情况
                // LayoutDetector.clearDetectedLayouts();
            }
            @Override
            public void onActivityPaused(android.app.Activity activity) { }
            @Override
            public void onActivityStopped(android.app.Activity activity) { }
            @Override
            public void onActivitySaveInstanceState(android.app.Activity activity, android.os.Bundle outState) { }
            @Override
            public void onActivityDestroyed(android.app.Activity activity) {
                // Activity 销毁时，可以考虑清除相关检测数据，或者保存到报告中
                // 比如在一个调试工具中，你可以收集所有Activity的检测数据
            }
            @Override
            public void onActivityCreated(android.app.Activity activity, android.os.Bundle savedInstanceState) {
                // 注意：在 Activity.onCreate() 中设置 Factory 是比较晚的，因为 Activity 自身的布局可能已经膨胀。
                // 最好的地方是在 Application.onCreate() 中设置，或者在 Activity.attachBaseContext() 中设置。
                // 这里我们已经在 Application 中设置了，所以 Activity 自身的 inflate 也会被拦截。
            }
        });
    }
}
