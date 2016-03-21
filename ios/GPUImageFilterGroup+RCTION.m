//
//  GPUImageFilterGroup+RCTION.m
//  RCTIONGPUImage
//
//  Created by Marko on 17/03/16.
//
//

#import "GPUImageFilterGroup+RCTION.h"

@implementation GPUImageFilterGroup (RCTION)

- (GPUImageOutput<GPUImageInput> *)filterByType:(Class)filterType {
    for (int i = 0; i < [self filterCount]; i++) {
        GPUImageOutput<GPUImageInput> *filter = [self filterAtIndex:i];
        if ([filter isKindOfClass:filterType]) {
            return filter;
        }
    }
    
    return nil;
}

@end
