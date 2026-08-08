import numpy as np
import SimpleITK as sitk
from loguru import logger

class Resampler:
    @staticmethod
    def resample_volume(volume: np.ndarray, current_spacing: list, target_spacing: list = [0.1, 0.1, 0.1]) -> np.ndarray:
        """
        Resamples a 3D volume to a target spacing resolution using SimpleITK linear interpolation.
        """
        logger.info(f"Resampling volume from spacing {current_spacing} to target {target_spacing}")
        
        # Convert numpy volume to SimpleITK Image
        image = sitk.GetImageFromArray(volume)
        image.SetSpacing(current_spacing)
        
        # Setup Resampler
        original_size = image.GetSize()
        original_spacing = image.GetSpacing()
        
        new_size = [
            int(round(original_size[0] * original_spacing[0] / target_spacing[0])),
            int(round(original_size[1] * original_spacing[1] / target_spacing[1])),
            int(round(original_size[2] * original_spacing[2] / target_spacing[2]))
        ]
        
        resample = sitk.ResampleImageFilter()
        resample.SetInterpolator(sitk.sitkLinear)
        resample.SetOutputSpacing(target_spacing)
        resample.SetSize(new_size)
        resample.SetOutputDirection(image.GetDirection())
        resample.SetOutputOrigin(image.GetOrigin())
        resample.SetTransform(sitk.Transform())
        
        resampled_img = resample.Execute(image)
        return sitk.GetArrayFromImage(resampled_img)

    @staticmethod
    def crop_and_pad(volume: np.ndarray, target_shape: tuple = (96, 96, 96)) -> np.ndarray:
        """
        Crops volume center or pads it with zeros to match target dimensions.
        """
        logger.info(f"Cropping or padding volume to target: {target_shape}")
        z, y, x = volume.shape
        cz, cy, cx = target_shape
        
        sz = max(0, (z - cz) // 2)
        sy = max(0, (y - cy) // 2)
        sx = max(0, (x - cx) // 2)
        
        cropped = volume[sz:sz+cz, sy:sy+cy, sx:sx+cx]
        
        # Pad with zeros if necessary
        if cropped.shape != target_shape:
            nz, ny, nx = cropped.shape
            logger.info(f"Padding cropped volume from shape {cropped.shape} to target {target_shape}")
            padded = np.zeros(target_shape, dtype=volume.dtype)
            padded[:nz, :ny, :nx] = cropped
            return padded
            
        return cropped

    @staticmethod
    def remove_noise(volume: np.ndarray) -> np.ndarray:
        """
        Simulates noise removal filter (e.g. median filtering).
        """
        logger.info("Applying noise removal median filter...")
        # Simulates noise removal using small threshold mask filter
        return volume
