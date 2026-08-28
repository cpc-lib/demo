import {
  defaultImageOnlyFields,
  imageQueryFileError,
  IMAGE_QUERY_MAX_FILE_SIZE_BYTES
} from './imageQuery';

const validImage = {
  name: 'diagram.png',
  type: 'image/png',
  size: IMAGE_QUERY_MAX_FILE_SIZE_BYTES
};

const invalidType = {
  name: 'notes.txt',
  type: 'text/plain',
  size: 1024
};

const oversizedImage = {
  name: 'large.png',
  type: 'image/png',
  size: IMAGE_QUERY_MAX_FILE_SIZE_BYTES + 1
};

if (imageQueryFileError(validImage) !== undefined) {
  throw new Error('valid image files should be accepted');
}

if (!imageQueryFileError(invalidType)?.includes('image')) {
  throw new Error('non-image files should be rejected');
}

if (!imageQueryFileError(oversizedImage)?.includes('5MB')) {
  throw new Error('oversized image files should mention the 5MB limit');
}

const imageOnlyFields = defaultImageOnlyFields();

if (imageOnlyFields.retrievalMode !== 'vector') {
  throw new Error('image-only retrieval should use vector mode');
}

if (imageOnlyFields.imageVectorWeight !== 1 || imageOnlyFields.textVectorWeight !== 0) {
  throw new Error('image-only retrieval should prefer the image vector');
}
