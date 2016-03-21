'use strict';

import React, {
    StyleSheet,
    Component,
    requireNativeComponent,
    PropTypes,
    View
} from 'react-native';
import GPUCropView from './crop-view';

const viewPropTypes = View.propTypes;

export default class GPUImageView extends Component {
    static propTypes = {
        ...viewPropTypes,
        gpuImageId: PropTypes.string
    };

    static defaultProps = {
        gpuImage: null
    };

    setNativeProps(props) {
        const nativeProps = Object.assign({}, {
            gpuImageId: this.getGPUImageId()
        });
        this.nativeView.setNativeProps(nativeProps);
    }

    componentWillReceiveProps(nextProps) {
        const oldId = this.getGPUImageId();
        const newId = nextProps.gpuImage ? nextProps.gpuImage.id : null;

        if (oldId !== newId && oldId !== null) {
            this.getGPUImage().releaseView();
        }
    }

    componentWillUnmount() {
        const gpuImage = this.getGPUImage();
        gpuImage && gpuImage.releaseView();
    }

    render() {
        const style = [styles.base, this.props.style];

        const nativeProps = Object.assign({}, {
            gpuImageId: this.getGPUImageId()
        });

        return (
            <View style={style}>
                <NativeGPUImageView ref={c => this.nativeView = c}
                                    style={styles.child} {...nativeProps} />
                {this.props.hasCropper && <GPUCropView ref={c => this.cropView = c} gpuImage={this.getGPUImage()} /> || null}
            </View>
        );
    }

    getGPUImage() {
        return this.props.gpuImage;
    }

    getGPUImageId() {
        return this.getGPUImage() ? this.getGPUImage().id : null;
    }

    executeCrop() {
        this.cropView && this.cropView.crop();
    }
}

const NativeGPUImageView = requireNativeComponent('RCTIONGPUImageView', GPUImageView);

const styles = StyleSheet.create({
    base: {

    },
    child: {
        position: 'absolute',
        top: 0,
        left: 0,
        bottom: 0,
        right: 0,
        backgroundColor: '#00000000'
    }
});
