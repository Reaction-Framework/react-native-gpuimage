'use strict';

export default class FilterBase {
    get id() {
        return null;
    }

    getParams() {
        return null;
    }

    getForNative() {
        return {
            id: this.id,
            params: this.getParams()
        };
    }
}
