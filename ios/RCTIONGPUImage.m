//
//  RCTIONGPUImage.m
//  RCTIONGPUImage
//
//  Created by Marko on 15/03/16.
//
//

#import "RCTIONGPUImage.h"

@interface RCTIONGPUImage ()

@property(nonatomic, copy) NSString *gpuImageId;

@end

@implementation RCTIONGPUImage {
    GPUImagePicture *_pictureSource;
}

#pragma mark - Init

- (instancetype)init {
    self = [super init];
    
    if (self) {
        self.gpuImageId = [[NSUUID UUID] UUIDString];
    }
    
    return self;
}

#pragma mark - Setters

-(void)setView:(RCTIONGPUImageView *)view {
    if (view != _view) {
        if (_view) {
            if (self.currentFilter) {
                [self.currentFilter removeTarget:_view];
            }
        }
        
        _view = view;
        
        if (_view) {
            if (self.currentFilter) {
                [self.currentFilter addTarget:_view];
            }
        }
        
        [self requestRender];
    }
}

-(void)setCurrentFilter:(GPUImageOutput<GPUImageInput> *)currentFilter {
    if (currentFilter != _currentFilter) {
        if (_currentFilter) {
            if (self.view) {
                [_currentFilter removeTarget:self.view];
            }
            
            if (_pictureSource) {
                [_pictureSource removeTarget:currentFilter];
            }
        }
        
        _currentFilter = currentFilter;
        
        if (_currentFilter) {
            if (self.view) {
                [_currentFilter addTarget:self.view];
            }
            
            if (_pictureSource) {
                [_pictureSource addTarget:currentFilter];
            }
        }
        
        [self requestRender];
    }
}

-(void)setUiImage:(UIImage *)uiImage {
    if (uiImage != _uiImage) {
        if (_pictureSource && self.currentFilter) {
            [_pictureSource removeTarget:self.currentFilter];
        }
        
        _pictureSource = nil;
        _uiImage = uiImage;
        
        if (_uiImage) {
            _pictureSource = [[GPUImagePicture alloc] initWithImage:self.uiImage];
            if (self.currentFilter) {
                [_pictureSource addTarget:self.currentFilter];
            }
        }
        
        [self requestRender];
    }
}

#pragma mark - Public methods

- (void)requestRender {
    if (self.currentFilter) {
        [self.currentFilter useNextFrameForImageCapture];
    }
    
    if (_pictureSource) {
        [_pictureSource processImage];
    }
}

- (UIImage *)getFilteredUIImage {
    if (!self.currentFilter) {
        return nil;
    }
    
    [self requestRender];
    
    return [self.currentFilter imageFromCurrentFramebuffer];
}

@end
