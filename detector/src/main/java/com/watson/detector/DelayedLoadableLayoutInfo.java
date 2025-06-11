package com.watson.detector;

public class DelayedLoadableLayoutInfo {
    public enum LayoutType {
        VIEW_STUB,
        INCLUDE_GONE_INVISIBLE,
        UNKNOWN_DELAYED
    }

    public String layoutResName; // 布局资源名称，如 R.layout.my_layout
    public int layoutResId;     // 布局资源ID
    public String viewIdResName; // 具体延迟加载View的ID名称，如 R.id.my_view_stub
    public int viewId;          // 具体延迟加载View的ID
    public LayoutType type;
    public String parentViewHierarchy; // 父View的层次结构路径，方便定位

    public DelayedLoadableLayoutInfo(int layoutResId, String layoutResName, int viewId, String viewIdResName, LayoutType type, String parentViewHierarchy) {
        this.layoutResId = layoutResId;
        this.layoutResName = layoutResName;
        this.viewId = viewId;
        this.viewIdResName = viewIdResName;
        this.type = type;
        this.parentViewHierarchy = parentViewHierarchy;
    }

    @Override
    public String toString() {
        return "DelayedLayoutInfo {" +
                "type=" + type +
                ", layoutResId=" + layoutResName +
                ", viewId=" + (viewIdResName != null ? viewIdResName : "N/A") +
                ", parent=" + parentViewHierarchy +
                '}';
    }
}
