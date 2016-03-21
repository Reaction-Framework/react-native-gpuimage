package io.reactionframework.android.react.gpuimage.utils;

import com.facebook.react.bridge.ReadableMap;
import io.reactionframework.android.react.gpuimage.filters.GPUImageCropFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.GPUImageHueFilter;

import java.util.HashMap;
import java.util.Map;

public class GPUFilterUtils {
    private static final String CROP_FILTER_ID = "crop";
    private static final String HUE_FILTER_ID = "hue";

    private static Map<String, Class> sFilterTypes;
    private static Map<String, Func<GPUImageFilter>> sFilterFactories;
    private static Map<Class, Action<GPUImageFilter, ReadableMap>> sFilterUpdaters;

    private static Map<String, Class> getFilterTypes() {
        if (sFilterTypes != null) {
            return sFilterTypes;
        }

        sFilterTypes = new HashMap<>();
        sFilterTypes.put(CROP_FILTER_ID, GPUImageCropFilter.class);
        sFilterTypes.put(HUE_FILTER_ID, GPUImageHueFilter.class);

        return sFilterTypes;
    }

    private static Map<String, Func<GPUImageFilter>> getFilterFactories() {
        if (sFilterFactories != null) {
            return sFilterFactories;
        }

        sFilterFactories = new HashMap<>();

        sFilterFactories.put(CROP_FILTER_ID, new Func<GPUImageFilter>() {
            @Override
            public GPUImageFilter func() {
                return new GPUImageCropFilter();
            }
        });

        sFilterFactories.put(HUE_FILTER_ID, new Func<GPUImageFilter>() {
            @Override
            public GPUImageFilter func() {
                return new GPUImageHueFilter();
            }
        });

        return sFilterFactories;
    }

    private static Map<Class, Action<GPUImageFilter, ReadableMap>> getFilterUpdaters() {
        if (sFilterUpdaters != null) {
            return sFilterUpdaters;
        }

        sFilterUpdaters = new HashMap<>();

        sFilterUpdaters.put(GPUImageCropFilter.class, new Action<GPUImageFilter, ReadableMap>() {
            @Override
            public void action(GPUImageFilter arg1, ReadableMap arg2) {
                if (!arg2.hasKey("cropRegion")) {
                    return;
                }

                ReadableMap cropRegion = arg2.getMap("cropRegion");
                GPUImageCropFilter filter = (GPUImageCropFilter) arg1;
                filter.setCropRegion(new GPUImageCropFilter.CropRegion(
                        (float) cropRegion.getDouble("x"),
                        (float) cropRegion.getDouble("y"),
                        (float) cropRegion.getDouble("width"),
                        (float) cropRegion.getDouble("height")
                ));
            }
        });

        sFilterUpdaters.put(GPUImageHueFilter.class, new Action<GPUImageFilter, ReadableMap>() {
            @Override
            public void action(GPUImageFilter arg1, ReadableMap arg2) {
                if (!arg2.hasKey("hue")) {
                    return;
                }

                GPUImageHueFilter filter = (GPUImageHueFilter) arg1;
                filter.setHue((float) arg2.getDouble("hue"));
            }
        });

        return sFilterUpdaters;
    }

    public static Class getFilterTypeForId(String filterId) {
        filterId = filterId.toLowerCase();

        if (!getFilterTypes().containsKey(filterId)) {
            return null;
        }

        return getFilterTypes().get(filterId);
    }

    public static GPUImageFilter getFilterForId(String filterId, ReadableMap params) {
        filterId = filterId.toLowerCase();

        if (!getFilterFactories().containsKey(filterId)) {
            return null;
        }

        GPUImageFilter filter = getFilterFactories().get(filterId).func();
        updateFilter(filter, params);

        return filter;
    }

    public static void updateFilter(GPUImageFilter filter, ReadableMap params) {
        Class filterType = filter.getClass();
        if (!getFilterUpdaters().containsKey(filterType)) {
            return;
        }

        getFilterUpdaters().get(filterType).action(filter, params);
    }

    public static GPUImageFilter getFilterFromGroup(GPUImageFilterGroup filterGroup, Class filterType) {
        for (GPUImageFilter filter:filterGroup.getFilters()) {
            if (filterType.isInstance(filter)) {
                return filter;
            }
        }

        return null;
    }

    private interface Func<T> {
        T func();
    }

    private interface Action<T1, T2> {
        void action(T1 arg1, T2 arg2);
    }
}
