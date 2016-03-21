//
//  RCTIONGPUImageView.m
//  RCTIONGPUImage
//
//  Created by Marko on 16/03/16.
//
//

#import "RCTIONGPUImageView.h"
#import "RCTIONGPUImageManager.h"

@implementation RCTIONGPUImageView

- (void)layoutSubviews {
    [super layoutSubviews];
    
    if (self.gpuImageId) {
        [[RCTIONGPUImageManager getGPUImage:self.gpuImageId] requestRender];
    }
}

-(void)setGpuImageId:(NSString *)gpuImageId {
    if (gpuImageId != _gpuImageId) {
        if (_gpuImageId) {
            [RCTIONGPUImageManager getGPUImage:self.gpuImageId].view = nil;
        }
        
        _gpuImageId = gpuImageId;

        if (_gpuImageId) {
            [RCTIONGPUImageManager getGPUImage:self.gpuImageId].view = self;
        }
    }
}

@end
