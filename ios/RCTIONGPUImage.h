//
//  RCTIONGPUImage.h
//  RCTIONGPUImage
//
//  Created by Marko on 15/03/16.
//
//

#import <Foundation/Foundation.h>
#import <CoreGraphics/CoreGraphics.h>
#import "GPUImage.h"
#import "RCTIONGPUImageView.h"

@interface RCTIONGPUImage : NSObject

#pragma mark - Base

@property(readonly, nonatomic, copy) NSString *gpuImageId;
@property(readwrite, nonatomic, strong) GPUImageOutput<GPUImageInput> *currentFilter;
@property(readwrite, nonatomic, strong) RCTIONGPUImageView *view;
@property(readwrite, nonatomic, strong) UIImage *uiImage;

- (void)requestRender;
- (UIImage *)getFilteredUIImage;

@end
