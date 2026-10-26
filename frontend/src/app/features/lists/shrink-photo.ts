/** Longest side sent to the API: sharp on the public list cards, a fraction of a phone photo's weight. */
const MAX_SIDE = 1600;
const QUALITY = 0.85;
/** Files at or below this size that already fit MAX_SIDE are sent as they are. */
const SMALL_ENOUGH = 500 * 1024;

/**
 * Resizes a picked photo in the browser and re-encodes it as JPEG, so guests don't download 5 MB per item.
 * Returns the original file when the browser can't decode it or the result would not be smaller.
 */
export async function shrinkPhoto(file: File): Promise<File> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  } catch {
    return file;
  }
  try {
    const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
    if (scale === 1 && file.size <= SMALL_ENOUGH) return file;
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(bitmap.width * scale);
    canvas.height = Math.round(bitmap.height * scale);
    const context = canvas.getContext('2d');
    if (!context) return file;
    // JPEG has no transparency: paint white first so transparent PNGs don't turn black.
    context.fillStyle = '#fff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', QUALITY));
    if (!blob || blob.size >= file.size) return file;
    return new File([blob], file.name.replace(/\.[^.]*$/, '') + '.jpg', { type: 'image/jpeg' });
  } finally {
    bitmap.close();
  }
}
