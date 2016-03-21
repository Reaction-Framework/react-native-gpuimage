//
//  RCTIONGPUImageManager.m
//  RCTIONGPUImage
//
//  Created by Marko on 14/03/16.
//
//

#import "RCTIONGPUImageManager.h"
#import "RCTUtils.h"
#import "GPUImageFilterGroup+RCTION.h"
#import "RCTIONGPUImageUtils.h"

static NSMutableDictionary<NSString *, RCTIONGPUImage *> *gpuImages;

@implementation RCTIONGPUImageManager

+ (NSMutableDictionary<NSString *, RCTIONGPUImage *> *) getGPUImages {
    if (!gpuImages) {
        gpuImages = [[NSMutableDictionary<NSString *, RCTIONGPUImage *> alloc] init];
    }
    
    return gpuImages;
}

+ (RCTIONGPUImage *) getGPUImage:(NSString *)gpuImageId {
    return [[RCTIONGPUImageManager getGPUImages] valueForKey:gpuImageId];
}

- (RCTIONGPUImage *)createGPUImage {
    RCTIONGPUImage *gpuImage = [[RCTIONGPUImage alloc] init];
    [[RCTIONGPUImageManager getGPUImages] setObject:gpuImage
                                             forKey:[gpuImage gpuImageId]];
    return gpuImage;
}

- (RCTIONGPUImage *)getGPUImage:(NSString *)gpuImageId {
    return [RCTIONGPUImageManager getGPUImage:gpuImageId];
}

- (void)releaseGPUImage:(NSString *)gpuImageId {
    [[RCTIONGPUImageManager getGPUImages] removeObjectForKey:gpuImageId];
}

- (void)reject:(RCTPromiseRejectBlock)reject withException:(NSException *)exception {
    NSLog(@"RCTIONGPUImageManager error: %@", exception);
    [self reject:reject withMessage:exception.reason];
}

- (void)reject:(RCTPromiseRejectBlock)reject withMessage:(NSString *)message {
    NSError *error = RCTErrorWithMessage(message);
    reject([@(error.code) stringValue], error.userInfo.description, error);
}

- (NSDictionary *)getParams:(NSDictionary *)options {
    return [options objectForKey:@"params"];
}

RCT_EXPORT_MODULE(RCTIONGPUImageManager);

RCT_EXPORT_METHOD(create:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    RCTIONGPUImage* gpuImage = [self createGPUImage];
    
    @try {
        NSString* path = [options objectForKey:@"path"];
        
        if (!path) {
            @throw [NSException exceptionWithName:NSInvalidArgumentException
                                           reason:@"GPUImage requires path argument."
                                         userInfo:nil];
        }
        
        UIImage *image = [UIImage imageWithContentsOfFile:path];
        NSInteger maxWidth = [options[@"maxWidth"] integerValue];
        NSInteger maxHeight = [options[@"maxHeight"] integerValue];
        
        if (maxWidth && maxHeight) {
            image = GPUResizeUIImage(image, maxWidth, maxHeight);
        }
        
        gpuImage.uiImage = image;
        resolve([gpuImage gpuImageId]);
    } @catch (NSException *exception) {
        [self releaseGPUImage:[gpuImage gpuImageId]];
        [self reject:reject withException:exception];
    }
}

RCT_EXPORT_METHOD(save:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        RCTIONGPUImage* gpuImage = [self getGPUImage:[options objectForKey:@"id"]];
        
        NSDictionary *params = [self getParams:options];
        NSString *path = [params objectForKey:@"path"];
        if (!path) {
            @throw [NSException exceptionWithName:NSInvalidArgumentException
                                           reason:@"GPUImage save requires path argument."
                                         userInfo:nil];
        }
        
        UIImage *filteredImage = [gpuImage getFilteredUIImage];
        if (!filteredImage) {
            @throw [NSException exceptionWithName:NSInvalidArgumentException
                                           reason:@"GPUImage could not create image to save."
                                         userInfo:nil];
        }
        
        NSData *imageData = UIImagePNGRepresentation(filteredImage);
        [imageData writeToFile:path atomically:YES];
        
        resolve(nil);
    } @catch (NSException *exception) {
        [self reject:reject withException:exception];
    }
}

RCT_EXPORT_METHOD(getImageSize:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        RCTIONGPUImage* gpuImage = [self getGPUImage:[options objectForKey:@"id"]];
        
        NSMutableDictionary<NSString *, NSNumber *> *output = [[NSMutableDictionary<NSString *, NSNumber *> alloc] init];
        if (gpuImage.uiImage) {
            [output setObject:@((int)gpuImage.uiImage.size.width) forKey:@"width"];
            [output setObject:@((int)gpuImage.uiImage.size.height) forKey:@"height"];
        }
        
        resolve(output);
    } @catch (NSException *exception) {
        [self reject:reject withException:exception];
    }
}

RCT_EXPORT_METHOD(addFilter:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        RCTIONGPUImage* gpuImage = [self getGPUImage:[options objectForKey:@"id"]];
        
        if (![gpuImage.currentFilter isKindOfClass:[GPUImageFilterGroup class]]) {
            gpuImage.currentFilter = [[GPUImageFilterGroup alloc] init];
        }
        
        GPUImageFilterGroup *group = (GPUImageFilterGroup *)gpuImage.currentFilter;
        
        NSDictionary *params = [self getParams:options];
        NSString *filterId = [params valueForKey:@"id"];
        Class filterType = GPUGetFilterTypeForId(filterId);
        if (filterType) {
            if ([group filterByType:filterType]) {
                resolve(nil);
                return;
            }
            
            params = [self getParams:params];
            GPUImageOutput<GPUImageInput> *filter = GPUGetFilterForId(filterId, params);
            
            if (filter) {
                [group addFilter:filter];
                
                if (![group.initialFilters count]) {
                    [group setInitialFilters:[NSArray arrayWithObject:filter]];
                }
                
                if (group.terminalFilter) {
                    [group.terminalFilter addTarget:filter];
                }
                
                [group setTerminalFilter:filter];
                
                [gpuImage requestRender];
                
                resolve(nil);
                return;
            }
        }
        
        @throw [NSException exceptionWithName:NSInvalidArgumentException
                                       reason:@"Filter could not be added to gpuImage."
                                     userInfo:nil];
    } @catch (NSException *exception) {
        [self reject:reject withException:exception];
    }
}

RCT_EXPORT_METHOD(updateFilter:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        RCTIONGPUImage* gpuImage = [self getGPUImage:[options objectForKey:@"id"]];
        
        GPUImageFilterGroup *group = (GPUImageFilterGroup *)gpuImage.currentFilter;
        NSDictionary *params = [self getParams:options];
        NSString *filterId = [params valueForKey:@"id"];
        Class filterType = GPUGetFilterTypeForId(filterId);
        if (filterType) {
            GPUImageOutput<GPUImageInput> *filter = [group filterByType:filterType];
            if (filter) {
                GPUUpdateFilter(filter, [self getParams:params]);
                [gpuImage requestRender];
                resolve(nil);
                return;
            }
        }
        
        @throw [NSException exceptionWithName:NSInvalidArgumentException
                                       reason:@"Filter could not be updated."
                                     userInfo:nil];
    } @catch (NSException *exception) {
        [self reject:reject withException:exception];
    }
}

RCT_EXPORT_METHOD(releaseView:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    @try {
        RCTIONGPUImage* gpuImage = [self getGPUImage:[options objectForKey:@"id"]];
        gpuImage.view = nil;
        resolve(nil);
    } @catch (NSException *exception) {
        [self reject:reject withException:exception];
    }
}

RCT_EXPORT_METHOD(release:(NSDictionary *)options
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
    [self releaseGPUImage:[options objectForKey:@"id"]];
    resolve(nil);
}

@end
