//
//  GPUImageFilterGroup+RCTION.h
//  RCTIONGPUImage
//
//  Created by Marko on 17/03/16.
//
//

#import "GPUImage.h"

@interface GPUImageFilterGroup (RCTION)

- (GPUImageOutput<GPUImageInput> *)filterByType:(Class)filterType;

@end
