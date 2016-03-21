//
//  RCTIONGPUImageUtils.h
//  RCTIONGPUImage
//
//  Created by Marko on 17/03/16.
//
//

#import <Foundation/Foundation.h>
#import "GPUImage.h"

Class GPUGetFilterTypeForId(NSString *filterId);
GPUImageOutput<GPUImageInput> *GPUGetFilterForId(NSString *filterId, NSDictionary *params);
void GPUUpdateFilter(GPUImageOutput<GPUImageInput> *filter, NSDictionary *params);
UIImage *GPUResizeUIImage(UIImage *image, CGFloat maxWidth, CGFloat maxHeight);
