'use strict';

import React, {
    StyleSheet,
    Component,
    View,
    PanResponder
} from 'react-native';
import CropFilter from './filters/crop-filter';

export default class GPUCropView extends Component {
    constructor(props) {
        super(props);

        this.cropFilter = new CropFilter();
        const gpuImage = props.gpuImage;
        gpuImage && gpuImage.addFilter(this.cropFilter);
    }

    componentWillReceiveProps(nextProps) {
        const oldId = this.getGPUImageId();
        const newId = nextProps.gpuImage ? nextProps.gpuImage.id : null;

        if (oldId !== newId && newId !== null) {
            const gpuImage = nextProps.gpuImage;
            this.cropFilter = new CropFilter();
            gpuImage.addFilter(this.cropFilter);
        }
    }

    componentWillMount() {
        this.panResponder = PanResponder.create({
            onStartShouldSetPanResponder: this.handleStartShouldSetPanResponder.bind(this),
            onMoveShouldSetPanResponder: this.handleMoveShouldSetPanResponder.bind(this),
            onPanResponderGrant: this.handlePanResponderGrant.bind(this),
            onPanResponderMove: this.handlePanResponderMove.bind(this),
            onPanResponderRelease: this.handlePanResponderEnd.bind(this),
            onPanResponderTerminate: this.handlePanResponderEnd.bind(this)
        });
    }

    componentDidMount() {
        const imageSize = {
            width: 1,
            height: 1
        };
        this.setupCropperSize(imageSize, 0, 0, 0, 0);
    }

    getGPUImage() {
        return this.props.gpuImage;
    }

    getGPUImageId() {
        return this.getGPUImage() ? this.getGPUImage().id : null;
    }

    onRootLayout() {
        this.root.measure(async (fx, fy, width, height, px, py) => {
            const gpuImage = this.getGPUImage();
            if (!gpuImage) {
                return;
            }

            if (this.width === width && this.height === height && !this.layoutRoot) {
                return;
            }

            this.layoutRoot = false;
            const imageSize = await gpuImage.getImageSize();
            this.setupCropperSize(imageSize, width, height, px, py);
        });
    }

    setupCropperSize(imageSize, width, height, offsetLeft, offsetTop) {
        this.width = width;
        this.height = height;

        const imageWidth = imageSize.width * this.cropFilter.cropRegion.width;
        const imageHeight = imageSize.height * this.cropFilter.cropRegion.height;
        const scale = Math.min(width / imageWidth, height / imageHeight);

        const newWidth = imageWidth * scale;
        const newHeight = imageHeight * scale;

        const newX = width / 2.0 - newWidth / 2.0;
        const newY = height / 2.0 - newHeight / 2.0;

        this.cropRectLeft = newX;
        this.cropRectTop = newY;
        this.cropRectWidth = newWidth;
        this.cropRectHeight = newHeight;

        this.maxRight = newX + newWidth;
        this.maxBottom = newY + newHeight;
        this.minLeft = newX;
        this.minTop = newY;
        this.offsetLeft = offsetLeft;
        this.offsetTop = offsetTop;

        this.cropRectStyle = {
            left: this.cropRectLeft,
            top: this.cropRectTop,
            width: this.cropRectWidth,
            height: this.cropRectHeight
        };

        this.updateCropRect();
    }

    render() {
        this.layoutRoot = true;

        return (
            <View onLayout={this.onRootLayout.bind(this)} ref={c => this.root = c} style={[styles.absolute]} {...this.panResponder.panHandlers}>
                <View ref={c => this.overlayLeft = c} style={[styles.absolute, styles.cropViewOverlay]}/>
                <View ref={c => this.overlayTop = c} style={[styles.absolute, styles.cropViewOverlay]}/>
                <View ref={c => this.overlayRight = c} style={[styles.absolute, styles.cropViewOverlay]}/>
                <View ref={c => this.overlayBottom = c} style={[styles.absolute, styles.cropViewOverlay]}/>
                <View ref={c => this.cropRect = c} style={[styles.absolute, styles.cropRectangle]}/>
            </View>
        );
    }

    handleStartShouldSetPanResponder(e, gestureState) {
        return true;
    }

    handleMoveShouldSetPanResponder(e, gestureState) {
        return true;
    }

    handlePanResponderGrant(e, gestureState) {

    }

    handlePanResponderMove(e, gestureState) {
        !this.handleResize(gestureState) && this.handleMove(gestureState);
        this.updateCropRect();
    }

    handleResize(gestureState) {
        const resizeThreshold = 30;
        const left = gestureState.x0 - this.offsetLeft;
        const leftResizing = left >= this.cropRectLeft - resizeThreshold &&
            left <= this.cropRectLeft + resizeThreshold;
        const rightResizing = left >= this.cropRectLeft + this.cropRectWidth - resizeThreshold &&
            left <= this.cropRectLeft + this.cropRectWidth + resizeThreshold;
        const top = gestureState.y0 - this.offsetTop;
        const topResizing = top >= this.cropRectTop - resizeThreshold &&
            top <= this.cropRectTop + resizeThreshold;
        const bottomResizing = top >= this.cropRectTop + this.cropRectHeight - resizeThreshold &&
            top <= this.cropRectTop + this.cropRectHeight + resizeThreshold;

        if (!leftResizing && !rightResizing && !topResizing && !bottomResizing) {
            return false;
        }

        if (leftResizing) {
            const newWidth = this.cropRectWidth - gestureState.dx;
            const newLeft = this.cropRectLeft + gestureState.dx;
            if (newWidth > 0 && newLeft >= this.minLeft) {
                this.cropRectStyle.left = newLeft;
                this.cropRectStyle.width = newWidth;
            }
        } else if (rightResizing) {
            const newWidth = this.cropRectWidth + gestureState.dx;
            const newRight = this.cropRectStyle.left + newWidth;
            if (newWidth > 0 && newRight <= this.maxRight) {
                this.cropRectStyle.width = newWidth;
            }
        }

        if (topResizing) {
            const newHeight = this.cropRectHeight - gestureState.dy;
            const newTop = this.cropRectTop + gestureState.dy;
            if (newHeight > 0 && newTop >= this.minTop) {
                this.cropRectStyle.top = newTop;
                this.cropRectStyle.height = newHeight;
            }
        } else if (bottomResizing) {
            const newHeight = this.cropRectHeight + gestureState.dy;
            const newBottom = this.cropRectStyle.top + newHeight;
            if (newHeight > 0 && newBottom <= this.maxBottom) {
                this.cropRectStyle.height = newHeight;
            }
        }

        return true;
    }

    handleMove(gestureState) {
        let newLeft = this.cropRectLeft + gestureState.dx;
        if (newLeft < this.minLeft) {
            newLeft = this.minLeft;
        }

        const newRight = newLeft + this.cropRectStyle.width;
        if (newRight > this.maxRight) {
            newLeft -= newRight - this.maxRight;
        }

        this.cropRectStyle.left = newLeft;

        let newTop = this.cropRectTop + gestureState.dy;
        if (newTop < this.minTop) {
            newTop = this.minTop;
        }

        const newBottom = newTop + this.cropRectStyle.height;
        if (newBottom > this.maxBottom) {
            newTop -= newBottom - this.maxBottom;
        }

        this.cropRectStyle.top = newTop;
    }

    handlePanResponderEnd(e, gestureState) {
        this.cropRectLeft = this.cropRectStyle.left;
        this.cropRectTop = this.cropRectStyle.top;
        this.cropRectWidth = this.cropRectStyle.width;
        this.cropRectHeight = this.cropRectStyle.height;
    }

    updateCropRect() {
        this.cropRect && this.cropRect.setNativeProps({
            style: this.cropRectStyle
        });

        this.overlayLeft && this.overlayLeft.setNativeProps({
            style: {
                top: this.cropRectStyle.top,
                height: this.cropRectStyle.height,
                left: 0,
                width: this.cropRectStyle.left
            }
        });

        this.overlayTop && this.overlayTop.setNativeProps({
            style: {
                top: 0,
                height: this.cropRectStyle.top,
                left: 0,
                width: this.width
            }
        });

        this.overlayRight && this.overlayRight.setNativeProps({
            style: {
                top: this.cropRectStyle.top,
                height: this.cropRectStyle.height,
                left: this.cropRectStyle.left + this.cropRectStyle.width,
                width: this.width - this.cropRectStyle.left + this.cropRectStyle.width
            }
        });

        this.overlayBottom && this.overlayBottom.setNativeProps({
            style: {
                top: this.cropRectStyle.top + this.cropRectStyle.height,
                height: this.height - this.cropRectStyle.top + this.cropRectStyle.height,
                left: 0,
                width: this.width
            }
        });
    }

    async crop() {
        const gpuImage = this.getGPUImage();
        if (!gpuImage) {
            return;
        }

        const cropFilter = this.cropFilter;
        cropFilter.cropRegion = {
            x: cropFilter.cropRegion.x + (this.cropRectStyle.left - this.minLeft) / (this.maxRight - this.minLeft) * cropFilter.cropRegion.width,
            y: cropFilter.cropRegion.y + (this.cropRectStyle.top - this.minTop) / (this.maxBottom - this.minTop) * cropFilter.cropRegion.height,
            width: cropFilter.cropRegion.width * this.cropRectStyle.width / (this.maxRight - this.minLeft),
            height: cropFilter.cropRegion.height * this.cropRectStyle.height / (this.maxBottom - this.minTop)
        };

        await gpuImage.updateFilter(cropFilter);

        this.root.measure(async (fx, fy, width, height, px, py) => {
            const imageSize = await gpuImage.getImageSize();
            this.setupCropperSize(imageSize, width, height, px, py);
        });
    }
}

const styles = StyleSheet.create({
    absolute: {
        position: 'absolute',
        top: 0,
        left: 0,
        bottom: 0,
        right: 0,
        backgroundColor: '#00000000'
    },
    cropViewOverlay: {
        backgroundColor: '#000000AA'
    },
    cropRectangle: {
        backgroundColor: '#00000000',
        borderColor: '#ffffff',
        borderStyle: 'dashed',
        borderWidth: 1
    }
});
