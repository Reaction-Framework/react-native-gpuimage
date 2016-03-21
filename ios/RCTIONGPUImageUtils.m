//
//  RCTIONGPUImageUtils.m
//  RCTIONGPUImage
//
//  Created by Marko on 17/03/16.
//
//

#import "RCTIONGPUImageUtils.h"

#define CROP_FILTER_ID                @"crop"
#define HUE_FILTER_ID                 @"hue"

static NSDictionary<NSString *, Class> *filterTypes;
NSDictionary<NSString *, Class>* getFilterTypes() {
    if (filterTypes) {
        return filterTypes;
    }
    
    NSMutableDictionary<NSString *, Class> *types = [[NSMutableDictionary<NSString *, Class> alloc] init];
    [types setObject:[GPUImageCropFilter class] forKey:CROP_FILTER_ID];
    [types setObject:[GPUImageHueFilter class] forKey:HUE_FILTER_ID];
    
    filterTypes = [[NSDictionary<NSString *, Class> alloc] initWithDictionary:types];
    return filterTypes;
}

static NSDictionary<NSString *, GPUImageOutput<GPUImageInput>*(^)()> *filterFactories;
NSDictionary<NSString *, GPUImageOutput<GPUImageInput>*(^)()>* getFilterFactories() {
    if (filterFactories) {
        return filterFactories;
    }
    
    NSMutableDictionary<NSString *, GPUImageOutput<GPUImageInput>*(^)()> *factories = [[NSMutableDictionary<NSString *, GPUImageOutput<GPUImageInput>*(^)()> alloc] init];
    [factories setObject:^() {
        return [[GPUImageCropFilter alloc] init];
    } forKey:CROP_FILTER_ID];
    [factories setObject:^() {
        return [[GPUImageHueFilter alloc] init];
    } forKey:HUE_FILTER_ID];
    
    filterFactories = [[NSDictionary<NSString *, GPUImageOutput<GPUImageInput>*(^)()> alloc] initWithDictionary:factories];
    return filterFactories;
}

static NSDictionary<NSString *, void (^)(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params)> *filterUpdaters;
NSDictionary<NSString *, void (^)(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params)>* getFilterUpdaters() {
    if (filterUpdaters) {
        return filterUpdaters;
    }
    
    NSMutableDictionary<NSString *, void (^)(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params)> *updaters = [[NSMutableDictionary<NSString *, void (^)(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params)> alloc] init];
    [updaters setObject:^(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params) {
        NSDictionary* cropRegion = [params valueForKey:@"cropRegion"];
        if (!cropRegion) {
            return;
        }
        
        NSNumber* x = [cropRegion valueForKey:@"x"];
        NSNumber* y = [cropRegion valueForKey:@"y"];
        NSNumber* width = [cropRegion valueForKey:@"width"];
        NSNumber* height = [cropRegion valueForKey:@"height"];
        GPUImageCropFilter *cropFilter = (GPUImageCropFilter *) filter;
        [cropFilter setCropRegion:CGRectMake([x floatValue], [y floatValue], [width floatValue], [height floatValue])];
    } forKey:NSStringFromClass([GPUImageCropFilter class])];
    [updaters setObject:^(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params) {
        NSNumber* hue = [params valueForKey:@"hue"];
        if (!hue) {
            return;
        }
        
        GPUImageHueFilter *hueFilter = (GPUImageHueFilter *) filter;
        [hueFilter setHue:[hue floatValue]];
    } forKey:NSStringFromClass([GPUImageHueFilter class])];
    
    filterUpdaters = [[NSDictionary<NSString *, void (^)(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params)> alloc] initWithDictionary:updaters];
    return filterUpdaters;
}

Class GPUGetFilterTypeForId(NSString *filterId) {
    filterId = [filterId lowercaseString];
    return [getFilterTypes() valueForKey:filterId];
}

GPUImageOutput<GPUImageInput> *GPUGetFilterForId(NSString *filterId, NSDictionary *params) {
    filterId = [filterId lowercaseString];
    GPUImageOutput<GPUImageInput>*(^factory)() = [getFilterFactories() valueForKey:filterId];
    
    if (!factory) {
        return nil;
    }
    
    GPUImageOutput<GPUImageInput> *filter = factory();
    GPUUpdateFilter(filter, params);
    return filter;
}

void GPUUpdateFilter(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params) {
    void (^updater)(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params) = [getFilterUpdaters() valueForKey:NSStringFromClass([filter class])];
    
    if (!updater) {
        return;
    }
    
    updater(filter, params);
}

UIImage *GPUResizeUIImage(UIImage *image, CGFloat maxWidth, CGFloat maxHeight) {
    CGFloat orgWidth = image.size.width;
    CGFloat orgHeight = image.size.height;
    
    if (orgWidth <= maxWidth && orgHeight <= maxHeight) {
        return image;
    }
    
    CGFloat newWidth = maxWidth;
    CGFloat newHeight = maxHeight;
    
    if (orgWidth / maxWidth < orgHeight / maxHeight) {
        newWidth = (newHeight / orgHeight) * orgWidth;
    } else {
        newHeight = (newWidth / orgWidth) * orgHeight;
    }
    
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(newWidth, newHeight), NO, 0.0);
    [image drawInRect:CGRectMake(0, 0, newWidth, newHeight)];
    UIImage *newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return newImage;
}
