'use strict';

import {NativeModules} from 'react-native';

const GPUImageManager = NativeModules.IONGPUImageManager;

export default class GPUImage {
    static async create(options) {
        const id = await GPUImageManager.create(options);
        return new GPUImage(id);
    }

    constructor(id) {
        this.id = id;
    }

    async save(options) {
        return GPUImageManager.save({
            id: this.id,
            params: options
        });
    }

    async getImageSize() {
        return GPUImageManager.getImageSize({
            id: this.id
        });
    }

    async addFilter(filter) {
        await GPUImageManager.addFilter({
            id: this.id,
            params: filter.getForNative()
        });
    }

    async updateFilter(filter) {
        await GPUImageManager.updateFilter({
            id: this.id,
            params: filter.getForNative()
        });
    }

    async releaseView() {
        return GPUImageManager.releaseView({
            id: this.id
        });
    }

    async release() {
        return GPUImageManager.release({
            id: this.id
        });
    }
}
