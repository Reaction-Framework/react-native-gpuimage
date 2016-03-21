'use strict';

import FilterBase from './filter-base';

export default class HueFilter extends FilterBase {
    constructor() {
        super();

        this.hue = 90.0;
    }

    get id() {
        return 'hue';
    }

    getParams() {
        return {
            hue: this.hue
        };
    }
}
