package com.watson.detector;// LayoutInflateInterceptor.java
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;

public class LayoutInflateInterceptor implements LayoutInflater.Factory2 {

    private static final String TAG = "LayoutInflateInterceptor";
    private final LayoutInflater.Factory2 mOriginalFactory2;
    private final LayoutInflater.Factory mOriginalFactory;
    private final Context mContext;

    public LayoutInflateInterceptor(Context context, LayoutInflater inflater) {
        this.mContext = context;

        // 获取原始的Factory2或Factory
        LayoutInflater.Factory2 factory2 = null;
        LayoutInflater.Factory factory = null;
        try {
            Field f = LayoutInflater.class.getDeclaredField("mFactory2");
            f.setAccessible(true);
            factory2 = (LayoutInflater.Factory2) f.get(inflater);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Could not get mFactory2 field: " + e.getMessage());
        }

        if (factory2 == null) {
            try {
                Field f = LayoutInflater.class.getDeclaredField("mFactory");
                f.setAccessible(true);
                factory = (LayoutInflater.Factory) f.get(inflater);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                Log.e(TAG, "Could not get mFactory field: " + e.getMessage());
            }
        }

        mOriginalFactory2 = factory2;
        mOriginalFactory = factory;
    }

    @Nullable
    @Override
    public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        View view = null;

        // 尝试通过原始Factory2来创建View
        if (mOriginalFactory2 != null) {
            view = mOriginalFactory2.onCreateView(parent, name, context, attrs);
        }

        // 如果原始Factory2没有创建成功，尝试通过原始Factory创建
        if (view == null && mOriginalFactory != null) {
            view = mOriginalFactory.onCreateView(name, context, attrs);
        }

        // 如果原始Factory都没有创建成功，尝试通过系统默认行为创建
        // LayoutInflater的内部实现会有一个私有的Factory来处理系统的View，
        // 这里为了简化，我们假设原始Factory能处理大部分情况。
        // 更健壮的实现可能需要反射调用LayoutInflater的private createViewFromTag方法。
        // 但对于标准View，通常会通过LayoutInflater的cloneInContext或setFactory/setFactory2来工作。

        // 实际的View创建逻辑，如果原始Factory没有处理，LayoutInflater会回退到默认行为
        // 我们只在View创建完成后进行检测
        if (view == null) {
            try {
                // 尝试用LayoutInflater的默认行为创建View (例如通过Class.forName(name).getConstructor(Context.class, AttributeSet.class).newInstance())
                // 这是为了确保即使没有原始Factory，也能创建View
                // 警告: 这种方式不完全等同于LayoutInflater的内部行为，可能无法正确处理所有情况。
                // 最佳实践是确保原始Factory2/Factory被正确调用。
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android Q及以上，LayoutInflater内部实现更复杂，不推荐直接反射
                    // 而是应该确保 Factory 被链式调用
                    // 如果mOriginalFactory2/mOriginalFactory为空，这里可能需要更多的逻辑来确保View被正确inflate
                    // 或者依赖外部传入的inflater对象来处理
                } else {
                    // 对于低版本，可以尝试通过反射调用 LayoutInflater 的 createViewFromTag 方法
                    // 但非常不推荐，容易导致兼容性问题
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to create view for " + name + ": " + e.getMessage());
            }
        }

        if (view != null) {
            // 在View创建成功后进行检测
            // 获取当前LayoutInflater所关联的布局资源ID (只能在inflate之前获取，这里只能通过传递)
            // 这是一个挑战：onCreateView不直接提供布局ID。
            // 我们只能在外部调用 inflate 时传入布局ID，或者通过反射获取 LayoutInflater 的 mRootView
            // 更好的方式是在应用层调用 inflate 的地方进行检测。
            // 但如果作为通用插件，需要在这里拦截。
            // 这里的 LayoutDetector.detectDelayedLoadableLayouts(mContext, 0, null, view, "")
            // 的 rootId 和 rootResName 只能是 N/A 或通过其他方式获取。
            // 对于 Factory2，它只知道要创建哪个View，不知道它是哪个根布局的一部分。
            // 解决办法：在外部 Activity/Fragment 的 inflate 之前和之后进行检测。
            // 或者，我们假设这个 Factory2 总是用于顶级布局的inflate。
            // 在这里，我们将检测的根布局ID和名称设置为当前创建的View的ID和名称（如果它有ID的话）
            int rootLayoutId = view.getId();
            String rootLayoutResName = "N/A";
            if (rootLayoutId != View.NO_ID) {
                try {
                    rootLayoutResName = context.getResources().getResourceEntryName(rootLayoutId);
                } catch (Resources.NotFoundException e) { /* ignore */ }
            } else {
                // 如果没有ID，尝试使用布局文件的名称（但这里无法直接获取，需要从外部传递）
                // 暂时用类名作为占位
                rootLayoutResName = name;
            }

            LayoutDetector.detectDelayedLoadableLayouts(mContext, rootLayoutId, rootLayoutResName, view, "Root");
        }
        return view;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        // Factory 回调，直接调用 Factory2 的重载方法
        return onCreateView(null, name, context, attrs);
    }

    /**
     * 设置自定义的 LayoutInflateInterceptor
     * 传入 Application 或 Activity 的 context
     * @param context
     */
    public static void install(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        try {
            // 反射获取 mFactory2 字段，保存原始 Factory2
            Field mFactory2Field = LayoutInflater.class.getDeclaredField("mFactory2");
            mFactory2Field.setAccessible(true);
            // 确保没有其他 Factory2 已经被设置，或者正确链式调用
            LayoutInflater.Factory2 originalFactory2 = (LayoutInflater.Factory2) mFactory2Field.get(inflater);

            LayoutInflateInterceptor interceptor = new LayoutInflateInterceptor(context, inflater);

            // 设置自定义的 Factory2
            inflater.setFactory2(interceptor);
            Log.d(TAG, "LayoutInflateInterceptor installed successfully.");

            // 如果有原始 Factory，也需要设置到我们新的 Factory2 中，以便它能被链式调用
            if (originalFactory2 != null && originalFactory2 != interceptor) { // 避免死循环
                // 这里需要处理链式调用，将原始 Factory 作为我们拦截器的内部变量
                // LayoutInflateInterceptor 构造函数中已经处理了获取原始Factory2/Factory
                // 所以这里只需要设置即可。
            }

        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to install LayoutInflateInterceptor: " + e.getMessage());
        }
    }
}