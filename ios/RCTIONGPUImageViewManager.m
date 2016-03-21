//
//  RCTIONGPUImageViewManager.m
//  RCTIONGPUImage
//
//  Created by Marko on 16/03/16.
//
//

#import "RCTIONGPUImageViewManager.h"

@implementation RCTIONGPUImageViewManager

RCT_EXPORT_MODULE(RCTIONGPUImageViewManager)

- (UIView *)view {
    return [[RCTIONGPUImageView alloc] init];
}

RCT_EXPORT_VIEW_PROPERTY(gpuImageId, NSString)

@end
