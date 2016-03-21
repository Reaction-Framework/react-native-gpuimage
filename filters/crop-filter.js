'use strict';

import FilterBase from './filter-base';

export default class CropFilter extends FilterBase {
    constructor() {
        super();

        this.cropRegion = {
            x: 0.0,
            y: 0.0,
            width: 1.0,
            height: 1.0
        };
    }

    get id() {
        return 'crop';
    }

    getParams() {
        return {
            cropRegion: this.cropRegion
        };
    }
}
