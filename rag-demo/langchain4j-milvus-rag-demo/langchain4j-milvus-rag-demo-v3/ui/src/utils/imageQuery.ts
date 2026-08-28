export const IMAGE_QUERY_MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

export type ImageQueryFileLike = Pick<File, 'name' | 'size' | 'type'>;

export type ImageOnlyFields = {
  question: undefined;
  retrievalMode: 'vector';
  modalities: string[];
  contentTypes: string[];
  textVectorWeight: number;
  imageVectorWeight: number;
  keywordWeight: number;
};

export function imageQueryFileError(file: ImageQueryFileLike): string | undefined {
  if (!file.type || !file.type.startsWith('image/')) {
    return 'Please select an image file.';
  }
  if (file.size > IMAGE_QUERY_MAX_FILE_SIZE_BYTES) {
    return 'Image query files must be 5MB or smaller.';
  }
  return undefined;
}

export function defaultImageOnlyFields(): ImageOnlyFields {
  return {
    question: undefined,
    retrievalMode: 'vector',
    modalities: ['image'],
    contentTypes: ['image', 'chart', 'table', 'flowchart', 'architecture'],
    textVectorWeight: 0,
    imageVectorWeight: 1,
    keywordWeight: 0
  };
}

export function hasImageQueryInput(values: {
  imageUrl?: string;
  imageBase64?: string;
  imageAssetId?: number;
}) {
  return hasText(values.imageUrl) || hasText(values.imageBase64) || !!values.imageAssetId;
}

export function readImageFileAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string' && reader.result) {
        resolve(reader.result);
        return;
      }
      reject(new Error('Failed to read image file.'));
    };
    reader.onerror = () => reject(reader.error || new Error('Failed to read image file.'));
    reader.readAsDataURL(file);
  });
}

function hasText(value?: string) {
  return !!value && !!value.trim();
}
