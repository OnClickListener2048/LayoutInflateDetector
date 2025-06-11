package com.watson.detector;// LayoutDetector.java
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import java.util.ArrayList;
import java.util.List;

public class LayoutDetector {

    private static final String TAG = "LayoutDetector";
    private static List<DelayedLoadableLayoutInfo> detectedLayouts = new ArrayList<>();

    public static List<DelayedLoadableLayoutInfo> getDetectedLayouts() {
        return detectedLayouts;
    }

    public static void clearDetectedLayouts() {
        detectedLayouts.clear();
    }

    /**
     * 递归遍历View树，查找延迟加载布局
     * @param context 上下文
     * @param rootId 根布局的资源ID (可选, 初始调用时可为0)
     * @param rootResName 根布局的资源名称 (可选)
     * @param view 要检测的View
     * @param parentHierarchyPath 父View的层次结构路径
     */
    public static void detectDelayedLoadableLayouts(Context context, int rootId, String rootResName, View view, String parentHierarchyPath) {
        if (view == null) {
            return;
        }

        String currentHierarchyPath = parentHierarchyPath + "/" + view.getClass().getSimpleName();
        if (view.getId() != View.NO_ID) {
            try {
                String idName = context.getResources().getResourceEntryName(view.getId());
                currentHierarchyPath += "[id/" + idName + "]";
            } catch (Resources.NotFoundException e) {
                // ID not found, maybe a dynamically generated ID or private ID
            }
        }


        // 1. 检测 ViewStub
        if (view instanceof ViewStub) {
            ViewStub viewStub = (ViewStub) view;
            int viewStubId = viewStub.getId();
            String viewStubIdName = "N/A";
            if (viewStubId != View.NO_ID) {
                try {
                    viewStubIdName = context.getResources().getResourceEntryName(viewStubId);
                } catch (Resources.NotFoundException e) { /* ignore */ }
            }

            DelayedLoadableLayoutInfo info = new DelayedLoadableLayoutInfo(
                    rootId, rootResName, viewStubId, viewStubIdName,
                    DelayedLoadableLayoutInfo.LayoutType.VIEW_STUB,
                    currentHierarchyPath
            );
            detectedLayouts.add(info);
            Log.d(TAG, "Detected ViewStub: " + info.toString());
        }

        // 2. 检测初始 visibility 为 GONE 或 INVISIBLE 的 View
        // 注意：这只是一个推测，无法直接从View判断它是否来自 <include>
        // 但如果它的初始状态是 GONE/INVISIBLE，通常意味着它会在后期被显示。
        // 这需要我们在 inflate 之后立即检测，因为 ViewStub inflate 后会变为实际的 View
        // 而 GONE/INVISIBLE 的 View 也可能在之后通过代码改变 visibility。
        // 所以我们检测的是 "初始" 状态。
        if (view.getVisibility() == View.GONE || view.getVisibility() == View.INVISIBLE) {
            // 排除 ViewStub 本身，因为 ViewStub 也可能初始是 GONE
            // 但 ViewStub 已经单独处理了
            if (!(view instanceof ViewStub)) {
                int viewId = view.getId();
                String viewIdName = "N/A";
                if (viewId != View.NO_ID) {
                    try {
                        viewIdName = context.getResources().getResourceEntryName(viewId);
                    } catch (Resources.NotFoundException e) { /* ignore */ }
                }

                // 排除父View也为GONE/INVISIBLE的情况，避免重复检测嵌套的GONE/INVISIBLE布局
                // 仅当其直接父View是VISIBLE时才检测
                // (更精确的判断需要维护一个当前层级所有View的visibility状态，这里简化处理)
                boolean parentIsVisible = true; // 假设为true，实际判断需更复杂
                if (view.getParent() instanceof View) {
                    View parentView = (View) view.getParent();
                    if (parentView.getVisibility() == View.GONE || parentView.getVisibility() == View.INVISIBLE) {
                        parentIsVisible = false;
                    }
                }

                if (parentIsVisible) {
                    DelayedLoadableLayoutInfo info = new DelayedLoadableLayoutInfo(
                            rootId, rootResName, viewId, viewIdName,
                            DelayedLoadableLayoutInfo.LayoutType.INCLUDE_GONE_INVISIBLE,
                            currentHierarchyPath
                    );
                    detectedLayouts.add(info);
                    Log.d(TAG, "Detected GONE/INVISIBLE View: " + info.toString());
                }
            }
        }

        // 递归遍历子View
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                detectDelayedLoadableLayouts(context, rootId, rootResName, viewGroup.getChildAt(i), currentHierarchyPath);
            }
        }
    }
}