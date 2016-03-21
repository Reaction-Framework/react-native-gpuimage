//
//  RCTIONGPUImageManager.h
//  RCTIONGPUImage
//
//  Created by Marko on 14/03/16.
//
//

#import <Foundation/Foundation.h>
#import "RCTBridgeModule.h"
#import "RCTIONGPUImage.h"

@interface RCTIONGPUImageManager : NSObject <RCTBridgeModule>

+ (RCTIONGPUImage *)getGPUImage:(NSString *)gpuImageId;

- (void)create:(NSDictionary *)options
      resolver:(RCTPromiseResolveBlock)resolve
      rejecter:(RCTPromiseRejectBlock)reject;

- (void)save:(NSDictionary *)options
    resolver:(RCTPromiseResolveBlock)resolve
    rejecter:(RCTPromiseRejectBlock)reject;

- (void)getImageSize:(NSDictionary *)options
            resolver:(RCTPromiseResolveBlock)resolve
            rejecter:(RCTPromiseRejectBlock)reject;

- (void)addFilter:(NSDictionary *)options
         resolver:(RCTPromiseResolveBlock)resolve
         rejecter:(RCTPromiseRejectBlock)reject;

- (void)updateFilter:(NSDictionary *)options
            resolver:(RCTPromiseResolveBlock)resolve
            rejecter:(RCTPromiseRejectBlock)reject;

- (void)releaseView:(NSDictionary *)options
           resolver:(RCTPromiseResolveBlock)resolve
           rejecter:(RCTPromiseRejectBlock)reject;

- (void)release:(NSDictionary *)options
       resolver:(RCTPromiseResolveBlock)resolve
       rejecter:(RCTPromiseRejectBlock)reject;

@end
